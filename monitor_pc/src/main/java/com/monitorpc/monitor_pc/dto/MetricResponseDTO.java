package com.monitorpc.monitor_pc.dto;

import com.monitorpc.monitor_pc.enums.MachineStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.List;

//DTO for Metrics->Frontend
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MetricResponseDTO {
    private String machineId;
    private String displayName;
    private Double cpuPercent;
    private Double ramPercent;
    private Double diskPercent;
    private String osName;
    private String ipAddress;
    private Double totalRamGb;
    private Double ramUsedGb;
    private Double diskFreeGb;
    private Long uptimeSeconds;
    private List<TopProcessDTO> topProcesses;
    private List<DiskPartitionDTO> diskPartitions;
    private Instant recordedAt;
    private MachineStatus machineStatus;
}
