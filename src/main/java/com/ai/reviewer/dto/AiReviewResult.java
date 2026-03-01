package com.ai.reviewer.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor // 🔴 关键：Jackson 需要无参构造函数
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class AiReviewResult {
    private String summary;
    private List<Issue> issues;
    private Documentation documentation;

    @Data
    public static class Issue {
        private String file;
        private Integer line;
        private String severity; // CRITICAL, HIGH, MEDIUM, LOW
        private String message;
        private String suggestion;
        @JsonProperty("fixed_code") // 🔴 关键：显式指定映射关系，防止下划线转驼峰失败
        private String fixedCode;
    }

    @Data
    public static class Documentation {
        @JsonProperty("needs_update")
        private Boolean needsUpdate;
        private String content;
    }
}
