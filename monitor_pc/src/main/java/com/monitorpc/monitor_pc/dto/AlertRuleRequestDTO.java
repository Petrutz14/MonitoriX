package com.monitorpc.monitor_pc.dto;

import com.monitorpc.monitor_pc.enums.AlertOperator;
import com.monitorpc.monitor_pc.enums.AlertSeverity;
import com.monitorpc.monitor_pc.enums.MetricType;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class AlertRuleRequestDTO {
    private String machineId; // null = global rule
    private MetricType metricType;
    private AlertOperator alertOperator;
    private AlertSeverity alertSeverity;
    private Integer threshold;
    private Boolean enabled;
}
