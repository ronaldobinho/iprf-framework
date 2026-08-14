package io.iprf.transaction.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** OpenAPI document metadata. The spec itself is generated from the controllers. */
@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI iprfOpenApi(@Value("${iprf.framework-version}") String frameworkVersion) {
        return new OpenAPI().info(new Info()
                .title("IPRF — Instant Payment Fraud & Resilience Framework")
                .version(frameworkVersion)
                .description("""
                        Reference implementation of a deterministic, explainable fraud decision \
                        engine for irrevocable instant-payment rails.

                        **All data served by this API is SYNTHETIC / DEMO DATA.** It is a \
                        reference implementation demonstrating a methodology, not a production \
                        fraud service, and no figure it returns describes any real institution.

                        Decisions are reproducible: the same input under the same rule versions \
                        always produces the same output, and no live database query is performed \
                        during evaluation.""")
                .license(new License().name("Apache-2.0").url("https://www.apache.org/licenses/LICENSE-2.0")));
    }
}
