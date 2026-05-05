# 任务进度

## 第一阶段任务清单

### 问题一：图片共享（MinIO对象存储）
- [x] 分析现状：图片存在本地C:/upload/，多实例无法共享
- [ ] 1. pom.xml 添加 MinIO 依赖
- [ ] 2. 创建 MinIO 配置类 MinIOConfig.java
- [ ] 3. 创建 MinIO 工具类 MinIOUtil.java
- [ ] 4. 修改 FileUploadUtil.java 改用 MinIO 存储
- [ ] 5. 修改 spring-mvc.xml 静态资源配置
- [ ] 6. 启动 MinIO Docker 容器
- [ ] 7. 测试图片上传和访问

### 问题二：成员一任务（Redis + 分布式Session）
- [ ] 1. pom.xml 添加 Redis 依赖（spring-data-redis + jedis）
- [ ] 2. 创建 RedisConfig.java 配置类
- [ ] 3. 修改 applicationContext.xml 加载 Redis 配置
- [ ] 4. 实现 RedisSessionConfig.java（分布式Session共享）
- [ ] 5. 创建 TestController.java（验证工具）
- [ ] 6. 测试两个Tomcat实例Session共享
