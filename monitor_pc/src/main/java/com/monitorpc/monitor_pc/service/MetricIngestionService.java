package com.monitorpc.monitor_pc.service;

import com.monitorpc.monitor_pc.dto.AgentPayloadDTO;
import com.monitorpc.monitor_pc.dto.MetricBucketProjection;
import com.monitorpc.monitor_pc.dto.MetricResponseDTO;
import com.monitorpc.monitor_pc.dto.TopProcessDTO;
import com.monitorpc.monitor_pc.enums.MachineStatus;
import com.monitorpc.monitor_pc.exception.ResourceNotFound;
import com.monitorpc.monitor_pc.mapper.MetricMapper;
import com.monitorpc.monitor_pc.model.DiskPartition;
import com.monitorpc.monitor_pc.model.Machine;
import com.monitorpc.monitor_pc.model.SystemMetric;
import com.monitorpc.monitor_pc.model.TopProcess;
import com.monitorpc.monitor_pc.model.User;
import com.monitorpc.monitor_pc.repository.DiskPartitionRepository;
import com.monitorpc.monitor_pc.repository.MachineRepository;
import com.monitorpc.monitor_pc.repository.SystemMetricRepository;
import com.monitorpc.monitor_pc.repository.TopProcessRepository;
import com.monitorpc.monitor_pc.repository.UserRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;


@Service
@RequiredArgsConstructor
public class MetricIngestionService {
    private final MachineRepository machineRepository;
    private final SystemMetricRepository systemMetricRepository;
    private final SimpMessagingTemplate simpMessagingTemplate;
    private final TopProcessRepository topProcessRepository;
    private final DiskPartitionRepository diskPartitionRepository;
    private final AlertEvaluationService alertEvaluationService;
    private final MetricMapper metricMapper;
    private final UserRepository userRepository;

    @Transactional
    public void ingest(AgentPayloadDTO agentPayloadDTO, String ownerUsername) {
        User owner = userRepository.findByUsername(ownerUsername)
                .orElseThrow(() -> new ResourceNotFound("User not found: " + ownerUsername));

        Machine machine = machineRepository
                .findByMachineId(agentPayloadDTO.getMachineId())
                .orElseGet(() -> {
                    Machine newMachine = Machine.builder()
                            .machineId(agentPayloadDTO.getMachineId())
                            .displayName(agentPayloadDTO.getDisplayName() != null
                                    ? agentPayloadDTO.getDisplayName()
                                    : agentPayloadDTO.getMachineId())
                            .status(MachineStatus.ONLINE)
                            .lastSeen(Instant.now())
                            .osName(agentPayloadDTO.getOsName())
                            .ipAddress(agentPayloadDTO.getIpAddress())
                            .totalRamGb(agentPayloadDTO.getTotalRamGb())
                            .owner(owner)
                            .build();
                    return machineRepository.save(newMachine);
                });

        machine.setStatus(MachineStatus.ONLINE);
        machine.setLastSeen(Instant.now());
        machine.setOsName(agentPayloadDTO.getOsName());
        machine.setIpAddress(agentPayloadDTO.getIpAddress());
        machine.setTotalRamGb(agentPayloadDTO.getTotalRamGb());

        SystemMetric systemMetric = SystemMetric.builder()
                .machine(machine)
                .cpuPercent(agentPayloadDTO.getCpuPercent())
                .ramPercent(agentPayloadDTO.getRamPercent())
                .diskPercent(agentPayloadDTO.getDiskPercent())
                .ramUsedGb(agentPayloadDTO.getRamUsedGb())
                .uptimeSeconds(agentPayloadDTO.getUptimeSeconds())
                .diskFreeGb(agentPayloadDTO.getDiskFreeGb())
                .recordedAt(Instant.now())
                .build();

        machineRepository.save(machine);
        systemMetricRepository.save(systemMetric);

        List<TopProcess> savedProcesses = new ArrayList<>();
        if (agentPayloadDTO.getTopProcesses() != null) {
            for (TopProcessDTO dto : agentPayloadDTO.getTopProcesses()) {
                savedProcesses.add(topProcessRepository.save(TopProcess.builder()
                        .systemMetric(systemMetric)
                        .pid(dto.getPid())
                        .name(dto.getName())
                        .cpuPercent(dto.getCpuPercent())
                        .ramPercent(dto.getRamPercent())
                        .ramUsedMb(dto.getRamUsedMb())
                        .build()));
            }
        }

        List<DiskPartition> savedPartitions = new ArrayList<>();
        if (agentPayloadDTO.getDiskPartitions() != null) {
            for (var dto : agentPayloadDTO.getDiskPartitions()) {
                savedPartitions.add(diskPartitionRepository.save(DiskPartition.builder()
                        .systemMetric(systemMetric)
                        .device(dto.getDevice())
                        .mountPoint(dto.getMountPoint())
                        .fileSystem(dto.getFileSystem())
                        .totalGb(dto.getTotalGb())
                        .usedGb(dto.getUsedGb())
                        .percent(dto.getPercent())
                        .build()));
            }
        }

        alertEvaluationService.evaluate(machine, systemMetric);

        simpMessagingTemplate.convertAndSend("/topic/metrics",
                metricMapper.toDTO(systemMetric, machine, savedProcesses, savedPartitions));
    }

    public MetricResponseDTO getSystemMetrics(String machineId, String username) {
        Machine machine = machineRepository
                .findByMachineIdAndOwnerUsername(machineId, username)
                .orElseThrow(() -> new ResourceNotFound("Machine not found: " + machineId));

        SystemMetric systemMetric = systemMetricRepository
                .findTop1ByMachineOrderByRecordedAtDesc(machine)
                .orElseThrow(() -> new ResourceNotFound("No metrics found for machine: " + machineId));

        return metricMapper.toDTO(systemMetric, machine,
                topProcessRepository.findAllBySystemMetric(systemMetric),
                diskPartitionRepository.findAllBySystemMetric(systemMetric));
    }

    public List<MetricBucketProjection> getHistory(String machineId, String username, Integer minutes) {
        Machine machine = machineRepository
                .findByMachineIdAndOwnerUsername(machineId, username)
                .orElseThrow(() -> new ResourceNotFound("Machine not found: " + machineId));
        Instant cutoff = Instant.now().minusSeconds(minutes * 60L);
        return systemMetricRepository.findBucketedHistory(machine.getId(), cutoff, bucketSeconds(minutes));
    }

    private int bucketSeconds(int minutes) {
        if (minutes <= 60)   return 15;
        if (minutes <= 360)  return 60;
        if (minutes <= 1440) return 300;
        return 3600;
    }
}
