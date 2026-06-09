package com.monitorpc.monitor_pc.model;

import com.monitorpc.monitor_pc.enums.AlertStatus;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

@Entity
@Table(name = "alert")
@Setter
@Getter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class Alert {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "alert_rule_id", nullable = false)
    private AlertRule alertRule;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "machine_id", nullable = false)
    private Machine machine;

    private Double triggeredValue;

    @Enumerated(EnumType.STRING)
    private AlertStatus status;

    @Builder.Default
    private Instant triggeredAt = Instant.now();

    private Instant resolvedAt;
}
