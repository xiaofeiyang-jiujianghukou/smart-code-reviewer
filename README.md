# 🚀 智能代码评审与文档生成助手 (Smart Code Reviewer)

[![Java](https://img.shields.io/badge/Java-17+-blue.svg)](https://openjdk.java.net/)
[![Spring AI](https://img.shields.io/badge/Spring-AI-green.svg)](https://spring.ai/)
[![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](LICENSE)

> **AI 驱动的代码质量守护者**
> 基于 **Spring AI** 框架，集成 **阿里云百炼**、**DeepSeek** 及 **Qwen (通义千问)** 等大模型能力，为开发团队提供自动化的代码评审、分支合并请求分析及技术文档生成功能。

## ✨ 核心特性

- **🤖 智能代码评审**：自动分析 Pull Request/Merge Request 中的代码变更，识别潜在 Bug、代码异味 (Code Smells) 及安全漏洞。
- **📝 自动化文档生成**：根据代码逻辑自动生成清晰的 API 文档、类说明及更新日志，减少人工编写成本。
- **🧠 多模型支持**：灵活切换底层大模型，支持阿里云百炼、DeepSeek、Qwen 等主流模型，适应不同场景需求。
- **🔗 深度集成**：无缝对接 Git 工作流，在分支合并阶段提供实时反馈。
- **⚡ 高效构建**：基于 Spring Boot 3.x 与 Maven 构建，开箱即用。

## 🛠️ 技术栈

- **开发语言**: Java 17+
- **核心框架**: Spring Boot, Spring AI
- **大模型集成**: 
  - Alibaba Cloud Bailian (阿里云百炼)
  - DeepSeek
  - Qwen (通义千问)
- **构建工具**: Maven
- **版本控制**: Git

## 📦 快速开始

### 前置要求

- JDK 17 或更高版本
- Maven 3.6+
- 已申请的大模型 API Key (阿里云/DeepSeek/Qwen)

### 1. 克隆项目

```bash
git clone https://github.com/xiaofeiyang-jiujianghukou/smart-code-reviewer.git
cd smart-code-reviewer
```

### 2. 配置环境变量

在项目根目录创建 `.env` 文件或直接在 `application.yml` 中配置您的 API Key：

```yaml
spring:
  ai:
    alibaba:
      api-key: ${YOUR_ALIBABA_CLOUD_API_KEY}
      # 或者配置其他模型
      dashscope:
        api-key: ${YOUR_DASHSCOPE_API_KEY}
    deepseek:
      api-key: ${YOUR_DEEPSEEK_API_KEY}
```

### 3. 构建与运行

使用 Maven Wrapper 进行构建并启动应用：

```bash
# Linux/Mac
./mvnw clean spring-boot:run

# Windows
mvnw.cmd clean spring-boot:run
```

应用启动后，默认访问地址为：`http://localhost:8080`

## 💡 使用场景

### 1. 代码合并评审
当开发人员发起 Merge Request 时，本助手会自动拉取变更代码，调用大模型进行深度分析，并在评论区输出评审意见：
- 逻辑错误检测
- 性能优化建议
- 规范符合度检查

### 2. 文档自动补全
提交代码后，触发文档生成任务，自动更新项目的 `README`、接口文档或内部 Wiki。

## ⚙️ 配置说明

主要配置文件位于 `src/main/resources/application.yml`。您可以在此调整：
- **模型选择**：指定默认使用的 LLM 模型。
- **评审规则**：自定义代码评审的严格程度和关注点。
- **触发条件**：设置触发评审的分支策略。

## 🤝 贡献指南

欢迎提交 Issue 和 Pull Request 来帮助改进本项目！

1. Fork 本仓库
2. 创建您的特性分支 (`git checkout -b feature/AmazingFeature`)
3. 提交您的更改 (`git commit -m 'Add some AmazingFeature'`)
4. 推送到分支 (`git push origin feature/AmazingFeature`)
5. 开启一个 Pull Request

## 📄 许可证

本项目采用 Apache 2.0 许可证 - 查看 [LICENSE](LICENSE) 文件了解详情。

## 📬 联系方式

如有问题或合作意向，请通过 GitHub Issues 联系作者。
