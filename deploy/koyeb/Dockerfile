# Runtime image for Koyeb's free instance: 512 MB of memory and 0.1 vCPU.
# Build context is the `server` module, matching server/Dockerfile, so the fat jar
# must already exist at server/build/libs/*-all.jar before this is built.
FROM eclipse-temurin:21-jre-alpine

WORKDIR /app
COPY build/libs/*-all.jar /app/server.jar

# The JVM's own defaults assume it owns the machine. At 512 MB they produce a heap
# large enough that the container is killed before the garbage collector ever runs.
# Every region is therefore capped explicitly, leaving roughly 50 MB of headroom:
#   heap 220 + metaspace 112 + code cache 48 + direct buffers 32 = 412 MB
# SerialGC because the parallel collectors spawn threads this instance cannot afford,
# and C1-only JIT because 0.1 vCPU makes full optimisation cost more than it returns.
ENV JAVA_TOOL_OPTIONS="\
-Xmx220m \
-Xss512k \
-XX:MaxMetaspaceSize=112m \
-XX:ReservedCodeCacheSize=48m \
-XX:MaxDirectMemorySize=32m \
-XX:+UseSerialGC \
-XX:TieredStopAtLevel=1 \
-XX:+ExitOnOutOfMemoryError \
-Dfile.encoding=UTF-8"

# Koyeb injects PORT into the environment and Env.port reads it; 8000 is Koyeb's default.
EXPOSE 8000

ENTRYPOINT ["java", "-Dlogback.configurationFile=logback.xml", "-jar", "/app/server.jar"]
