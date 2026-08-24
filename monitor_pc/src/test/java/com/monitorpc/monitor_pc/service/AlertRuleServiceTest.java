package com.monitorpc.monitor_pc.service;

import com.monitorpc.monitor_pc.dto.AlertRuleResponseDTO;
import com.monitorpc.monitor_pc.enums.AlertStatus;
import com.monitorpc.monitor_pc.exception.ResourceNotFound;
import com.monitorpc.monitor_pc.mapper.AlertMapper;
import com.monitorpc.monitor_pc.model.Alert;
import com.monitorpc.monitor_pc.model.AlertRule;
import com.monitorpc.monitor_pc.repository.AlertRepository;
import com.monitorpc.monitor_pc.repository.AlertRuleRepository;
import com.monitorpc.monitor_pc.repository.MachineRepository;
import com.monitorpc.monitor_pc.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AlertRuleServiceTest {

    @Mock AlertRuleRepository alertRuleRepository;
    @Mock AlertRepository alertRepository;
    @Mock MachineRepository machineRepository;
    @Mock UserRepository userRepository;
    @Mock AlertMapper alertMapper;
    @Mock SimpMessagingTemplate messagingTemplate;

    @InjectMocks AlertRuleService service;

    private static final String USER = "test-user";

    @Test
    void toggleRule_disable_resolvesOngoingAlerts() {
        AlertRule rule = AlertRule.builder().enabled(true).build();
        Alert ongoing = Alert.builder().status(AlertStatus.ONGOING).build();

        when(alertRuleRepository.findByIdAndOwnerUsername(1L, USER)).thenReturn(Optional.of(rule));
        when(alertRuleRepository.save(rule)).thenReturn(rule);
        when(alertRepository.findByAlertRuleAndStatus(rule, AlertStatus.ONGOING))
                .thenReturn(List.of(ongoing));
        when(alertMapper.toDTO(rule)).thenReturn(mock(AlertRuleResponseDTO.class));
        when(alertMapper.toDTO(ongoing)).thenReturn(mock(com.monitorpc.monitor_pc.dto.AlertResponseDTO.class));

        service.toggleRule(1L, false, USER);

        assertThat(rule.getEnabled()).isFalse();
        assertThat(ongoing.getStatus()).isEqualTo(AlertStatus.RESOLVED);
        assertThat(ongoing.getResolvedAt()).isNotNull();
        verify(alertRepository).save(ongoing);
        verify(messagingTemplate).convertAndSend(eq("/topic/alerts"), any(Object.class));
    }

    @Test
    void toggleRule_enable_doesNotTouchAlerts() {
        AlertRule rule = AlertRule.builder().enabled(false).build();

        when(alertRuleRepository.findByIdAndOwnerUsername(1L, USER)).thenReturn(Optional.of(rule));
        when(alertRuleRepository.save(rule)).thenReturn(rule);
        when(alertMapper.toDTO(rule)).thenReturn(mock(AlertRuleResponseDTO.class));

        service.toggleRule(1L, true, USER);

        assertThat(rule.getEnabled()).isTrue();
        verify(alertRepository, never()).findByAlertRuleAndStatus(any(), any());
        verify(messagingTemplate, never()).convertAndSend(anyString(), any(Object.class));
    }

    @Test
    void toggleRule_disable_multipleOngoing_resolvesAll() {
        AlertRule rule = AlertRule.builder().enabled(true).build();
        Alert a1 = Alert.builder().status(AlertStatus.ONGOING).build();
        Alert a2 = Alert.builder().status(AlertStatus.ONGOING).build();

        when(alertRuleRepository.findByIdAndOwnerUsername(2L, USER)).thenReturn(Optional.of(rule));
        when(alertRuleRepository.save(rule)).thenReturn(rule);
        when(alertRepository.findByAlertRuleAndStatus(rule, AlertStatus.ONGOING))
                .thenReturn(List.of(a1, a2));
        when(alertMapper.toDTO(rule)).thenReturn(mock(AlertRuleResponseDTO.class));
        when(alertMapper.toDTO(any(Alert.class))).thenReturn(mock(com.monitorpc.monitor_pc.dto.AlertResponseDTO.class));

        service.toggleRule(2L, false, USER);

        assertThat(a1.getStatus()).isEqualTo(AlertStatus.RESOLVED);
        assertThat(a2.getStatus()).isEqualTo(AlertStatus.RESOLVED);
        verify(alertRepository, times(2)).save(any(Alert.class));
        verify(messagingTemplate, times(2)).convertAndSend(eq("/topic/alerts"), any(Object.class));
    }

    @Test
    void toggleRule_disable_noOngoing_noAlertOps() {
        AlertRule rule = AlertRule.builder().enabled(true).build();

        when(alertRuleRepository.findByIdAndOwnerUsername(3L, USER)).thenReturn(Optional.of(rule));
        when(alertRuleRepository.save(rule)).thenReturn(rule);
        when(alertRepository.findByAlertRuleAndStatus(rule, AlertStatus.ONGOING))
                .thenReturn(List.of());
        when(alertMapper.toDTO(rule)).thenReturn(mock(AlertRuleResponseDTO.class));

        service.toggleRule(3L, false, USER);

        verify(alertRepository, never()).save(any(Alert.class));
        verify(messagingTemplate, never()).convertAndSend(anyString(), any(Object.class));
    }

    @Test
    void toggleRule_notFound_throws() {
        when(alertRuleRepository.findByIdAndOwnerUsername(99L, USER)).thenReturn(Optional.empty());
        assertThrows(ResourceNotFound.class, () -> service.toggleRule(99L, false, USER));
    }
}
