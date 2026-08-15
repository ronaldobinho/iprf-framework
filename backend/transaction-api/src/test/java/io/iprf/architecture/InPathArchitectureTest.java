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

    /** Every package that participates in transaction authorization. */
    private static final String[] IN_PATH_PACKAGES = {
            "io.iprf.engine..",
            "io.iprf.network..",
    };

    /**
     * Persistence access that implies a live query.
     *
     * <p>Note what is <em>not</em> here: {@code org.springframework.data.redis}.
     * The rule forbids querying a transactional datastore on the payment path,
     * and Redis is where pre-computed state deliberately lives — reading it
     * in-path is the design, not a violation. Banning the whole
     * {@code org.springframework.data} namespace would have been easier to write
     * and would have banned the thing the architecture requires.
     */
    private static final String[] PERSISTENCE_PACKAGES = {
            "jakarta.persistence..",
            "javax.persistence..",
            "org.springframework.data.jpa..",
            "org.springframework.data.repository..",
            "org.springframework.data.relational..",
            "org.springframework.data.jdbc..",
            "org.springframework.jdbc..",
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
    static final ArchRule inPathMustNotReachPersistence = noClasses()
            .that().resideInAnyPackage(IN_PATH_PACKAGES)
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
     * The in-path layers must not make outbound calls either. An external call
     * has unbounded latency, independent availability and non-deterministic
     * results — each of which alone breaks the in-path contract.
     */
    @ArchTest
    static final ArchRule inPathMustNotCallOut = noClasses()
            .that().resideInAnyPackage(IN_PATH_PACKAGES)
            .should().dependOnClassesThat().resideInAnyPackage(
                    "java.net.http..",
                    "org.springframework.web.client..",
                    "org.springframework.web.reactive.function.client..",
                    "okhttp3..",
                    "org.apache.hc..")
            .because("External enrichment is Layer 4 and is asynchronous by classification. "
                    + "An outbound call from an in-path layer is the classification being violated.");

    /**
     * In-path layers reach pre-computed state through the store interfaces, not
     * through a Redis client directly.
     *
     * <p>Without this, an evaluator could hold a {@code RedisTemplate} and issue
     * whatever command it liked — including a scan or a sort, which are
     * unbounded. Going through the interface is what keeps the access pattern a
     * single keyed lookup.
     */
    @ArchTest
    static final ArchRule inPathMustNotHoldARedisClient = noClasses()
            .that().resideInAnyPackage(IN_PATH_PACKAGES)
            .should().dependOnClassesThat().resideInAnyPackage(
                    "org.springframework.data.redis..",
                    "io.lettuce..",
                    "redis.clients..")
            .because("Evaluators read state through RiskStateStore, whose contract is a single "
                    + "keyed lookup. A raw client would let an unbounded command onto the "
                    + "payment path.");

    /**
     * In-path layers must not acquire write access to risk state.
     *
     * <p>{@code RiskStateWriter} exists as a separate interface precisely so that
     * "Layer 3 cannot populate what it reads" is a compile-time property. This
     * rule is what stops someone from injecting the concrete store, which
     * implements both.
     */
    @ArchTest
    static final ArchRule inPathMustNotWriteRiskState = noClasses()
            .that().resideInAnyPackage(IN_PATH_PACKAGES)
            .should().dependOnClassesThat().haveFullyQualifiedName("io.iprf.state.RiskStateWriter")
            .because("Risk state is populated asynchronously. An in-path writer would mean the "
                    + "payment path can change the state a later decision reads, on the path.");
}
