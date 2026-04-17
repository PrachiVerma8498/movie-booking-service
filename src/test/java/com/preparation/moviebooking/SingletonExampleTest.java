package com.preparation.moviebooking;

import com.preparation.moviebooking.util.SingletonExample;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Constructor;
import java.util.Collections;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for SingletonExample covering:
 *  1. Basic singleton contract (same reference on repeated calls)
 *  2. Thread-safety under concurrent access (no two threads get different instances)
 *  3. Reflection attack prevention
 */
class SingletonExampleTest {

    // -----------------------------------------------------------------------
    // 1.  Basic singleton contract
    // -----------------------------------------------------------------------

    @Test
    @DisplayName("Same instance is returned on sequential calls")
    void testSameInstanceOnSequentialCalls() {
        SingletonExample first  = SingletonExample.getInstance();
        SingletonExample second = SingletonExample.getInstance();
        SingletonExample third  = SingletonExample.getInstance();

        assertSame(first, second, "Second call must return the same instance");
        assertSame(second, third,  "Third call must return the same instance");
    }

    @Test
    @DisplayName("getInstance() never returns null")
    void testInstanceIsNeverNull() {
        assertNotNull(SingletonExample.getInstance());
    }

    // -----------------------------------------------------------------------
    // 2.  Thread-safety
    // -----------------------------------------------------------------------

    /**
     * Launches THREAD_COUNT threads that all race to call getInstance() at the
     * same moment (synchronised via a CountDownLatch "start gun").
     * After all threads finish, we assert that exactly ONE unique instance was
     * seen across all threads.
     */
    @Test
    @DisplayName("Only one instance is created under high concurrency")
    void testSingleInstanceUnderConcurrency() throws InterruptedException {
        final int THREAD_COUNT = 100;

        Set<SingletonExample> instances = Collections.newSetFromMap(new ConcurrentHashMap<>());
        CountDownLatch startGun  = new CountDownLatch(1);   // blocks all threads until fired
        CountDownLatch doneLatch = new CountDownLatch(THREAD_COUNT);

        ExecutorService executor = Executors.newFixedThreadPool(THREAD_COUNT);

        for (int i = 0; i < THREAD_COUNT; i++) {
            executor.submit(() -> {
                try {
                    startGun.await();                        // wait for the "go" signal
                    instances.add(SingletonExample.getInstance());
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    doneLatch.countDown();
                }
            });
        }

        startGun.countDown();                                // fire — all threads start simultaneously
        boolean finished = doneLatch.await(10, TimeUnit.SECONDS);
        executor.shutdown();

        assertTrue(finished, "All threads should complete within the timeout");
        assertEquals(1, instances.size(),
                "Expected exactly 1 unique singleton instance across " + THREAD_COUNT + " threads, but got: " + instances.size());
    }

    /**
     * Stress-tests the singleton over many repeated runs to expose any
     * intermittent race condition.
     */
    @RepeatedTest(10)
    @DisplayName("Singleton contract holds across repeated concurrent stress runs")
    void testSingletonUnderRepeatedConcurrentStress() throws InterruptedException {
        final int THREAD_COUNT = 50;

        Set<Integer> identityHashCodes = ConcurrentHashMap.newKeySet();
        CountDownLatch startGun  = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(THREAD_COUNT);

        ExecutorService executor = Executors.newFixedThreadPool(THREAD_COUNT);

        for (int i = 0; i < THREAD_COUNT; i++) {
            executor.submit(() -> {
                try {
                    startGun.await();
                    // System.identityHashCode is based on memory address —
                    // distinct objects will (almost always) have distinct codes.
                    identityHashCodes.add(System.identityHashCode(SingletonExample.getInstance()));
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    doneLatch.countDown();
                }
            });
        }

        startGun.countDown();
        boolean finished = doneLatch.await(10, TimeUnit.SECONDS);
        executor.shutdown();

        assertTrue(finished, "All threads should complete within the timeout");
        assertEquals(1, identityHashCodes.size(),
                "All threads must observe the same object identity");
    }

    // -----------------------------------------------------------------------
    // 3.  Reflection attack prevention
    // -----------------------------------------------------------------------

    /**
     * Verifies that trying to create a second instance via reflection is blocked.
     *
     * The SingletonExample constructor is private. An attacker could still call
     * it via reflection. This test documents and enforces that behavior.
     * If the constructor were ever made accessible, this test would catch the regression.
     */
    @Test
    @DisplayName("Private constructor is inaccessible via reflection")
    void testReflectionCannotBreakSingleton() {
        assertThrows(Exception.class, () -> {
            Constructor<SingletonExample> constructor =
                    SingletonExample.class.getDeclaredConstructor();
            // setAccessible(true) is intentionally NOT called here —
            // the constructor must remain inaccessible.
            constructor.newInstance();   // must throw IllegalAccessException
        }, "Reflection must not be able to instantiate SingletonExample without setAccessible(true)");
    }
}


