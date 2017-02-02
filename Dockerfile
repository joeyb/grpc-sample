FROM ubuntu
RUN \
  apt-get update && \
  apt-get install -y git && \
  apt-get install -y openjdk-8-jdk

ENV JAVA_HOME /usr/lib/jvm/java-8-openjdk-amd64

COPY . /grpc-sample

RUN \
  cd /grpc-sample && \
  ./gradlew clean build

RUN rm -rf /var/lib/apt/lists/*

EXPOSE 10000

WORKDIR /grpc-sample

CMD "./gradlew" "run"
