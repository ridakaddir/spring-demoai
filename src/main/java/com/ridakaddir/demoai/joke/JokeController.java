package com.ridakaddir.demoai.joke;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Web adapter for the {@code joke} module: exposes {@code GET /jokes} and
 * delegates joke generation to {@link JokeService}.
 */
@RestController
class JokeController {

	private final JokeService jokeService;

	JokeController(JokeService jokeService) {
		this.jokeService = jokeService;
	}

	@GetMapping("/jokes")
	JokeResponse joke() {
		return new JokeResponse(this.jokeService.developerJoke());
	}

	record JokeResponse(String joke) {
	}
}
