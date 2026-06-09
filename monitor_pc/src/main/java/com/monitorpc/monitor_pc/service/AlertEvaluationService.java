package com.monitorpc.monitor_pc.service;

import com.monitorpc.monitor_pc.dto.AlertResponseDTO;
import com.monitorpc.monitor_pc.enums.AlertStatus;
import com.monitorpc.monitor_pc.mapper.AlertMapper;
import com.monitorpc.monitor_pc.model.Alert;
import com.monitorpc.monitor_pc.model.AlertRule;
import com.monitorpc.monitor_pc.model.Machine;
import com.monitorpc.monitor_pc.model.SystemMetric;
import com.monitorpc.monitor_pc.repository.AlertRepository;
import com.monitorpc.monitor_pc.repository.AlertRuleRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class AlertEvaluationService {

    private final AlertRuleRepository alertRuleRepository;
    private final AlertRepository alertRepository;
    private final SimpMessagingTemplate messagingTemplate;
    private final AlertMapper alertMapper;

    @Transactional
    public void evaluate(Machine machine, SystemMetric metric) {
        List<AlertRule> rules = alertRuleRepository.findApplicableRules(machine);

        for (AlertRule rule : rules) {
            double value = extractValue(metric, rule);
            boolean conditionMet = evaluateCondition(value, rule);

            Optional<Alert> ongoingAlert = alertRepository
                    .findByAlertRuleAndMachineAndStatus(rule, machine, AlertStatus.ONGOING);

            if (conditionMet && ongoingAlert.isEmpty()) {
                Alert alert = Alert.builder()
                        .alertRule(rule)
                        .machine(machine)
                        .triggeredValue(value)
                        .status(AlertStatus.ONGOING)
                        .build();
                alertRepository.save(alert);
                messagingTemplate.convertAndSend("/topic/alerts", alertMapper.toDTO(alert));
            } else if (!conditionMet && ongoingAlert.isPresent()) {
                Alert alert = ongoingAlert.get();
                alert.setStatus(AlertStatus.RESOLVED);
                alert.setResolvedAt(Instant.now());
                alertRepository.save(alert);
                messagingTemplate.convertAndSend("/topic/alerts", alertMapper.toDTO(alert));
            }
        }
    }

    @Transactional
    public List<AlertResponseDTO> getAlertsForMachine(String machineId, Machine machine) {
        return alertRepository.findByMachineOrderByTriggeredAtDesc(machine)
                .stream()
                .map(alertMapper::toDTO)
                .toList();
    }

    @Transactional
    public List<AlertResponseDTO> getActiveAlertsForMachine(Machine machine) {
        return alertRepository.findByMachineAndStatusOrderByTriggeredAtDesc(machine, AlertStatus.ONGOING)
                .stream()
                .map(alertMapper::toDTO)
                .toList();
    }

    private double extractValue(SystemMetric metric, AlertRule rule) {
        return switch (rule.getMetricType()) {
            case CPU_PERCENT -> metric.getCpuPercent();
            case RAM_PERCENT -> metric.getRamPercent();
            case DISK_PERCENT -> metric.getDiskPercent();
        };
    }

    private boolean evaluateCondition(double value, AlertRule rule) {
        double threshold = rule.getThreshold();
        return switch (rule.getAlertOperator()) {
            case HIGHER -> value > threshold;
            case HIGHER_OR_EQUAL -> value >= threshold;
            case LOWER -> value < threshold;
            case LOWER_OR_EQUAL -> value <= threshold;
            case EQUAL -> value == threshold;
        };
    }
}
