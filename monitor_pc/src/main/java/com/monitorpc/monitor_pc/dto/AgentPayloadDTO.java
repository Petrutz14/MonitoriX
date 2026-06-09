package com.monitorpc.monitor_pc.dto;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

//DTO for Agent->Backend
@NoArgsConstructor
@Data
public class AgentPayloadDTO {
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
}
