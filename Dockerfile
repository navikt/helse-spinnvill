FROM gcr.io/distroless/java21-debian12:nonroot

ENV TZ="Europe/Oslo"
ENV JDK_JAVA_OPTIONS='-XX:MaxRAMPercentage=90'

WORKDIR /app

COPY spinnvill-app/build/libs/*.jar .

CMD ["app.jar"]
