package com.ridakaddir.demoai;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

/**
 * Runs only the database migration and then exits.
 *
 * <p>Flyway migrates automatically during context startup (before any runner executes),
 * so by the time this runs the schema is already up to date. This simply shuts the
 * application down so it can be used as a one-shot migration task, e.g.:
 *
 * <pre>./mvnw spring-boot:run -Dspring-boot.run.profiles=migrate</pre>
 * <pre>java -jar target/demoai-0.0.1-SNAPSHOT.jar --spring.profiles.active=migrate</pre>
 */
@Component
@Profile("migrate")
class MigrateRunner implements ApplicationRunner {

	private static final Logger log = LoggerFactory.getLogger(MigrateRunner.class);

	private final ConfigurableApplicationContext context;

	MigrateRunner(ConfigurableApplicationContext context) {
		this.context = context;
	}

	@Override
	public void run(ApplicationArguments args) {
		log.info("Flyway migration complete; exiting (migrate profile).");
		int exitCode = SpringApplication.exit(context, () -> 0);
		System.exit(exitCode);
	}
}
