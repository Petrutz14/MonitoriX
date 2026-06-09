package com.monitorpc.monitor_pc.model;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Setter
@Getter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class DiskPartition {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "metric_id")
    private SystemMetric systemMetric;
    private String device;
    private String mountPoint;
    private String fileSystem;
    private Double totalGb;
    private Double usedGb;
    private Double percent;
}
