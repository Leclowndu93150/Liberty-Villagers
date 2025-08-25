package com.leclowndu93150.libertyvillagers.util;

import net.minecraft.util.RandomSource;

public class JitteredLinearRetry {
    private static final int MIN_INTERVAL_INCREASE = 40;
    private static final int MAX_INTERVAL_INCREASE = 80;
    private static final int MAX_RETRY_PATHFINDING_INTERVAL = 400;
    private final RandomSource random;
    private long previousAttemptTimestamp;
    private long nextScheduledAttemptTimestamp;
    private int currentDelay;

    public JitteredLinearRetry(RandomSource random, long timestamp) {
        this.random = random;
        this.markAttempt(timestamp);
    }

    public void markAttempt(long timestamp) {
        this.previousAttemptTimestamp = timestamp;
        int i = this.currentDelay + this.random.nextInt(40) + 40;
        this.currentDelay = Math.min(i, 400);
        this.nextScheduledAttemptTimestamp = timestamp + (long)this.currentDelay;
    }

    public boolean isStillValid(long timestamp) {
        return timestamp - this.previousAttemptTimestamp < 400L;
    }

    public boolean shouldRetry(long timestamp) {
        return timestamp >= this.nextScheduledAttemptTimestamp;
    }

    public String toString() {
        return "RetryMarker{, previousAttemptAt="
                + this.previousAttemptTimestamp
                + ", nextScheduledAttemptAt="
                + this.nextScheduledAttemptTimestamp
                + ", currentDelay="
                + this.currentDelay
                + "}";
    }
}
