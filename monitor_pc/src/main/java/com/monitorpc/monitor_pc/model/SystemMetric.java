package com.monitorpc.monitor_pc.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

@Entity
@Table(name = "system_metric")
@AllArgsConstructor
@NoArgsConstructor
@Setter
@Getter
@Builder
public class SystemMetric {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "machine_id")
    private Machine machine;
    private Double cpuPercent;
    private Double ramPercent;
    private Double ramUsedGb;
    private Double diskPercent;
    private Double diskFreeGb;
    private Long uptimeSeconds;
    @Column(nullable = false)
    private Instant recordedAt;
}
