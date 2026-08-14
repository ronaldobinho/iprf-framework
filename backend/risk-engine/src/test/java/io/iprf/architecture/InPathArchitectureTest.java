package io.iprf.architecture;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

/**
 * Structural enforcement of the framework's one non-negotiable principle.
 *
 * <p>The in-path modules must read pre-computed state only — no live database
 * query during transaction authorization. That rule is exactly the kind that
 * erodes: it gets violated by a well-intentioned change, under deadline, by
 * someone who needs one more piece of data and notices that a repository is
 * right there. Code review is not a reliable guard against it, because the
 * change looks reasonable in isolation.
 *
 * <p>So the build enforces it. If these tests fail, the fix is to move the work
 * off the payment path — not to relax the rule.
 *
 * <p>See {@code docs/framework/methodology.md} and
 * {@code docs/framework/architecture.md}.
 */
@AnalyzeClasses(
        packages = "io.iprf",
        importOptions = ImportOption.DoNotIncludeTests.class)
class InPathArchitectureTest {

    private static final String[] PERSISTENCE_PACKAGES = {
            "jakarta.persistence..",
            "javax.persistence..",
            "org.springframework.data..",
            "org.hibernate..",
            "java.sql..",
            "javax.sql..",
            "com.zaxxer.hikari..",
    };

    /**
     * The core rule. A JPA entity manager, a Spring Data repository or a raw JDBC
     * connection on the authorization path means unbounded latency governed by
     * the query planner, index state and connection pool saturation — none of
     * which are properties of the rule that issued it.
     */
    @ArchTest
    static final ArchRule inPathEngineMustNotReachPersistence = noClasses()
            .that().resideInAnyPackage("io.iprf.engine..")
            .should().dependOnClassesThat().resideInAnyPackage(PERSISTENCE_PACKAGES)
            .because("Layers 1-3 run in-path and must read pre-computed state only. "
                    + "A live query here converts a fraud control into an availability incident "
                    + "under exactly the load that triggers it.");

    /**
     * The state layer is the boundary. It may talk to Redis; it may not become a
     * database facade, because then every in-path caller inherits a database
     * dependency it cannot see.
     */
    @ArchTest
    static final ArchRule stateAccessMustNotReachPersistence = noClasses()
            .that().resideInAnyPackage("io.iprf.state..")
            .should().dependOnClassesThat().resideInAnyPackage(PERSISTENCE_PACKAGES)
            .because("Pre-computed state is populated out of band. If the store can query, "
                    + "the in-path guarantee is only a convention.");

    /**
     * Domain types stay free of persistence annotations so the audit module can
     * map them to storage without the in-path modules inheriting a JPA
     * dependency through the shared types.
     */
    @ArchTest
    static final ArchRule domainMustStayPersistenceFree = noClasses()
            .that().resideInAnyPackage("io.iprf.domain..")
            .should().dependOnClassesThat().resideInAnyPackage(PERSISTENCE_PACKAGES)
            .because("Domain records are shared with in-path modules; annotating them for "
                    + "persistence would drag JPA onto the payment path through the back door.");

    /**
     * The engine must not make outbound calls either. An external call has
     * unbounded latency, independent availability and non-deterministic results
     * — each of which alone breaks the in-path contract.
     */
    @ArchTest
    static final ArchRule inPathEngineMustNotCallOut = noClasses()
            .that().resideInAnyPackage("io.iprf.engine..")
            .should().dependOnClassesThat().resideInAnyPackage(
                    "java.net.http..",
                    "org.springframework.web.client..",
                    "org.springframework.web.reactive.function.client..",
                    "okhttp3..",
                    "org.apache.hc..")
            .because("External enrichment is Layer 4 and is asynchronous by classification. "
                    + "An outbound call from an in-path layer is the classification being violated.");
}
