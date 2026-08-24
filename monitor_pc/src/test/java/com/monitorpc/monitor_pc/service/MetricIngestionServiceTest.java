package com.monitorpc.monitor_pc.service;

import com.monitorpc.monitor_pc.dto.AgentPayloadDTO;
import com.monitorpc.monitor_pc.dto.MetricResponseDTO;
import com.monitorpc.monitor_pc.enums.MachineStatus;
import com.monitorpc.monitor_pc.mapper.MetricMapper;
import com.monitorpc.monitor_pc.model.Machine;
import com.monitorpc.monitor_pc.model.SystemMetric;
import com.monitorpc.monitor_pc.model.User;
import com.monitorpc.monitor_pc.repository.*;
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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MetricIngestionServiceTest {

    @Mock MachineRepository machineRepository;
    @Mock SystemMetricRepository systemMetricRepository;
    @Mock SimpMessagingTemplate simpMessagingTemplate;
    @Mock TopProcessRepository topProcessRepository;
    @Mock DiskPartitionRepository diskPartitionRepository;
    @Mock AlertEvaluationService alertEvaluationService;
    @Mock MetricMapper metricMapper;
    @Mock UserRepository userRepository;

    @InjectMocks MetricIngestionService service;

    private static final String AGENT = "test-agent";
    private User agentUser;
    private AgentPayloadDTO payload;

    @BeforeEach
    void setUp() {
        agentUser = User.builder().username(AGENT).role("ROLE_AGENT").build();

        payload = new AgentPayloadDTO();
        payload.setMachineId("test-host");
        payload.setCpuPercent(45.0);
        payload.setRamPercent(60.0);
        payload.setDiskPercent(30.0);
        payload.setRamUsedGb(8.0);
        payload.setDiskFreeGb(50.0);
        payload.setUptimeSeconds(3600L);
        payload.setOsName("Windows 11");
        payload.setIpAddress("192.168.1.1");
        payload.setTotalRamGb(16.0);
        payload.setTopProcesses(List.of());
        payload.setDiskPartitions(List.of());
    }

    @Test
    void ingest_newMachine_createsAndSavesAll() {
        when(userRepository.findByUsername(AGENT)).thenReturn(Optional.of(agentUser));
        when(machineRepository.findByMachineId("test-host")).thenReturn(Optional.empty());
        when(machineRepository.save(any(Machine.class))).thenAnswer(i -> i.getArgument(0));
        when(systemMetricRepository.save(any(SystemMetric.class))).thenAnswer(i -> i.getArgument(0));
        when(metricMapper.toDTO(any(), any(), any(), any())).thenReturn(mock(MetricResponseDTO.class));

        service.ingest(payload, AGENT);

        verify(machineRepository, times(2)).save(any(Machine.class));
        verify(systemMetricRepository).save(any(SystemMetric.class));
        verify(alertEvaluationService).evaluate(any(Machine.class), any(SystemMetric.class));
        verify(simpMessagingTemplate).convertAndSend(eq("/topic/metrics"), any(Object.class));
    }

    @Test
    void ingest_existingMachine_updatesStatusToOnline() {
        Machine existing = Machine.builder()
                .machineId("test-host")
                .status(MachineStatus.OFFLINE)
                .lastSeen(Instant.now().minusSeconds(120))
                .owner(agentUser)
                .build();

        when(userRepository.findByUsername(AGENT)).thenReturn(Optional.of(agentUser));
        when(machineRepository.findByMachineId("test-host")).thenReturn(Optional.of(existing));
        when(machineRepository.save(any(Machine.class))).thenAnswer(i -> i.getArgument(0));
        when(systemMetricRepository.save(any(SystemMetric.class))).thenAnswer(i -> i.getArgument(0));
        when(metricMapper.toDTO(any(), any(), any(), any())).thenReturn(mock(MetricResponseDTO.class));

        service.ingest(payload, AGENT);

        assertThat(existing.getStatus()).isEqualTo(MachineStatus.ONLINE);
        assertThat(existing.getLastSeen()).isNotNull();
        verify(machineRepository, times(1)).save(any(Machine.class));
        verify(alertEvaluationService).evaluate(eq(existing), any(SystemMetric.class));
    }

    @Test
    void ingest_nullProcessesAndPartitions_handlesGracefully() {
        payload.setTopProcesses(null);
        payload.setDiskPartitions(null);

        when(userRepository.findByUsername(AGENT)).thenReturn(Optional.of(agentUser));
        when(machineRepository.findByMachineId("test-host")).thenReturn(Optional.empty());
        when(machineRepository.save(any(Machine.class))).thenAnswer(i -> i.getArgument(0));
        when(systemMetricRepository.save(any(SystemMetric.class))).thenAnswer(i -> i.getArgument(0));
        when(metricMapper.toDTO(any(), any(), any(), any())).thenReturn(mock(MetricResponseDTO.class));

        service.ingest(payload, AGENT);

        verify(topProcessRepository, never()).save(any());
        verify(diskPartitionRepository, never()).save(any());
    }

    @Test
    void ingest_broadcastsToWebSocket() {
        when(userRepository.findByUsername(AGENT)).thenReturn(Optional.of(agentUser));
        when(machineRepository.findByMachineId("test-host")).thenReturn(Optional.empty());
        when(machineRepository.save(any(Machine.class))).thenAnswer(i -> i.getArgument(0));
        when(systemMetricRepository.save(any(SystemMetric.class))).thenAnswer(i -> i.getArgument(0));
        MetricResponseDTO dto = mock(MetricResponseDTO.class);
        when(metricMapper.toDTO(any(), any(), any(), any())).thenReturn(dto);

        service.ingest(payload, AGENT);

        verify(simpMessagingTemplate).convertAndSend("/topic/metrics", dto);
    }
}
