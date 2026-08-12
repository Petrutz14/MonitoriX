package com.monitorpc.monitor_pc.repository;

import com.monitorpc.monitor_pc.model.Machine;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface MachineRepository extends JpaRepository<Machine, Long> {
    Optional<Machine> findByMachineId(String machineId);
    List<Machine> findByOwnerUsername(String username);
    Optional<Machine> findByMachineIdAndOwnerUsername(String machineId, String username);
}
