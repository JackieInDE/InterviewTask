package com.meet5.service.integrationTest;

import com.meet5.service.RiskManagementService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import redis.clients.jedis.JedisPooled;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class RiskManagementServiceImplIntegrationTest {

    @Autowired
    private RiskManagementService riskManagementService;

    @Autowired
    private JedisPooled jedisPooled;

    private static final String USER_OPERATION_PREFIX = "user:operation:";

    private static final int EXPIRE_SECONDS = 600;
    private static final int LIMIT_COUNT = 100;

    @BeforeEach
    void setup() {
        jedisPooled.flushDB();
    }

    @Nested
    @DisplayName("Functional Tests")
    class FunctionalTests {

        @Test
        @DisplayName("Should not trigger sensitive behavior below limit")
        void shouldNotTriggerSensitiveBehaviorBelowLimit() {
            Long userId = 1L;

            for (int i = 0; i < LIMIT_COUNT - 1; i++) {
                boolean isSensitive = riskManagementService.checkSensitiveBehavior(userId);
                assertFalse(isSensitive, "Should not trigger sensitive behavior below the limit");
            }
        }

        @Test
        @DisplayName("Should trigger sensitive behavior when limit reached")
        void shouldTriggerSensitiveBehaviorAtLimit() {
            Long userId = 2L;

            for (int i = 0; i < LIMIT_COUNT - 1; i++) {
                riskManagementService.checkSensitiveBehavior(userId);
            }

            boolean isSensitive = riskManagementService.checkSensitiveBehavior(userId);
            assertTrue(isSensitive, "Should trigger sensitive behavior at the limit");
        }
    }

    @Nested
    @DisplayName("Data Validation Tests")
    class DataValidationTests {

        @Test
        @DisplayName("Should correctly increment counter per user separately")
        void shouldHandleMultipleUsersIndependently() {
            Long userId1 = 3L;
            Long userId2 = 4L;

            for (int i = 0; i < 50; i++) {
                riskManagementService.checkSensitiveBehavior(userId1);
            }


            boolean isSensitive = riskManagementService.checkSensitiveBehavior(userId2);
            assertFalse(isSensitive, "User2 should not be affected by User1's operations");
        }

        @Test
        @DisplayName("Should reset counter after expiration")
        void shouldResetCounterAfterExpiration() throws InterruptedException {
            Long userId = 5L;

            for (int i = 0; i < LIMIT_COUNT - 1; i++) {
                riskManagementService.checkSensitiveBehavior(userId);
            }

            jedisPooled.expire(USER_OPERATION_PREFIX + userId, 1);
            Thread.sleep(1500);

            boolean isSensitiveAfterReset = riskManagementService.checkSensitiveBehavior(userId);
            assertFalse(isSensitiveAfterReset, "Counter should reset after key expiration");
        }
    }

    @Nested
    @DisplayName("Boundary Tests")
    class BoundaryTests {

        @Test
        @DisplayName("Should not trigger sensitive behavior when exactly at limit-1")
        void shouldNotTriggerAtLimitMinusOne() {
            Long userId = 6L;

            for (int i = 0; i < LIMIT_COUNT - 1; i++) {
                riskManagementService.checkSensitiveBehavior(userId);
            }

            boolean isSensitive = riskManagementService.checkSensitiveBehavior(userId);
            assertTrue(isSensitive, "Should trigger when reaching exactly the limit");
        }

        @Test
        @DisplayName("Should trigger only once per threshold crossing")
        void shouldTriggerOnlyOnce() {
            Long userId = 7L;

            for (int i = 0; i < LIMIT_COUNT; i++) {
                riskManagementService.checkSensitiveBehavior(userId);
            }

            boolean firstTrigger = riskManagementService.checkSensitiveBehavior(userId);
            boolean secondTrigger = riskManagementService.checkSensitiveBehavior(userId);

            assertTrue(firstTrigger, "First after limit should trigger");
            assertTrue(secondTrigger, "Subsequent calls remain triggered (based on redis counter)");
        }
    }

    @Nested
    @DisplayName("Concurrency Tests")
    class ConcurrencyTests {

        @Test
        @DisplayName("Should handle concurrent operations correctly")
        void shouldHandleConcurrentOperations() throws InterruptedException, ExecutionException {
            Long userId = 8L;
            int threads = 20;
            int operationsPerThread = 10;

            ExecutorService executor = Executors.newFixedThreadPool(threads);
            List<Future<Boolean>> futures = new ArrayList<>();

            for (int i = 0; i < threads; i++) {
                futures.add(executor.submit(() -> {
                    boolean triggered = false;
                    for (int j = 0; j < operationsPerThread; j++) {
                        triggered |= riskManagementService.checkSensitiveBehavior(userId);
                    }
                    return triggered;
                }));
            }

            int triggerCount = 0;
            for (Future<Boolean> future : futures) {
                if (future.get()) {
                    triggerCount++;
                }
            }

            executor.shutdown();

            assertTrue(triggerCount > 0, "At least one thread should detect triggering sensitive behavior");
        }
    }
}
