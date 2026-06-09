package com.monitorpc.monitor_pc.repository;

import com.monitorpc.monitor_pc.model.AlertRule;
import com.monitorpc.monitor_pc.model.Machine;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface AlertRuleRepository extends JpaRepository<AlertRule, Long> {

    List<AlertRule> findByMachine(Machine machine);

    List<AlertRule> findByMachineIsNull();

    // Returns global rules + rules specific to this machine
    @Query("SELECT r FROM AlertRule r WHERE r.enabled = true AND (r.machine IS NULL OR r.machine = :machine)")
    List<AlertRule> findApplicableRules(@Param("machine") Machine machine);
}
