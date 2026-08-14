package io.iprf.transaction;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

/**
 * Entry point of the IPRF modular monolith.
 *
 * <p>Scanning is rooted at {@code io.iprf} so that sibling modules on the classpath
 * (risk-engine, risk-state, audit, ...) are discovered without this module having
 * to know about each of them individually.
 */
@SpringBootApplication(scanBasePackages = "io.iprf")
@ConfigurationPropertiesScan("io.iprf")
public class TransactionApiApplication {

    public static void main(String[] args) {
        SpringApplication.run(TransactionApiApplication.class, args);
    }
}
