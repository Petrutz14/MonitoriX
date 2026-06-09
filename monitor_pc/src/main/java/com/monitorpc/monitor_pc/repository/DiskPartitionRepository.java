package com.monitorpc.monitor_pc.repository;

import com.monitorpc.monitor_pc.model.DiskPartition;
import com.monitorpc.monitor_pc.model.SystemMetric;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface DiskPartitionRepository extends JpaRepository<DiskPartition,Long> {
    List<DiskPartition> findAllBySystemMetric(SystemMetric systemMetric);
}
