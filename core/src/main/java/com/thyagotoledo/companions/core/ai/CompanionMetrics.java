package com.thyagotoledo.companions.core.ai;

import java.util.concurrent.atomic.AtomicLong;

/** Contadores leves; não armazena prompts, respostas ou dados do mundo. */
public final class CompanionMetrics {
    private final AtomicLong requests = new AtomicLong();
    private final AtomicLong successes = new AtomicLong();
    private final AtomicLong failures = new AtomicLong();
    private final AtomicLong rejected = new AtomicLong();

    void request() { requests.incrementAndGet(); }
    void success() { successes.incrementAndGet(); }
    void failure() { failures.incrementAndGet(); }
    void rejected() { rejected.incrementAndGet(); }

    public Snapshot snapshot() {
        return new Snapshot(requests.get(), successes.get(), failures.get(), rejected.get());
    }

    public static final class Snapshot {
        private final long requests;
        private final long successes;
        private final long failures;
        private final long rejected;

        private Snapshot(long requests, long successes, long failures, long rejected) {
            this.requests = requests;
            this.successes = successes;
            this.failures = failures;
            this.rejected = rejected;
        }

        public long getRequests() { return requests; }
        public long getSuccesses() { return successes; }
        public long getFailures() { return failures; }
        public long getRejected() { return rejected; }
    }
}
