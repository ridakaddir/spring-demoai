package com.ridakaddir.demoai;

import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.instrumentation.logback.appender.v1_0.OpenTelemetryAppender;
import org.springframework.context.annotation.Configuration;

/**
 * Wires the Spring-managed OpenTelemetry SDK into the Logback {@code OpenTelemetryAppender}
 * declared in {@code logback-spring.xml}, so application logs are exported via OTLP to Loki.
 *
 * <p>Spring Boot auto-configures the OTLP log exporter (see
 * {@code management.opentelemetry.logging.export.otlp.endpoint}) but, unlike tracing and
 * metrics, does not attach the Logback appender for us — the appender buffers records until
 * {@link OpenTelemetryAppender#install(OpenTelemetry)} is called with an SDK instance. Doing
 * that here (at bean construction) flushes buffered startup logs and streams the rest.
 */
@Configuration(proxyBeanMethods = false)
class OpenTelemetryAppenderConfig {

	OpenTelemetryAppenderConfig(OpenTelemetry openTelemetry) {
		OpenTelemetryAppender.install(openTelemetry);
	}
}
