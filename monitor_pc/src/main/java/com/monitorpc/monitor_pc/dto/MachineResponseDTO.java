package com.monitorpc.monitor_pc.dto;

import com.monitorpc.monitor_pc.enums.MachineStatus;
import lombok.Builder;
import lombok.Data;

import java.time.Instant;

//DTO for Machine->Frontend
@Data
@Builder
public class MachineResponseDTO {
    private String machineId;
    private String displayName;
    private String osName;
    private String ipAddress;
    private Double totalRamGb;
    private MachineStatus machineStatus;
    private Instant lastSeen;

}
