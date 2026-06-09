package com.monitorpc.monitor_pc.model;

import jakarta.persistence.*;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Builder
public class TopProcess {
    @Id
    @GeneratedValue (strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "metric_id")
    private SystemMetric systemMetric;
    private Integer pid;
    private String name;
    private Double cpuPercent;
    private Double ramPercent;
    private Double ramUsedMb;
}
