package com.monitorpc.monitor_pc.repository;

import com.monitorpc.monitor_pc.model.SystemMetric;
import com.monitorpc.monitor_pc.model.TopProcess;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TopProcessRepository extends JpaRepository<TopProcess,Long> {
    List<TopProcess> findAllBySystemMetric(SystemMetric systemMetric);
}
