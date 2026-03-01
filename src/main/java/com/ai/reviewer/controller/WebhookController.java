package com.ai.reviewer.controller;

import com.ai.reviewer.service.ReviewService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/webhook")
@RequiredArgsConstructor
public class WebhookController {

    private final ReviewService reviewService;

    // GitHub Webhook 入口
    @PostMapping(value = "/github", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<String> handleGitHubWebhook(
            @RequestBody Map<String, Object> payload,
            @RequestHeader("X-GitHub-Event") String event) {

        log.info("Received GitHub Event: {}", event);

        if ("pull_request".equals(event)) {
            String action = (String) payload.get("action");
            // 只在 PR 打开或更新时触发
            if ("opened".equals(action) || "synchronize".equals(action)) {
                try {
                    Map<String, Object> pr = (Map<String, Object>) payload.get("pull_request");
                    int prNumber = (Integer) pr.get("number");

                    Map<String, Object> repo = (Map<String, Object>) payload.get("repository");
                    String owner = ((Map<String, Object>) repo.get("owner")).get("login").toString();
                    String repoName = repo.get("name").toString();

                    // 异步处理，避免 Webhook 超时 (GitHub 超时限制约 10-30 秒)
                    // 生产环境建议使用消息队列 (RabbitMQ/Kafka)
                    new Thread(() -> {
                        reviewService.processPullRequest(owner, repoName, prNumber);
                    }).start();

                    return ResponseEntity.ok("Processing...");
                } catch (Exception e) {
                    log.error("Error parsing payload", e);
                    return ResponseEntity.badRequest().body("Error parsing payload");
                }
            }
        }

        return ResponseEntity.ok("Ignored event");
    }
}