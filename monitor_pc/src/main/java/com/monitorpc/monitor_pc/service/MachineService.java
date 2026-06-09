package com.monitorpc.monitor_pc.service;

import com.monitorpc.monitor_pc.dto.MachineResponseDTO;
import com.monitorpc.monitor_pc.dto.MachineUpdateDTO;
import com.monitorpc.monitor_pc.exception.ResourceNotFound;
import com.monitorpc.monitor_pc.mapper.MachineMapper;
import com.monitorpc.monitor_pc.model.Machine;
import com.monitorpc.monitor_pc.repository.MachineRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class MachineService {
    private final MachineRepository machineRepository;
    private final MachineMapper machineMapper;

    public List<MachineResponseDTO> getAllMachines(){
        return machineRepository.findAll().stream().map(machineMapper::toDTO).toList();
    }

    public MachineResponseDTO getMachine(String machineId){
        Machine machine = machineRepository
                .findByMachineId(machineId)
                .orElseThrow(() -> new ResourceNotFound("Machine not found"));
        return machineMapper.toDTO(machine);
    }

    @Transactional
    public MachineResponseDTO updateDisplayName(String machineId, MachineUpdateDTO machineUpdateDTO){
        Machine machine = machineRepository
                .findByMachineId(machineId)
                .orElseThrow(() -> new ResourceNotFound("Machine not found"));
        machine.setDisplayName(machineUpdateDTO.getDisplayName());
        return machineMapper.toDTO(machine);
    }
}
