package com.monitorpc.monitor_pc.service;

import com.monitorpc.monitor_pc.dto.AlertResponseDTO;
import com.monitorpc.monitor_pc.enums.*;
import com.monitorpc.monitor_pc.model.User;
import com.monitorpc.monitor_pc.mapper.AlertMapper;
import com.monitorpc.monitor_pc.model.*;
import com.monitorpc.monitor_pc.repository.AlertRepository;
import com.monitorpc.monitor_pc.repository.AlertRuleRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AlertEvaluationServiceTest {

    @Mock AlertRuleRepository alertRuleRepository;
    @Mock AlertRepository alertRepository;
    @Mock SimpMessagingTemplate messagingTemplate;
    @Mock AlertMapper alertMapper;

    @InjectMocks AlertEvaluationService service;

    private Machine machine;
    private SystemMetric highCpuMetric;
    private SystemMetric lowCpuMetric;
    private AlertRule cpuRule;

    @BeforeEach
    void setUp() {
        User owner = User.builder().username("test-owner").role("ROLE_USER").build();
        machine = Machine.builder()
                .machineId("test-host")
                .status(MachineStatus.ONLINE)
                .lastSeen(Instant.now())
                .owner(owner)
                .build();

        highCpuMetric = SystemMetric.builder()
                .machine(machine).cpuPercent(90.0).ramPercent(50.0).diskPercent(40.0)
                .ramUsedGb(8.0).diskFreeGb(100.0).uptimeSeconds(3600L).recordedAt(Instant.now())
                .build();

        lowCpuMetric = SystemMetric.builder()
                .machine(machine).cpuPercent(20.0).ramPercent(50.0).diskPercent(40.0)
                .ramUsedGb(8.0).diskFreeGb(100.0).uptimeSeconds(3600L).recordedAt(Instant.now())
                .build();

        cpuRule = AlertRule.builder()
                .metricType(MetricType.CPU_PERCENT)
                .alertOperator(AlertOperator.HIGHER)
                .alertSeverity(AlertSeverity.HIGH)
                .threshold(85)
                .enabled(true)
                .build();
    }

    @Test
    void evaluate_conditionMet_noOngoing_createsAlert() {
        when(alertRuleRepository.findApplicableRules(eq(machine), any())).thenReturn(List.of(cpuRule));
        when(alertRepository.findByAlertRuleAndMachineAndStatus(cpuRule, machine, AlertStatus.ONGOING))
                .thenReturn(Optional.empty());
        when(alertMapper.toDTO(any(Alert.class))).thenReturn(mock(AlertResponseDTO.class));

        service.evaluate(machine, highCpuMetric);

        verify(alertRepository).save(argThat(alert ->
                alert.getStatus() == AlertStatus.ONGOING && alert.getTriggeredValue() == 90.0));
        verify(messagingTemplate).convertAndSend(eq("/topic/alerts"), any(Object.class));
    }

    @Test
    void evaluate_conditionNotMet_ongoingExists_resolvesAlert() {
        Alert ongoing = Alert.builder()
                .alertRule(cpuRule).machine(machine)
                .triggeredValue(90.0).status(AlertStatus.ONGOING).build();

        when(alertRuleRepository.findApplicableRules(eq(machine), any())).thenReturn(List.of(cpuRule));
        when(alertRepository.findByAlertRuleAndMachineAndStatus(cpuRule, machine, AlertStatus.ONGOING))
                .thenReturn(Optional.of(ongoing));
        when(alertMapper.toDTO(any(Alert.class))).thenReturn(mock(AlertResponseDTO.class));

        service.evaluate(machine, lowCpuMetric);

        assertThat(ongoing.getStatus()).isEqualTo(AlertStatus.RESOLVED);
        assertThat(ongoing.getResolvedAt()).isNotNull();
        verify(alertRepository).save(ongoing);
        verify(messagingTemplate).convertAndSend(eq("/topic/alerts"), any(Object.class));
    }

    @Test
    void evaluate_conditionMet_ongoingExists_noNewAlert() {
        Alert ongoing = Alert.builder()
                .alertRule(cpuRule).machine(machine)
                .triggeredValue(90.0).status(AlertStatus.ONGOING).build();

        when(alertRuleRepository.findApplicableRules(eq(machine), any())).thenReturn(List.of(cpuRule));
        when(alertRepository.findByAlertRuleAndMachineAndStatus(cpuRule, machine, AlertStatus.ONGOING))
                .thenReturn(Optional.of(ongoing));

        service.evaluate(machine, highCpuMetric);

        verify(alertRepository, never()).save(any());
        verify(messagingTemplate, never()).convertAndSend(anyString(), any(Object.class));
    }

    @Test
    void evaluate_conditionNotMet_noOngoing_noAction() {
        when(alertRuleRepository.findApplicableRules(eq(machine), any())).thenReturn(List.of(cpuRule));
        when(alertRepository.findByAlertRuleAndMachineAndStatus(cpuRule, machine, AlertStatus.ONGOING))
                .thenReturn(Optional.empty());

        service.evaluate(machine, lowCpuMetric);

        verify(alertRepository, never()).save(any());
        verify(messagingTemplate, never()).convertAndSend(anyString(), any(Object.class));
    }

    @Test
    void evaluate_noRules_doesNothing() {
        when(alertRuleRepository.findApplicableRules(eq(machine), any())).thenReturn(List.of());

        service.evaluate(machine, highCpuMetric);

        verify(alertRepository, never()).findByAlertRuleAndMachineAndStatus(any(), any(), any());
    }

    @Test
    void evaluate_ramRule_higherOrEqual_triggersCorrectly() {
        AlertRule ramRule = AlertRule.builder()
                .metricType(MetricType.RAM_PERCENT)
                .alertOperator(AlertOperator.HIGHER_OR_EQUAL)
                .alertSeverity(AlertSeverity.MEDIUM)
                .threshold(50)
                .enabled(true)
                .build();

        when(alertRuleRepository.findApplicableRules(eq(machine), any())).thenReturn(List.of(ramRule));
        when(alertRepository.findByAlertRuleAndMachineAndStatus(ramRule, machine, AlertStatus.ONGOING))
                .thenReturn(Optional.empty());
        when(alertMapper.toDTO(any(Alert.class))).thenReturn(mock(AlertResponseDTO.class));

        service.evaluate(machine, highCpuMetric);

        verify(alertRepository).save(argThat(a -> a.getStatus() == AlertStatus.ONGOING));
    }
}
