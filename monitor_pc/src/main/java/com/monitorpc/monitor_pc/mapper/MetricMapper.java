package com.monitorpc.monitor_pc.mapper;

import com.monitorpc.monitor_pc.dto.DiskPartitionDTO;
import com.monitorpc.monitor_pc.dto.MetricResponseDTO;
import com.monitorpc.monitor_pc.dto.TopProcessDTO;
import com.monitorpc.monitor_pc.model.DiskPartition;
import com.monitorpc.monitor_pc.model.Machine;
import com.monitorpc.monitor_pc.model.SystemMetric;
import com.monitorpc.monitor_pc.model.TopProcess;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

@Mapper(componentModel = "spring")
public interface MetricMapper {

    @Mapping(source = "machine.machineId", target = "machineId")
    @Mapping(source = "machine.displayName", target = "displayName")
    @Mapping(source = "machine.osName", target = "osName")
    @Mapping(source = "machine.ipAddress", target = "ipAddress")
    @Mapping(source = "machine.totalRamGb", target = "totalRamGb")
    @Mapping(source = "machine.status", target = "machineStatus")
    @Mapping(source = "metric.cpuPercent", target = "cpuPercent")
    @Mapping(source = "metric.ramPercent", target = "ramPercent")
    @Mapping(source = "metric.ramUsedGb", target = "ramUsedGb")
    @Mapping(source = "metric.diskPercent", target = "diskPercent")
    @Mapping(source = "metric.diskFreeGb", target = "diskFreeGb")
    @Mapping(source = "metric.uptimeSeconds", target = "uptimeSeconds")
    @Mapping(source = "metric.recordedAt", target = "recordedAt")
    @Mapping(source = "processes", target = "topProcesses")
    @Mapping(source = "partitions", target = "diskPartitions")
    MetricResponseDTO toDTO(SystemMetric metric, Machine machine, List<TopProcess> processes, List<DiskPartition> partitions);

    TopProcessDTO toDTO(TopProcess topProcess);

    DiskPartitionDTO toDTO(DiskPartition diskPartition);
}
