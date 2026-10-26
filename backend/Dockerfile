# =========================================================
# 後端：Spring Boot 4 + Java 21
# 第一階段用 Maven 編譯出 jar，第二階段只放 JRE + jar，讓映像檔變小
# =========================================================

# ---------- 1. 編譯階段 ----------
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /build

# 先只複製 pom.xml 下載相依套件。pom.xml 沒改時這層會走快取，之後改程式碼重建會快很多
COPY pom.xml .
RUN mvn -B -q dependency:go-offline

COPY src ./src
# 測試需要真的 MySQL / Redis，交給 GitHub Actions 跑；這裡打包時略過
RUN mvn -B -q package -DskipTests \
    && cp target/carwash-system-*.jar /build/app.jar

# ---------- 2. 執行階段 ----------
FROM eclipse-temurin:21-jre
WORKDIR /app

ENV TZ=Asia/Taipei \
    JAVA_OPTS="-XX:MaxRAMPercentage=75 -Duser.timezone=Asia/Taipei"

# 不用 root 執行，降低被入侵時的風險
RUN useradd --system --uid 1001 spring
COPY --from=build /build/app.jar app.jar
USER spring

EXPOSE 8081
ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -jar /app/app.jar"]
