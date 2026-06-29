# AGENTS.md - ERP系统开发指南

## 技术栈
- **Java**: 21
- **Spring Boot**: 3.5.5
- **MyBatis-Plus**: 3.5.5
- **数据库**: MySQL 8.0+ (需要配置连接)
- **认证**: JWT (jjwt 0.11.5)
- **密码加密**: Argon2

## 快速命令

### 构建与运行
```bash
# 编译项目
./mvnw compile

# 运行应用
./mvnw spring-boot:run

# 打包
./mvnw package

# 运行测试
./mvnw test
```

### 单个测试
```bash
# 运行单个测试类
./mvnw test -Dtest=ApprovalInstanceServiceTest

# 运行单个测试方法
./mvnw test -Dtest=ApprovalInstanceServiceTest#testMethod
```

## 项目结构
```
Erp/
├── src/main/java/com/tonghui/erp/
│   ├── Controller/     # REST控制器
│   ├── Service/        # 业务逻辑层
│   ├── Data/           # 数据访问层
│   │   ├── Entity/     # 实体类
│   │   └── mapper/     # MyBatis Mapper接口
│   ├── Common/         # 公共模块
│   │   ├── Config/     # 配置类
│   │   ├── Dto/        # 数据传输对象
│   │   └── utils/      # 工具类
│   └── ErpApplication.java
├── src/main/resources/
│   ├── Data/mapper/    # MyBatis XML映射文件
│   └── application.yml # 应用配置
└── src/test/           # 测试代码
```

## 关键配置
- **数据库**: `src/main/resources/application.yml` 中的 `spring.datasource`
- **JWT**: `jwt.secret-key` 需要替换为安全密钥
- **文件存储**: `file.base-path` 默认为 `./uploaded-files`
- **环境变量**: `ERP_FILE_STORAGE_PATH` 可自定义文件存储路径

## 开发规范
- **架构**: 三层架构 (Controller → Service → Data)
- **认证**: 除登录接口外，所有API需要JWT令牌
- **API路径**: 以 `/api` 开头
- **分页**: 使用 `PageRequestDto` 进行分页查询
- **响应**: 统一使用 `ApiResponse` 格式
- **密码**: 使用Argon2算法加密

## 数据库要求
- 需要MySQL 8.0+数据库
- 数据库名: `erp_db` (可在application.yml中修改)
- 启动前需确保数据库连接正常

## 测试
- 测试文件位于 `src/test/java`
- 使用Spring Boot Test
- 测试覆盖率较低，主要测试关键业务逻辑

## 默认账户
- **用户名**: root
- **密码**: root
- 系统启动时自动创建，用于首次登录和管理

## 常见问题
1. **数据库连接失败**: 检查application.yml中的数据库配置
2. **JWT认证失败**: 确保请求头包含 `Authorization: Bearer <token>`
3. **文件上传失败**: 检查文件类型和大小限制
4. **初始化失败**: 确保数据库表已创建，系统会自动初始化root用户

## 相关文档
- [项目概述](docs/00-项目概述.md)
- [Service接口文档](SERVICE_DOC.md)
- [库存预警后端](库存预警后端.md)