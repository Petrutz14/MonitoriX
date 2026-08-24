package com.monitorpc.monitor_pc.model;

import com.monitorpc.monitor_pc.enums.AlertOperator;
import com.monitorpc.monitor_pc.enums.AlertSeverity;
import com.monitorpc.monitor_pc.enums.MetricType;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

@Entity
@Setter
@Getter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class AlertRule {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "machine_id")
    private Machine machine;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "owner_id")
    private User owner;
    @Enumerated(EnumType.STRING)
    private MetricType metricType;
    @Enumerated(EnumType.STRING)
    private AlertOperator alertOperator;
    @Enumerated(EnumType.STRING)
    private AlertSeverity alertSeverity;
    @Column(nullable = false)
    private Integer threshold;
    private Boolean enabled;
    @Builder.Default
    private Instant createdAt=Instant.now();

}
