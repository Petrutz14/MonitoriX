package com.monitorpc.monitor_pc.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TopProcessDTO {
    private Integer pid;
    private String name;
    private Double cpuPercent;
    private Double ramPercent;
    private Double ramUsedMb;
}
