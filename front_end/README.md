# API敏感信息脱敏系统 - 前端界面

这是API敏感信息脱敏系统的前端界面，基于Vue 3和Vite开发，提供直观的用户界面用于管理和监控敏感信息脱敏过程。

## 功能特性

- 敏感信息检测与脱敏操作界面
- 支持多种敏感信息类型的识别与处理
- 可视化脱敏结果展示
- 自定义脱敏规则配置
- 脱敏历史记录查询
- 响应式设计，支持多设备访问

## 技术栈

- **前端框架**：Vue 3
- **构建工具**：Vite
- **样式处理**：CSS/SCSS
- **HTTP客户端**：Axios
- **UI组件库**：待补充（根据实际使用的组件库）

## 项目结构

```
front_end/
├── public/           # 静态资源文件
├── src/
│   ├── assets/       # 项目资源（图片、字体等）
│   ├── components/   # Vue组件
│   ├── views/        # 页面视图
│   ├── services/     # API服务
│   ├── utils/        # 工具函数
│   ├── router/       # 路由配置
│   ├── store/        # 状态管理
│   ├── App.vue       # 应用主组件
│   └── main.js       # 应用入口文件
├── index.html        # HTML入口
├── vite.config.js    # Vite配置
├── package.json      # 项目依赖
└── README.md         # 项目说明文档
```

## 快速开始

### 环境要求

- Node.js 16.x 或更高版本
- npm 8.x 或更高版本

### 安装与运行

1. 安装依赖

```bash
npm install
```

2. 运行开发服务器

```bash
npm run dev
```

默认情况下，开发服务器将在 http://localhost:5173 启动。

3. 构建生产版本

```bash
npm run build
```

构建后的文件将输出到 `dist` 目录。

## API集成

前端应用与后端服务通过RESTful API进行交互，主要接口包括：

- 敏感信息检测接口
- 脱敏处理接口
- 脱敏规则配置接口
- 历史记录查询接口

后端服务默认运行在 http://localhost:8081。如需修改API基础URL，请在项目配置中更新。

## 开发指南

### 组件开发

1. 在 `src/components` 目录下创建新组件
2. 使用Vue 3的组合式API (`<script setup>`)进行开发
3. 遵循组件命名规范（PascalCase）

### API调用

1. 在 `src/services` 目录下创建API服务文件
2. 使用Axios进行HTTP请求
3. 统一处理错误和响应格式

## 代码规范

- 遵循Vue 3官方推荐的最佳实践
- 使用ESLint和Prettier保持代码风格一致
- 为组件和关键函数添加适当的文档注释

## 贡献指南

欢迎对本项目进行贡献！

1. Fork 本仓库
2. 创建特性分支 (`git checkout -b feature/amazing-feature`)
3. 提交更改 (`git commit -m 'Add some amazing feature'`)
4. 推送到分支 (`git push origin feature/amazing-feature`)
5. 开启Pull Request

## 许可证

[MIT](https://choosealicense.com/licenses/mit/)
