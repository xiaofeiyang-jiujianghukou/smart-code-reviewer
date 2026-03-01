package com.ai.reviewer.config;

import com.alibaba.cloud.ai.dashscope.api.DashScopeApi;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import com.alibaba.cloud.ai.dashscope.chat.DashScopeChatOptions;
import com.alibaba.cloud.ai.dashscope.chat.DashScopeChatModel;
import org.springframework.context.annotation.Primary;

@Configuration
public class AiConfig {

    @Value("${spring.ai.alibaba.api-key}")
    private String apiKey;

    @Value("${spring.ai.alibaba.chat.options.model:qwen-coder-plus}")
    private String model;

    /**
     * 手动创建 ChatModel Bean
     * 如果自动配置失效，这个 Bean 会被创建
     */
    @Bean
    @Primary
    public ChatModel dashScopeChatModel() {
        DashScopeChatOptions options = DashScopeChatOptions.builder()
                .withModel(model)
                .withTemperature(0.7)
                .build();

        return new DashScopeChatModel(new DashScopeApi(apiKey), options);
    }

    /**
     * 手动创建 ChatClient Bean
     * 基于上面的 ChatModel 构建
     */
    @Bean
    public ChatClient chatClient(ChatModel chatModel) {
        return ChatClient.builder(chatModel).build();
    }
}