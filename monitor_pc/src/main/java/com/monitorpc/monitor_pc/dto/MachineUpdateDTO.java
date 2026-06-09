package com.monitorpc.monitor_pc.dto;

import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

//DTO for Frontend->Machine Update
@Data
@NoArgsConstructor
public class MachineUpdateDTO {
    private String displayName;
}
