package com.monitorpc.monitor_pc.service;

import com.monitorpc.monitor_pc.enums.MachineStatus;
import com.monitorpc.monitor_pc.model.Machine;
import com.monitorpc.monitor_pc.repository.MachineRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;


@Slf4j
@Service
@RequiredArgsConstructor
public class MachineHealthService {
    private final MachineRepository machineRepository;

    //Check if inactive for 30 seconds+Logs status change
    @Scheduled(fixedRate = 30000)
    @Transactional
    public void offlineChecker(){
        List<Machine> machines=machineRepository.findAll();
        log.info("Checking {} machines", machines.size());
        Instant cutoff = Instant.now().minusSeconds(30);
        for(Machine machine : machines){
            if(machine.getLastSeen().isBefore(cutoff) && machine.getStatus()!=MachineStatus.OFFLINE){
                machine.setStatus(MachineStatus.OFFLINE);
                log.info("Machine {} set to offline",machine.getMachineId());
            }
        }
    }
}
