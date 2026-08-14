package io.iprf.transaction;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * Guards the scaffold: the application context must start. Every later session
 * adds beans to this context, so a failure here localizes the breakage
 * immediately instead of surfacing as an unrelated test failure.
 */
@SpringBootTest
class TransactionApiApplicationTests {

    @Test
    void contextLoads() {
        // Deliberately empty: the assertion is that context startup did not throw.
    }
}
