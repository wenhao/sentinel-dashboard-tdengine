FROM openjdk:8-jdk-alpine

WORKDIR /opt

RUN adduser -S sentinel && \
    apk add --no-cache tzdata && \
    cp /usr/share/zoneinfo/Asia/Shanghai /etc/localtime && \
    echo "Asia/Shanghai" >  /etc/timezone && \
    rm -rf /var/cache/apk/* && \
    sed -i "s/dl-cdn.alpinelinux.org/mirrors.aliyun.com/g" /etc/apk/repositories && \
    curl -sf https://www.taosdata.com/assets-download/TDengine-client-2.1.3.2-Linux-x64.tar.gz -o TDengine-client-2.1.3.2-Linux-x64.tar.gz && \
    tar -xvf TDengine-client-2.1.3.2-Linux-x64.tar.gz && \
    mv /opt/TDengine-client-2.1.3.2/driver/libtaos.so.2.1.3.2 /opt/TDengine-client-2.1.3.2/driver/libtaos.so && \
    rm -f /opt/TDengine-client-2.1.3.2-Linux-x64.tar.gz

COPY target/sentinel-dashboard-tdengine.jar /opt/sentinel-dashboard-tdengine.jar

USER sentinel

EXPOSE 8080
CMD java ${JAVA_OPTS} -Djava.security.egd=file:/dev/./urandom -jar "/opt/sentinel-dashboard-tdengine.jar"
