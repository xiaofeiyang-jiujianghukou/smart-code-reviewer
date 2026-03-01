package com.ai.reviewer.service;

import com.ai.reviewer.dto.AiReviewResult;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.prompt.PromptTemplate;
import org.springframework.ai.chat.prompt.TemplateFormat;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class ReviewService {

    private final ChatClient chatClient;
    private final GitService gitService;
    private final ObjectMapper objectMapper = new ObjectMapper(); // 引入 Jackson

    @Value("${app.ai.review-prompt-template}")
    private String promptTemplateString;

    public void processPullRequest(String owner, String repo, int prNumber) {
        try {
            // 1. 获取代码 Diff
            log.info("Fetching diff for {}/{}/#{}", owner, repo, prNumber);
            String diff = gitService.getPullRequestDiff(owner, repo, prNumber);

            if (diff.isEmpty() || diff.length() > 50000) {
                // 简单限制：如果 Diff 太大，可能需要分片处理或只分析关键文件
                log.warn("Diff is empty or too large, skipping detailed analysis for now.");
                // 这里可以添加逻辑：只分析前几个文件，或者提示用户拆分 PR
            }

            // 2. 构建 Prompt
            if (!promptTemplateString.contains("{{code_diff}}")) {
                log.error("Prompt template missing '{{code_diff}}' placeholder!");
                throw new IllegalArgumentException("Template error: missing {{code_diff}}");
            }

            String finalPrompt = promptTemplateString.replace("{{code_diff}}", diff);


            // 3. 调用大模型 (Spring AI 1.0+ 语法)
            // 使用 .entity() 自动将 JSON 响应转换为 Java 对象
            log.info("Sending request to AI model...");
            log.info("Final Prompt: /n {}", finalPrompt);
            String fullResponse = chatClient.prompt()
                    .user(finalPrompt)
                    .stream()
                    .content()          // 获取 Flux<String>
                    .collectList()      // 收集为 List<String>
                    .map(list -> String.join("", list)) // 拼接为完整字符串
                    .block(Duration.ofMinutes(5)); // 阻塞等待完成

            if (fullResponse == null || fullResponse.isEmpty()) {
                throw new RuntimeException("AI returned empty response");
            }

            log.info("✅ AI Response received. Parsing JSON manually...");

            // 4. 【关键修改】手动解析 JSON
            AiReviewResult result = parseJsonResponse(fullResponse);

            // 5. 提交评论
            //String markdownComment = buildMarkdownComment(result);
            gitService.postReviewComment(owner, repo, prNumber, result);

            log.info("🎉 Review completed for PR #{}", prNumber);

        } catch (Exception e) {
            log.error("Failed to process PR review", e);
            // 可以选择发送一个错误评论通知开发者
        }
    }

    /**
     * 手动解析 JSON 字符串为 Java 对象
     * 完全绕过 Spring AI 的内部模板机制
     */
    private AiReviewResult parseJsonResponse(String json) {
        try {
            // 清洗可能存在的 Markdown 标记 (AI 喜欢加 ```json ... ```)
            String cleanJson = json.replaceAll("```json", "").replaceAll("```", "").trim();

            // 使用 Jackson 解析
            return objectMapper.readValue(cleanJson, AiReviewResult.class);

        } catch (Exception e) {
            log.error("JSON Parse Error. Raw response:\n{}", json);
            // 降级处理：返回一个包含错误信息的对象，避免程序崩溃
            AiReviewResult fallback = new AiReviewResult();
            fallback.setSummary("⚠️ AI 返回格式错误，无法解析 JSON。原始回复片段:\n" +
                    json.substring(0, Math.min(300, json.length())));
            fallback.setIssues(new ArrayList<>());
            return fallback;
        }
    }

    private String buildMarkdownComment(AiReviewResult result) {
        if (result == null) return "❌ AI 服务异常，未返回结果。";

        StringBuilder sb = new StringBuilder();
        sb.append("### 🤖 AI 智能代码评审报告\n\n");

        // 🔴 关键修复：检测是否发生了降级（解析失败）
        // 如果 Summary 以警告开头，说明 JSON 解析失败了，直接展示错误，不要显示“太棒了”
        if (result.getSummary() != null && result.getSummary().contains("⚠️")) {
            sb.append("### ⚠️ AI 响应格式异常\n\n");
            sb.append("AI 返回的数据格式不正确（可能是输出过长被截断），导致无法生成结构化报告。\n\n");
            sb.append("**原始回复片段**:\n");
            sb.append("```text\n").append(result.getSummary()).append("\n```\n\n");
            sb.append("建议：请检查代码变更是否过大，或尝试拆分 Pull Request。");
            return sb.toString();
        }

        // 正常流程
        sb.append("> ").append("**📝 总结**: ").append(result.getSummary()).append("\n\n");

        if (result.getIssues() != null && !result.getIssues().isEmpty()) {
            sb.append("#### 🚨 发现的问题 (共 ").append(result.getIssues().size()).append(" 处)\n\n");

            for (AiReviewResult.Issue issue : result.getIssues()) {
                if (issue.getFile() == null || issue.getMessage() == null) continue;

                String icon = switch (issue.getSeverity()) {
                    case "CRITICAL" -> "🔴";
                    case "HIGH" -> "🟠";
                    case "MEDIUM" -> "🟡";
                    default -> "🔵";
                };

                String fileRef = String.format("`%s:%d`", issue.getFile(), issue.getLine() != null ? issue.getLine() : 0);

                sb.append(String.format(
                        "%s **[%s] %s** %s\n> %s\n\n%s\n\n---\n",
                        icon,
                        issue.getSeverity(),
                        issue.getMessage(),
                        fileRef,
                        issue.getSuggestion() != null ? issue.getSuggestion() : "无具体建议",
                        issue.getFixedCode() != null && !issue.getFixedCode().isEmpty()
                                ? "```java\n" + issue.getFixedCode() + "\n```"
                                : ""
                ));
            }
        } else {
            sb.append("### ✅ 太棒了！\n\n没有发现明显的代码规范、安全或性能问题。继续保持！\n");
        }

        sb.append("\n---\n*此评论由 Spring AI + DeepSeek/Qwen 自动生成*");
        return sb.toString();
    }
}