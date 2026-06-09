package com.monitorpc.monitor_pc.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DiskPartitionDTO {
    private String device;
    private String mountPoint;
    private String fileSystem;
    private Double totalGb;
    private Double usedGb;
    private Double percent;
}
