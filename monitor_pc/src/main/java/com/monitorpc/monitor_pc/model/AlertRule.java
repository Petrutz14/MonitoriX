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
    @GeneratedValue
    private Long id;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "machine_id")
    private Machine machine;
    @Enumerated(EnumType.STRING)
    private MetricType metricType;
    @Enumerated(EnumType.STRING)
    private AlertOperator alertOperator;
    @Enumerated(EnumType.STRING)
    private AlertSeverity alertSeverity;
    private Integer threshold;
    private Boolean enabled;
    @Builder.Default
    private Instant createdAt=Instant.now();

}
