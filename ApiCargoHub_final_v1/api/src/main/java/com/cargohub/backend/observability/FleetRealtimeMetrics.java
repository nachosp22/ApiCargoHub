package com.cargohub.backend.observability;

import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.LongAdder;
import org.springframework.stereotype.Component;

@Component
public class FleetRealtimeMetrics {

    private final LongAdder snapshotRequests = new LongAdder();
    private final LongAdder snapshotDegraded = new LongAdder();
    private final LongAdder etaFallback = new LongAdder();
    private final LongAdder trackingWrites = new LongAdder();

    private final LongAdder snapshotLatencyMsTotal = new LongAdder();
    private final AtomicLong snapshotLatencySamples = new AtomicLong(0);

    private final LongAdder etaLatencyMsTotal = new LongAdder();
    private final AtomicLong etaLatencySamples = new AtomicLong(0);

    public void incrementSnapshotRequests() {
        snapshotRequests.increment();
    }

    public void incrementSnapshotDegraded() {
        snapshotDegraded.increment();
    }

    public void incrementEtaFallback() {
        etaFallback.increment();
    }

    public void incrementTrackingWrites() {
        trackingWrites.increment();
    }

    public void recordSnapshotLatency(long durationMs) {
        snapshotLatencyMsTotal.add(durationMs);
        snapshotLatencySamples.incrementAndGet();
    }

    public void recordEtaLatency(long durationMs) {
        etaLatencyMsTotal.add(durationMs);
        etaLatencySamples.incrementAndGet();
    }

    public long getSnapshotRequests() {
        return snapshotRequests.sum();
    }

    public long getSnapshotDegraded() {
        return snapshotDegraded.sum();
    }

    public long getEtaFallback() {
        return etaFallback.sum();
    }

    public long getTrackingWrites() {
        return trackingWrites.sum();
    }

    public long getSnapshotLatencyAvgMs() {
        long samples = snapshotLatencySamples.get();
        if (samples == 0) {
            return 0;
        }
        return snapshotLatencyMsTotal.sum() / samples;
    }

    public long getEtaLatencyAvgMs() {
        long samples = etaLatencySamples.get();
        if (samples == 0) {
            return 0;
        }
        return etaLatencyMsTotal.sum() / samples;
    }
}
