package com.preparation.moviebooking.util;

/**
 * Thread-safe Singleton using Double-Checked Locking (DCL).
 *
 * Why volatile?
 *   Without `volatile`, the JVM / CPU may reorder instructions so that
 *   `singleton` is visible to other threads *before* the constructor body
 *   has finished executing (partial construction). `volatile` prevents this
 *   by establishing a happens-before relationship.
 *
 * Why two null-checks?
 *   - Outer check   → avoids the (expensive) synchronized block on every call
 *                     once the instance has been created.
 *   - Inner check   → guards against two threads that both passed the outer
 *                     check before the first one entered the synchronized block.
 */
public class SingletonExample {

    // volatile guarantees visibility and prevents instruction reordering
    private static volatile SingletonExample singleton = null;

    // Private constructor prevents direct instantiation
    private SingletonExample() {}

    public static SingletonExample getInstance() {
        if (singleton == null) {                          // first check (no lock)
            synchronized (SingletonExample.class) {
                if (singleton == null) {                  // second check (with lock)
                    singleton = new SingletonExample();
                }
            }
        }
        return singleton;
    }
}

