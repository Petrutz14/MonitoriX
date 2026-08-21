package com.monitorpc.monitor_pc.controller;

import com.monitorpc.monitor_pc.dto.AlertResponseDTO;
import com.monitorpc.monitor_pc.dto.AlertRuleRequestDTO;
import com.monitorpc.monitor_pc.dto.AlertRuleResponseDTO;
import com.monitorpc.monitor_pc.service.AlertEvaluationService;
import com.monitorpc.monitor_pc.service.AlertRuleService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api")
@CrossOrigin(origins = "http://localhost:4200")
@RequiredArgsConstructor
public class AlertController {

    private final AlertRuleService alertRuleService;
    private final AlertEvaluationService alertEvaluationService;

    @PostMapping("/alert-rules")
    public ResponseEntity<AlertRuleResponseDTO> createRule(@RequestBody AlertRuleRequestDTO request,
                                                           @AuthenticationPrincipal Jwt jwt) {
        return ResponseEntity.status(HttpStatus.CREATED).body(alertRuleService.createRule(request, jwt.getSubject()));
    }

    @GetMapping("/alert-rules")
    public ResponseEntity<List<AlertRuleResponseDTO>> getAllRules(@AuthenticationPrincipal Jwt jwt) {
        return ResponseEntity.ok(alertRuleService.getAllRules(jwt.getSubject()));
    }

    @GetMapping("/alert-rules/global")
    public ResponseEntity<List<AlertRuleResponseDTO>> getGlobalRules(@AuthenticationPrincipal Jwt jwt) {
        return ResponseEntity.ok(alertRuleService.getGlobalRules(jwt.getSubject()));
    }

    @GetMapping("/alert-rules/{machineId}")
    public ResponseEntity<List<AlertRuleResponseDTO>> getRulesForMachine(@PathVariable String machineId,
                                                                          @AuthenticationPrincipal Jwt jwt) {
        return ResponseEntity.ok(alertRuleService.getRulesForMachine(machineId, jwt.getSubject()));
    }

    @PatchMapping("/alert-rules/{id}/toggle")
    public ResponseEntity<AlertRuleResponseDTO> toggleRule(@PathVariable Long id,
                                                           @RequestParam Boolean enabled,
                                                           @AuthenticationPrincipal Jwt jwt) {
        return ResponseEntity.ok(alertRuleService.toggleRule(id, enabled, jwt.getSubject()));
    }

    @DeleteMapping("/alert-rules/{id}")
    public ResponseEntity<Void> deleteRule(@PathVariable Long id, @AuthenticationPrincipal Jwt jwt) {
        alertRuleService.deleteRule(id, jwt.getSubject());
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/alerts/{machineId}")
    public ResponseEntity<List<AlertResponseDTO>> getAlerts(@PathVariable String machineId,
                                                            @AuthenticationPrincipal Jwt jwt) {
        return ResponseEntity.ok(alertEvaluationService.getAlertsForMachine(machineId, jwt.getSubject()));
    }

    @GetMapping("/alerts/{machineId}/active")
    public ResponseEntity<List<AlertResponseDTO>> getActiveAlerts(@PathVariable String machineId,
                                                                   @AuthenticationPrincipal Jwt jwt) {
        return ResponseEntity.ok(alertEvaluationService.getActiveAlertsForMachine(machineId, jwt.getSubject()));
    }
}
