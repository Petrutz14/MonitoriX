package com.monitorpc.monitor_pc.repository;

import com.monitorpc.monitor_pc.model.Machine;
import com.monitorpc.monitor_pc.model.SystemMetric;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface SystemMetricRepository extends JpaRepository<SystemMetric,Long> {
    Optional<SystemMetric> findTop1ByMachineOrderByRecordedAtDesc(Machine machine);

    List<SystemMetric> findAllByMachineAndRecordedAtAfterOrderByRecordedAtDesc(Machine machine, Instant cutoff);
}
