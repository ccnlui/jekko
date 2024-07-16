FROM gradle:7.4.2-jdk17 AS builder
WORKDIR /jekko
COPY . .
RUN gradle installDist
ENTRYPOINT [ "/jekko/app/build/install/jekko/bin/jekko" ]

# TODO: multi-stage build.
FROM eclipse-temurin:17-jre
COPY --from=builder /jekko/app/build/install/jekko /jekko
ENTRYPOINT [ "/jekko/bin/jekko" ]
