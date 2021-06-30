FROM openjdk:8-jdk-alpine

WORKDIR /opt

RUN adduser -S sentinel && \
    apk add --no-cache tzdata && \
    cp /usr/share/zoneinfo/Asia/Shanghai /etc/localtime && \
    echo "Asia/Shanghai" >  /etc/timezone && \
    rm -rf /var/cache/apk/* && \
    sed -i "s/dl-cdn.alpinelinux.org/mirrors.aliyun.com/g" /etc/apk/repositories

COPY target/sentinel-dashboard-tdengine.jar /opt/sentinel-dashboard-tdengine.jar

USER sentinel

EXPOSE 8080
CMD java ${JAVA_OPTS} -Djava.security.egd=file:/dev/./urandom -jar "/opt/sentinel-dashboard-tdengine.jar"
