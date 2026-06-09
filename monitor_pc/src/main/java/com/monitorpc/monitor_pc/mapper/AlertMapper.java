package com.monitorpc.monitor_pc.mapper;

import com.monitorpc.monitor_pc.dto.AlertResponseDTO;
import com.monitorpc.monitor_pc.dto.AlertRuleResponseDTO;
import com.monitorpc.monitor_pc.model.Alert;
import com.monitorpc.monitor_pc.model.AlertRule;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface AlertMapper {

    @Mapping(source = "machine.machineId", target = "machineId")
    AlertRuleResponseDTO toDTO(AlertRule rule);

    @Mapping(source = "alertRule.id", target = "alertRuleId")
    @Mapping(source = "machine.machineId", target = "machineId")
    @Mapping(source = "machine.displayName", target = "displayName")
    @Mapping(source = "alertRule.metricType", target = "metricType")
    @Mapping(source = "alertRule.alertSeverity", target = "alertSeverity")
    @Mapping(source = "alertRule.alertOperator", target = "alertOperator")
    @Mapping(source = "alertRule.threshold", target = "threshold")
    AlertResponseDTO toDTO(Alert alert);
}
