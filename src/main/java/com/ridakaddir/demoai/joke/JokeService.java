package com.ridakaddir.demoai.joke;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;

/**
 * Application service for the {@code joke} module. Encapsulates the Ollama call
 * (model configured via {@code spring.ai.ollama.chat.model} in application.yml)
 * so the web adapter stays thin. Package-private: internal to the module.
 */
@Service
class JokeService {

	private static final Logger log = LoggerFactory.getLogger(JokeService.class);

	private final ChatClient chatClient;

	JokeService(ChatClient.Builder chatClientBuilder) {
		this.chatClient = chatClientBuilder
				.defaultSystem("You are a witty comedian. Reply with a single, short, "
						+ "clean, family-friendly joke and nothing else — no preamble, no explanation.")
				.build();
	}

	String developerJoke() {
		// These logs fire inside the request's trace context, so the OpenTelemetry
		// Logback appender attaches trace_id/span_id and Loki correlates them to Tempo.
		log.info("Requesting a developer joke from the LLM");
		String joke = this.chatClient.prompt()
				.user("Tell me a joke about software developers.")
				.call()
				.content();
		log.info("LLM returned a joke ({} chars)", joke != null ? joke.length() : 0);
		return joke;
	}
}
