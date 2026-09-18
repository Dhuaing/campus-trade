# syntax=docker/dockerfile:1
# 多阶段构建：Maven 编译 -> JRE 运行
FROM maven:3.9-eclipse-temurin-17 AS builder
WORKDIR /build
COPY pom.xml ./
COPY src ./src
# .m2 依赖走 BuildKit 缓存挂载；依赖下载抖动时自动重试，最多 3 次
RUN --mount=type=cache,target=/root/.m2 \
    bash -c 'for i in 1 2 3; do mvn clean package -DskipTests -B -Dmaven.wagon.http.retryHandler.count=3 && break; echo "build retry ($i/3)..."; sleep 5; done; ls target/*.jar'

FROM eclipse-temurin:17-jre
WORKDIR /app
COPY --from=builder /build/target/*.jar app.jar
EXPOSE 8080
CMD ["sh", "-c", "java $JAVA_OPTS -jar app.jar --server.port=${PORT}"]