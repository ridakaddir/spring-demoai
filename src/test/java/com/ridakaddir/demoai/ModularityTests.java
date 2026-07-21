package com.ridakaddir.demoai;

import org.junit.jupiter.api.Test;
import org.springframework.modulith.core.ApplicationModules;

/**
 * Verifies the Spring Modulith structure: each direct sub-package of the
 * application root (e.g. {@code joke}) is a module, and modules respect each
 * other's boundaries. Pure static analysis — no Spring context required.
 */
class ModularityTests {

	@Test
	void verifiesModularStructure() {
		ApplicationModules.of(DemoaiApplication.class).verify();
	}
}
