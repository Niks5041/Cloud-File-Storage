#использовать образ на базе eclipse-temurin с Java 21 и JRE
FROM eclipse-temurin:21-jre-jammy
#Все команды, которые будут выполнены далее, выполняются в директории /app в контейнере
WORKDIR /app
#копируем все .jar файлы из локальной директории build/libs в контейнер и называешь их app.jar
COPY build/libs/*.jar app.jar
#когда контейнер запускается, он должен выполнить команду Java с указанным JAR файлом app.jar
ENTRYPOINT ["sh", "-c", "java ${JAVA_OPTS} -jar /app/app.jar"]
