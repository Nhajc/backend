FROM eclipse-temurin:25.0.5_8-jre-alpine AS audio-build
ARG ALPINE_MIRROR=https://dl-cdn.alpinelinux.org
RUN sed -i "s|https://dl-cdn.alpinelinux.org|${ALPINE_MIRROR}|g" /etc/apk/repositories \
    && apk add --no-cache build-base cmake curl
WORKDIR /source
RUN curl -fL --retry 3 https://github.com/mstorsjo/fdk-aac/archive/refs/tags/v2.0.3.tar.gz -o fdk-aac.tar.gz \
    && tar -xzf fdk-aac.tar.gz \
    && cmake -S fdk-aac-2.0.3 -B build -DBUILD_SHARED_LIBS=ON -DCMAKE_BUILD_TYPE=Release -DCMAKE_INSTALL_PREFIX=/opt/fdk \
    && cmake --build build -j2 \
    && cmake --install build \
    && mkdir -p /opt/fdk/share/licenses \
    && cp fdk-aac-2.0.3/NOTICE /opt/fdk/share/licenses/fdk-aac-NOTICE

FROM eclipse-temurin:25.0.5_8-jre-alpine AS runtime-tools
ARG ALPINE_MIRROR=https://dl-cdn.alpinelinux.org
RUN sed -i "s|https://dl-cdn.alpinelinux.org|${ALPINE_MIRROR}|g" /etc/apk/repositories \
    && apk add --no-cache flac fdkaac libstdc++
# The distribution library may omit SBR. Use the pinned full library for HE-AAC.
COPY --from=audio-build /opt/fdk/lib/ /usr/local/lib/
COPY --from=audio-build /opt/fdk/share/licenses/ /usr/local/share/licenses/
ENV LD_LIBRARY_PATH=/usr/local/lib
RUN dd if=/dev/zero of=/tmp/aac-check.pcm bs=176400 count=1 \
    && fdkaac --silent --raw --raw-rate 44100 --raw-channels 2 --profile 5 --bitrate 64000 -o /tmp/aac-check.m4a /tmp/aac-check.pcm \
    && rm /tmp/aac-check.pcm /tmp/aac-check.m4a

FROM eclipse-temurin:25.0.5_8-jdk-alpine AS build
WORKDIR /workspace
COPY .mvn/ .mvn/
COPY mvnw pom.xml ./
COPY src/ src/
RUN sed -i 's/\r$//' mvnw && chmod +x mvnw && ./mvnw -DskipTests package

FROM runtime-tools
WORKDIR /app
COPY --from=build /workspace/target/*.jar app.jar
USER nobody
ENTRYPOINT ["java", "-jar", "app.jar"]
