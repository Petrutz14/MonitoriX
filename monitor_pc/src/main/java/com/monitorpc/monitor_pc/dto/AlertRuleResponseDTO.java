package com.monitorpc.monitor_pc.dto;

import com.monitorpc.monitor_pc.enums.AlertOperator;
import com.monitorpc.monitor_pc.enums.AlertSeverity;
import com.monitorpc.monitor_pc.enums.MetricType;
import lombok.Builder;
import lombok.Data;

import java.time.Instant;

@Data
@Builder
public class AlertRuleResponseDTO {
    private Long id;
    private String machineId; // null = global rule
    private MetricType metricType;
    private AlertOperator alertOperator;
    private AlertSeverity alertSeverity;
    private Integer threshold;
    private Boolean enabled;
    private Instant createdAt;
}
