package com.monitorpc.monitor_pc.dto;

import com.monitorpc.monitor_pc.enums.AlertOperator;
import com.monitorpc.monitor_pc.enums.AlertSeverity;
import com.monitorpc.monitor_pc.enums.AlertStatus;
import com.monitorpc.monitor_pc.enums.MetricType;
import lombok.Builder;
import lombok.Data;

import java.time.Instant;

@Data
@Builder
public class AlertResponseDTO {
    private Long id;
    private Long alertRuleId;
    private String machineId;
    private String displayName;
    private MetricType metricType;
    private AlertSeverity alertSeverity;
    private AlertOperator alertOperator;
    private Integer threshold;
    private Double triggeredValue;
    private AlertStatus status;
    private Instant triggeredAt;
    private Instant resolvedAt;
}
