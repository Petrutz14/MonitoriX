package com.monitorpc.monitor_pc.repository;

import com.monitorpc.monitor_pc.model.AlertRule;
import com.monitorpc.monitor_pc.model.Machine;
import com.monitorpc.monitor_pc.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface AlertRuleRepository extends JpaRepository<AlertRule, Long> {

    List<AlertRule> findByMachineAndOwnerUsername(Machine machine, String username);

    List<AlertRule> findByMachineIsNullAndOwnerUsername(String username);

    List<AlertRule> findByOwnerUsername(String username);

    Optional<AlertRule> findByIdAndOwnerUsername(Long id, String username);

    @Query("SELECT r FROM AlertRule r WHERE r.enabled = true AND r.owner = :owner AND (r.machine IS NULL OR r.machine = :machine)")
    List<AlertRule> findApplicableRules(@Param("machine") Machine machine, @Param("owner") User owner);
}
