package com.monitorpc.monitor_pc.service;

import com.monitorpc.monitor_pc.dto.AlertRuleRequestDTO;
import com.monitorpc.monitor_pc.dto.AlertRuleResponseDTO;
import com.monitorpc.monitor_pc.exception.ResourceNotFound;
import com.monitorpc.monitor_pc.enums.AlertStatus;
import com.monitorpc.monitor_pc.mapper.AlertMapper;
import com.monitorpc.monitor_pc.model.Alert;
import com.monitorpc.monitor_pc.model.AlertRule;
import com.monitorpc.monitor_pc.model.Machine;
import com.monitorpc.monitor_pc.model.User;
import com.monitorpc.monitor_pc.repository.AlertRepository;
import com.monitorpc.monitor_pc.repository.AlertRuleRepository;
import com.monitorpc.monitor_pc.repository.MachineRepository;
import com.monitorpc.monitor_pc.repository.UserRepository;
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
    private final UserRepository userRepository;
    private final AlertMapper alertMapper;
    private final SimpMessagingTemplate messagingTemplate;

    public AlertRuleResponseDTO createRule(AlertRuleRequestDTO request, String username) {
        User owner = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFound("User not found"));

        Machine machine = null;
        if (request.getMachineId() != null) {
            machine = machineRepository.findByMachineIdAndOwnerUsername(request.getMachineId(), username)
                    .orElseThrow(() -> new ResourceNotFound("Machine not found: " + request.getMachineId()));
        }

        AlertRule rule = AlertRule.builder()
                .machine(machine)
                .owner(owner)
                .metricType(request.getMetricType())
                .alertOperator(request.getAlertOperator())
                .alertSeverity(request.getAlertSeverity())
                .threshold(request.getThreshold())
                .enabled(request.getEnabled() != null ? request.getEnabled() : true)
                .build();

        return alertMapper.toDTO(alertRuleRepository.save(rule));
    }

    public List<AlertRuleResponseDTO> getAllRules(String username) {
        return alertRuleRepository.findByOwnerUsername(username).stream().map(alertMapper::toDTO).toList();
    }

    public List<AlertRuleResponseDTO> getGlobalRules(String username) {
        return alertRuleRepository.findByMachineIsNullAndOwnerUsername(username).stream().map(alertMapper::toDTO).toList();
    }

    public List<AlertRuleResponseDTO> getRulesForMachine(String machineId, String username) {
        Machine machine = machineRepository.findByMachineIdAndOwnerUsername(machineId, username)
                .orElseThrow(() -> new ResourceNotFound("Machine not found: " + machineId));
        return alertRuleRepository.findByMachineAndOwnerUsername(machine, username).stream().map(alertMapper::toDTO).toList();
    }

    public void deleteRule(Long id, String username) {
        AlertRule rule = alertRuleRepository.findByIdAndOwnerUsername(id, username)
                .orElseThrow(() -> new ResourceNotFound("Alert rule not found: " + id));
        alertRuleRepository.delete(rule);
    }

    public AlertRuleResponseDTO toggleRule(Long id, Boolean enabled, String username) {
        AlertRule rule = alertRuleRepository.findByIdAndOwnerUsername(id, username)
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
