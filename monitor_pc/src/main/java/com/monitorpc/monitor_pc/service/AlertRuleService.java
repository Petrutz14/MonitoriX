package com.monitorpc.monitor_pc.service;

import com.monitorpc.monitor_pc.dto.AlertRuleRequestDTO;
import com.monitorpc.monitor_pc.dto.AlertRuleResponseDTO;
import com.monitorpc.monitor_pc.exception.ResourceNotFound;
import com.monitorpc.monitor_pc.enums.AlertStatus;
import com.monitorpc.monitor_pc.mapper.AlertMapper;
import com.monitorpc.monitor_pc.model.Alert;
import com.monitorpc.monitor_pc.model.AlertRule;
import com.monitorpc.monitor_pc.model.Machine;
import com.monitorpc.monitor_pc.repository.AlertRepository;
import com.monitorpc.monitor_pc.repository.AlertRuleRepository;
import com.monitorpc.monitor_pc.repository.MachineRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AlertRuleService {

    private final AlertRuleRepository alertRuleRepository;
    private final AlertRepository alertRepository;
    private final MachineRepository machineRepository;
    private final AlertMapper alertMapper;
    private final SimpMessagingTemplate messagingTemplate;

    public AlertRuleResponseDTO createRule(AlertRuleRequestDTO request) {
        Machine machine = null;
        if (request.getMachineId() != null) {
            machine = machineRepository.findByMachineId(request.getMachineId())
                    .orElseThrow(() -> new ResourceNotFound("Machine not found: " + request.getMachineId()));
        }

        AlertRule rule = AlertRule.builder()
                .machine(machine)
                .metricType(request.getMetricType())
                .alertOperator(request.getAlertOperator())
                .alertSeverity(request.getAlertSeverity())
                .threshold(request.getThreshold())
                .enabled(request.getEnabled() != null ? request.getEnabled() : true)
                .build();

        return alertMapper.toDTO(alertRuleRepository.save(rule));
    }

    public List<AlertRuleResponseDTO> getAllRules() {
        return alertRuleRepository.findAll().stream().map(alertMapper::toDTO).toList();
    }

    public List<AlertRuleResponseDTO> getGlobalRules() {
        return alertRuleRepository.findByMachineIsNull().stream().map(alertMapper::toDTO).toList();
    }

    public List<AlertRuleResponseDTO> getRulesForMachine(String machineId) {
        Machine machine = machineRepository.findByMachineId(machineId)
                .orElseThrow(() -> new ResourceNotFound("Machine not found: " + machineId));
        return alertRuleRepository.findByMachine(machine).stream().map(alertMapper::toDTO).toList();
    }

    public void deleteRule(Long id) {
        if (!alertRuleRepository.existsById(id)) {
            throw new ResourceNotFound("Alert rule not found: " + id);
        }
        alertRuleRepository.deleteById(id);
    }

    public AlertRuleResponseDTO toggleRule(Long id, Boolean enabled) {
        AlertRule rule = alertRuleRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFound("Alert rule not found: " + id));
        rule.setEnabled(enabled);
        alertRuleRepository.save(rule);

        if (!enabled) {
            List<Alert> ongoingAlerts = alertRepository.findByAlertRuleAndStatus(rule, AlertStatus.ONGOING);
            for (Alert alert : ongoingAlerts) {
                alert.setStatus(AlertStatus.RESOLVED);
                alert.setResolvedAt(Instant.now());
                alertRepository.save(alert);
                messagingTemplate.convertAndSend("/topic/alerts", alertMapper.toDTO(alert));
            }
        }

        return alertMapper.toDTO(rule);
    }
}
