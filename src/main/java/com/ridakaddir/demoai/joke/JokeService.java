package com.ridakaddir.demoai.joke;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;

/**
 * Application service for the {@code joke} module. Encapsulates the Ollama call
 * (model configured via {@code spring.ai.ollama.chat.model} in application.yml)
 * so the web adapter stays thin. Package-private: internal to the module.
 */
@Service
class JokeService {

	private final ChatClient chatClient;

	JokeService(ChatClient.Builder chatClientBuilder) {
		this.chatClient = chatClientBuilder
				.defaultSystem("You are a witty comedian. Reply with a single, short, "
						+ "clean, family-friendly joke and nothing else — no preamble, no explanation.")
				.build();
	}

	String developerJoke() {
		return this.chatClient.prompt()
				.user("Tell me a joke about software developers.")
				.call()
				.content();
	}
}
