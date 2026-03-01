package com.ai.reviewer.service;

import com.ai.reviewer.dto.AiReviewResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.kohsuke.github.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

@Slf4j
@Service
@RequiredArgsConstructor
public class GitService {

    @Value("${app.github.token}")
    private String githubToken;

    private GitHub getGitHub() throws IOException {
        return GitHub.connectUsingOAuth(githubToken);
    }

    /**
     * 获取 PR 的文件变更 Diff 内容
     */
    public String getPullRequestDiff(String owner, String repoName, int prNumber) throws IOException {
        GitHub github = getGitHub();
        GHRepository repository = github.getRepository(owner + "/" + repoName);
        GHPullRequest pr = repository.getPullRequest(prNumber);

        PagedIterable<GHPullRequestFileDetail> filesIterable = pr.listFiles();

        // 获取所有文件
        return StreamSupport.stream(filesIterable.spliterator(), false)
                .map(file -> {
                    // 注意：github-api 库直接获取完整 Diff 字符串可能需要一些技巧
                    // 这里简化处理，实际生产中建议调用 GitHub REST API raw diff 接口
                    // 或者使用 file.getPatch() 获取该文件的 patch

                    // 获取文件名
                    String fileName = file.getFilename();
                    // 获取该文件的 Patch (Diff 片段)
                    // 注意：getPatch() 可能抛出 IOException，需捕获
                    String patch = file.getPatch();

                    if (patch == null || patch.isEmpty()) {
                        return "";
                    }

                    // 格式化输出，方便 AI 识别文件边界
                    return "### File: " + fileName + "\n```diff\n" + patch + "\n```\n";
                })
                .filter(s -> !s.isEmpty())
                .collect(Collectors.joining("\n\n---\n\n"));
    }

    /**
     * 在 PR 下发表评论
     */
    public void postReviewComment(String owner, String repoName, int prNumber, AiReviewResult result) throws IOException {
        GitHub github = getGitHub();
        GHRepository repository = github.getRepository(owner + "/" + repoName);
        GHPullRequest pr = repository.getPullRequest(prNumber);

        StringBuilder commentBody = new StringBuilder();
        commentBody.append("### 🤖 AI 代码评审报告\n\n");
        commentBody.append("**总结**: ").append(result.getSummary()).append("\n\n");

        if (result.getIssues() != null && !result.getIssues().isEmpty()) {
            commentBody.append("#### 🚨 发现的问题\n\n");
            for (AiReviewResult.Issue issue : result.getIssues()) {
                String icon = switch (issue.getSeverity()) {
                    case "CRITICAL" -> "🔴";
                    case "HIGH" -> "🟠";
                    case "MEDIUM" -> "🟡";
                    default -> "🔵";
                };
                commentBody.append(String.format(
                        "%s **[%s] %s** (`%s:%d`)\n\n> %s\n\n**💡 建议**: %s\n\n```java\n%s\n```\n\n",
                        icon,                          // 1. %s (图标)
                        issue.getSeverity(),           // 2. %s (等级)
                        issue.getMessage(),            // 3. %s (问题消息)
                        issue.getFile(),               // 4. %s (文件)
                        issue.getLine() != null ? issue.getLine() : 0, // 5. %d (行号，防止空指针)
                        issue.getMessage(),            // 6. %s (引用块中再次显示消息，或者您可以换成 issue.getDescription() 如果有这个字段)
                        //    👆 注意：如果您想让引用块显示别的内容，请替换这里。通常显示 Message 最稳妥。
                        issue.getSuggestion() != null ? issue.getSuggestion() : "无具体建议", // 7. %s (建议)
                        issue.getFixedCode() != null ? issue.getFixedCode() : "// 无自动修复代码" // 8. %s (修复代码)
                ));
            }
        } else {
            commentBody.append("✅ **太棒了！没有发现明显问题。**\n");
        }

        if (result.getDocumentation() != null && result.getDocumentation().getNeedsUpdate()) {
            commentBody.append("#### 📄 文档生成建议\n\n");
            commentBody.append(result.getDocumentation().getContent()).append("\n");
        }

        pr.comment(commentBody.toString());
        log.info("Posted review comment to PR #{}", prNumber);
    }

    /**
     * (进阶) 自动提交文档更新
     * 此处省略具体文件提交逻辑，原理是：获取文件内容 -> 插入 AI 生成的 Javadoc -> 创建新 Commit
     */
}