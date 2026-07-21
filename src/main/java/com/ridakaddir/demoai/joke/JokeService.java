package com.ridakaddir.demoai.joke;

import io.micrometer.core.instrument.DistributionSummary;
import io.micrometer.core.instrument.MeterRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.metadata.Usage;
import org.springframework.ai.chat.model.ChatResponse;
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

	// Per-request token distributions. Spring AI's built-in gen_ai.client.token.usage
	// is a plain counter (rates/averages only); these summaries add p50/p95 percentiles.
	private final DistributionSummary promptTokens;

	private final DistributionSummary completionTokens;

	JokeService(ChatClient.Builder chatClientBuilder, MeterRegistry meterRegistry) {
		this.chatClient = chatClientBuilder
				.defaultSystem("You are a witty comedian. Reply with a single, short, "
						+ "clean, family-friendly joke and nothing else — no preamble, no explanation.")
				.build();
		this.promptTokens = tokensPerRequestSummary(meterRegistry, "prompt");
		this.completionTokens = tokensPerRequestSummary(meterRegistry, "completion");
	}

	private static DistributionSummary tokensPerRequestSummary(MeterRegistry meterRegistry, String type) {
		return DistributionSummary.builder("llm.tokens.per.request")
				.description("LLM tokens consumed by a single request")
				.baseUnit("tokens")
				.tag("type", type)
				.publishPercentiles(0.5, 0.95)
				.register(meterRegistry);
	}

	String developerJoke() {
		// These logs fire inside the request's trace context, so the OpenTelemetry
		// Logback appender attaches trace_id/span_id and Loki correlates them to Tempo.
		log.info("Requesting a developer joke from the LLM");
		ChatResponse response = this.chatClient.prompt()
				.user("Tell me a joke about software developers.")
				.call()
				.chatResponse();
		String joke = response != null ? response.getResult().getOutput().getText() : null;
		Usage usage = response != null ? response.getMetadata().getUsage() : null;
		if (usage != null) {
			this.promptTokens.record(usage.getPromptTokens());
			this.completionTokens.record(usage.getCompletionTokens());
			log.info("LLM returned a joke ({} chars) — tokens: prompt={} completion={} total={}",
					joke != null ? joke.length() : 0, usage.getPromptTokens(), usage.getCompletionTokens(),
					usage.getTotalTokens());
		}
		else {
			log.info("LLM returned a joke ({} chars) — no token usage metadata available",
					joke != null ? joke.length() : 0);
		}
		return joke;
	}
}
