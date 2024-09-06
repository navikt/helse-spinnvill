FROM gcr.io/distroless/java21-debian12:nonroot

ENV TZ="Europe/Oslo"
ENV JAVA_OPTS='-XX:MaxRAMPercentage=90'

WORKDIR /app

COPY spinnvill-app/build/libs/*.jar .

CMD ["app.jar"]
