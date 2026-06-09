package com.monitorpc.monitor_pc.repository;

import com.monitorpc.monitor_pc.enums.AlertStatus;
import com.monitorpc.monitor_pc.model.Alert;
import com.monitorpc.monitor_pc.model.AlertRule;
import com.monitorpc.monitor_pc.model.Machine;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface AlertRepository extends JpaRepository<Alert, Long> {

    Optional<Alert> findByAlertRuleAndMachineAndStatus(AlertRule alertRule, Machine machine, AlertStatus status);

    List<Alert> findByMachineOrderByTriggeredAtDesc(Machine machine);

    List<Alert> findByMachineAndStatusOrderByTriggeredAtDesc(Machine machine, AlertStatus status);
}
