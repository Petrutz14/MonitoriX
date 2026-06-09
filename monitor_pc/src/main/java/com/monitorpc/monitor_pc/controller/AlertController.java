package com.monitorpc.monitor_pc.controller;

import com.monitorpc.monitor_pc.dto.AlertResponseDTO;
import com.monitorpc.monitor_pc.dto.AlertRuleRequestDTO;
import com.monitorpc.monitor_pc.dto.AlertRuleResponseDTO;
import com.monitorpc.monitor_pc.exception.ResourceNotFound;
import com.monitorpc.monitor_pc.model.Machine;
import com.monitorpc.monitor_pc.repository.MachineRepository;
import com.monitorpc.monitor_pc.service.AlertEvaluationService;
import com.monitorpc.monitor_pc.service.AlertRuleService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api")
@CrossOrigin(origins = "http://localhost:4200")
@RequiredArgsConstructor
public class AlertController {

    private final AlertRuleService alertRuleService;
    private final AlertEvaluationService alertEvaluationService;
    private final MachineRepository machineRepository;

    @PostMapping("/alert-rules")
    public ResponseEntity<AlertRuleResponseDTO> createRule(@RequestBody AlertRuleRequestDTO request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(alertRuleService.createRule(request));
    }

    @GetMapping("/alert-rules")
    public ResponseEntity<List<AlertRuleResponseDTO>> getAllRules() {
        return ResponseEntity.ok(alertRuleService.getAllRules());
    }

    @GetMapping("/alert-rules/global")
    public ResponseEntity<List<AlertRuleResponseDTO>> getGlobalRules() {
        return ResponseEntity.ok(alertRuleService.getGlobalRules());
    }

    @GetMapping("/alert-rules/{machineId}")
    public ResponseEntity<List<AlertRuleResponseDTO>> getRulesForMachine(@PathVariable String machineId) {
        return ResponseEntity.ok(alertRuleService.getRulesForMachine(machineId));
    }

    @PatchMapping("/alert-rules/{id}/toggle")
    public ResponseEntity<AlertRuleResponseDTO> toggleRule(@PathVariable Long id, @RequestParam Boolean enabled) {
        return ResponseEntity.ok(alertRuleService.toggleRule(id, enabled));
    }

    @DeleteMapping("/alert-rules/{id}")
    public ResponseEntity<Void> deleteRule(@PathVariable Long id) {
        alertRuleService.deleteRule(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/alerts/{machineId}")
    public ResponseEntity<List<AlertResponseDTO>> getAlerts(@PathVariable String machineId) {
        Machine machine = machineRepository.findByMachineId(machineId)
                .orElseThrow(() -> new ResourceNotFound("Machine not found: " + machineId));
        return ResponseEntity.ok(alertEvaluationService.getAlertsForMachine(machineId, machine));
    }

    @GetMapping("/alerts/{machineId}/active")
    public ResponseEntity<List<AlertResponseDTO>> getActiveAlerts(@PathVariable String machineId) {
        Machine machine = machineRepository.findByMachineId(machineId)
                .orElseThrow(() -> new ResourceNotFound("Machine not found: " + machineId));
        return ResponseEntity.ok(alertEvaluationService.getActiveAlertsForMachine(machine));
    }
}
