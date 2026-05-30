# ApiSensitivities - Code Wiki

## 1. 项目概述 (Project Overview)
**ApiSensitivities** 是一个专注于 API 敏感信息脱敏及大型语言模型（LLM）隐私保护的系统。项目提供了从敏感数据检测、多策略脱敏到 LLM 安全代理的完整解决方案，支持处理纯文本、结构化数据（JSON/XML）以及二进制数据（图片、音频、PDF、DOC）。系统旨在防止将包含个人隐私（PII）或敏感数据的请求直接暴露给外部大语言模型。

## 2. 整体架构 (Overall Architecture)
项目采用前后端分离的架构：
- **后端 (Backend)**：基于 **Spring Boot 3** (Java 21) 构建。使用 MyBatis 作为持久层框架，内置 H2 内存数据库（兼容 MySQL）。集成 HanLP 进行自然语言处理（NLP），使用 Apache PDFBox 和 POI 解析文件。
- **前端 (Frontend)**：基于 **Vue 3** 和 **Vite** 开发。提供敏感信息规则配置、脱敏过程监控、LLM 代理测试和数据统计看板（使用 Chart.js）。

后端分为两大核心业务流：
1. **直接数据脱敏流**：接收数据 -> 数据解析 -> 情景感知 -> 敏感实体检测 -> 策略脱敏 -> 返回脱敏数据。
2. **LLM 代理流**：接收 LLM 请求 -> 输入脱敏（占位符替换等） -> 调用真实 LLM 提供商 -> 输出还原映射 -> 返回给用户。

## 3. 主要模块职责 (Main Modules & Responsibilities)

### 3.1 后端核心模块
- **Desensitization (脱敏管理模块)**
  - 核心编排：统筹解析、情景感知、检测和脱敏的全流程。
  - 脱敏策略：包括数据替换（DataReplacement）、泛化（Generalization）、掩码（Mask）、语义占位符（SemanticPlaceholder）等策略。
- **SensitiveDetection (敏感信息检测模块)**
  - 基于正则表达式 (`RegexDetectionService`) 和 NLP 模型 (`NlpDetectionService`) 进行敏感实体（如身份证、手机号、人名等）检测。
  - 支持文本、结构化数据、二进制数据的检测提取。
- **ScenarioPerception (情景感知模块)**
  - 基于关键词或 LLM 分析请求上下文（如医疗、金融场景），动态调整敏感检测范围，提高准确率，减少误报。
- **DataParser (数据解析模块)**
  - 负责将各种格式的数据（JSON、XML、PDF、DOC等）转换为统一的文本内容，以供后续检测模块处理。
- **LlmProxy & LlmClient (LLM 代理与客户端模块)**
  - `LlmProxyService`：代理 LLM 请求，在调用大模型前拦截并脱敏输入内容，大模型返回后还原占位符，实现“无感”的隐私保护。
  - `LlmClient`：实现了对接多家大模型厂商的适配（包括 OpenAI, DeepSeek, 豆包, 千问, Kimi, 混元以及本地 Ollama）。

### 3.2 前端界面模块 (`front_end/src/components/`)
- **LlmDesensitization.vue**：LLM 敏感信息脱敏的测试交互界面，展示脱敏前后的输入输出对比。
- **SensitiveRules.vue**：敏感信息检测规则（黑白名单、自定义正则）的配置与管理。
- **ConversationHistory.vue**：脱敏及对话历史记录展示。
- **DashboardStats.vue**：数据仪表盘，使用 Vue-Chartjs 展示拦截次数、敏感类型占比等统计信息。

## 4. 关键类与函数说明 (Key Classes & Functions)

### `com.hdu.apisensitivities.service.DesensitizationManager`
- **职责**：脱敏流程的中央编排器。
- **`process(DesensitizationRequest request)`**：
  执行完整的脱敏流程：解析数据 -> 评估情景（目前默认检测所有类型） -> `detectSensitiveEntities`（检测实体） -> `applyDesensitization`（执行脱敏策略）。

### `com.hdu.apisensitivities.service.LlmProxyService`
- **职责**：处理大语言模型请求全流程，包含自我反思（Self-Reflection）与占位符映射。
- **`processLlmRequest(LlmRequest request)`**：
  处理 LLM 请求，对输入 prompt 进行基础脱敏和 NLP 语义占位符打码（调用 `SemanticPlaceholderStrategy`），进行 Agent 反思审计。随后调用 `callLlmApiWithDataType` 访问云端 LLM，最后将返回结果中的占位符还原。

### `com.hdu.apisensitivities.service.SensitiveDetection.SensitiveDetectionService`
- **职责**：敏感信息检测接口。
- **`detectSensitiveInfo(...)`**：针对不同数据类型提供重载方法，根据传入的情景或指定类型集合，提取 `SensitiveEntity` 列表。

### `com.hdu.apisensitivities.service.Desensitization.SemanticPlaceholderStrategy`
- **职责**：语义占位符脱敏策略。
- **工作原理**：将识别出的敏感词（如人名 "张三"）替换为占位符（如 "[ENTITY_1]"），并将映射关系存储在当前线程的上下文中，等大模型返回包含 "[ENTITY_1]" 的回答时，再替换回真实数据。

## 5. 依赖关系 (Dependencies)

### 后端依赖 (`pom.xml`)
- **Spring Boot (3.5.6)**：`spring-boot-starter-web`, `spring-boot-starter-test`
- **数据库**：`mysql-connector-j`, `h2`
- **ORM**：`mybatis-spring-boot-starter` (3.0.5)
- **文档处理**：`pdfbox` (PDF提取), `poi` (Word提取)
- **NLP 引擎**：`hanlp` (portable-1.8.3，用于中文分词与命名实体识别)
- **其他**：`lombok`, `jackson-databind`, `metadata-extractor`, `spring-dotenv`

### 前端依赖 (`front_end/package.json`)
- **核心框架**：`vue` (^3.5.22), `vite`
- **图表与可视化**：`chart.js`, `vue-chartjs`
- **工具库**：`markdown-it`, `highlight.js`, `html2canvas`, `jspdf`

## 6. 项目运行方式 (How to Run)

### 6.1 环境准备
- Java 21+
- Maven 3.6+
- Node.js 16.x+ 及 npm 8.x+
- (可选) 配置 `.env` 文件或 `application.properties` 填入所需的大模型 API Key。

### 6.2 启动后端服务
1. 进入项目根目录。
2. 运行 Maven 命令启动 Spring Boot 应用：
   ```bash
   ./mvnw spring-boot:run
   ```
   或者使用 IDE 直接运行 `ApiSensitivitiesApplication.java`。
3. 后端默认运行在 `http://localhost:8080`。数据库默认使用内存 H2 库，启动时自动初始化结构。

### 6.3 启动前端服务
1. 进入前端目录：
   ```bash
   cd front_end
   ```
2. 安装依赖：
   ```bash
   npm install
   ```
3. 启动开发服务器：
   ```bash
   npm run dev
   ```
4. 浏览器访问 `http://localhost:5173` 即可打开前端交互界面。
