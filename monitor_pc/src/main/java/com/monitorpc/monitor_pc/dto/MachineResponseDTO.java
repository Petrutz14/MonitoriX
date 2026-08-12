package com.monitorpc.monitor_pc.dto;

import com.monitorpc.monitor_pc.enums.MachineStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

//DTO for Machine->Frontend
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MachineResponseDTO {
    private String machineId;
    private String displayName;
    private String osName;
    private String ipAddress;
    private Double totalRamGb;
    private MachineStatus machineStatus;
    private Instant lastSeen;

}
