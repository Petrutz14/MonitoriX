package com.monitorpc.monitor_pc.controller;

import com.monitorpc.monitor_pc.dto.MachineResponseDTO;
import com.monitorpc.monitor_pc.dto.MachineUpdateDTO;
import com.monitorpc.monitor_pc.model.Machine;
import com.monitorpc.monitor_pc.service.MachineService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/machines")
@CrossOrigin(origins = "http://localhost:4200")
@RequiredArgsConstructor
public class MachineController {
    private final MachineService machineService;

    @GetMapping
    public ResponseEntity<List<MachineResponseDTO>> getAllMachines(){
        List<MachineResponseDTO> machines = machineService.getAllMachines();
        return ResponseEntity.status(HttpStatus.OK).body(machines);
    }

    @GetMapping("/{machineId}")
    public ResponseEntity<MachineResponseDTO> getMachine(@PathVariable String machineId){
        MachineResponseDTO machineResponseDTO = machineService.getMachine(machineId);
        return ResponseEntity.status(HttpStatus.OK).body(machineResponseDTO);
    }

    @PatchMapping("/{machineId}")
    public ResponseEntity<MachineResponseDTO> updateDisplayName(@PathVariable String machineId,@RequestBody MachineUpdateDTO machineUpdateDTO){
        MachineResponseDTO updated = machineService.updateDisplayName(machineId,machineUpdateDTO);
        return ResponseEntity.status(HttpStatus.OK).body(updated);
    }
}
