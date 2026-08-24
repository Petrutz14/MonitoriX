package com.monitorpc.monitor_pc.controller;

import com.monitorpc.monitor_pc.dto.MachineResponseDTO;
import com.monitorpc.monitor_pc.dto.MachineUpdateDTO;
import com.monitorpc.monitor_pc.service.MachineService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/machines")
@RequiredArgsConstructor
public class MachineController {
    private final MachineService machineService;

    @GetMapping
    public ResponseEntity<List<MachineResponseDTO>> getAllMachines(@AuthenticationPrincipal Jwt jwt) {
        return ResponseEntity.status(HttpStatus.OK).body(machineService.getAllMachines(jwt.getSubject()));
    }

    @GetMapping("/{machineId}")
    public ResponseEntity<MachineResponseDTO> getMachine(@PathVariable String machineId,
                                                         @AuthenticationPrincipal Jwt jwt) {
        return ResponseEntity.status(HttpStatus.OK).body(machineService.getMachine(machineId, jwt.getSubject()));
    }

    @PatchMapping("/{machineId}")
    public ResponseEntity<MachineResponseDTO> updateDisplayName(@PathVariable String machineId,
                                                                 @RequestBody MachineUpdateDTO machineUpdateDTO,
                                                                 @AuthenticationPrincipal Jwt jwt) {
        return ResponseEntity.status(HttpStatus.OK)
                .body(machineService.updateDisplayName(machineId, jwt.getSubject(), machineUpdateDTO));
    }
}
