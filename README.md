# finger-mark

### 介绍
finger-mark 是指纹的意思，代表着绝对不重复

- ID 生成：基于雪花算法
- 机器号注册：基于 Redis 的 setNx 命令

可以完美解决时间回拨问题！具体看博客：https://mp.weixin.qq.com/s/o5QzHX1TrgvQa3ipzCtEqQ

### 使用

- 在 application.properties 配置 Redis「IP」和「端口号」后启动项目，即可对外提供 ID 分发功能
```
# redis config
spring.redis.host=127.0.0.1
spring.redis.port=6379
```
 
