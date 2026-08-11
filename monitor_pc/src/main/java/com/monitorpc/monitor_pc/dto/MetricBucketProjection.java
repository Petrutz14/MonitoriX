package com.monitorpc.monitor_pc.dto;

import java.time.Instant;

public interface MetricBucketProjection {
    Instant getBucket();
    Double getAvgcpu();
    Double getAvgram();
    Double getAvgdisk();
    Double getAvgramusedgb();
    Double getAvgdiskfreegb();
}
