package com.monitorpc.monitor_pc.controller;

import com.monitorpc.monitor_pc.dto.AgentPayloadDTO;
import com.monitorpc.monitor_pc.dto.MetricBucketProjection;
import com.monitorpc.monitor_pc.dto.MetricResponseDTO;
import com.monitorpc.monitor_pc.service.MetricIngestionService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/metrics")
@RequiredArgsConstructor
public class MetricController {
    private final MetricIngestionService metricIngestionService;

    @PostMapping
    public ResponseEntity<String> createMetric(@RequestBody AgentPayloadDTO agentPayloadDTO,
                                               @AuthenticationPrincipal Jwt jwt) {
        metricIngestionService.ingest(agentPayloadDTO, jwt.getSubject());
        return ResponseEntity.status(HttpStatus.CREATED).body("Metric received");
    }

    @GetMapping("/{machineId}")
    public ResponseEntity<MetricResponseDTO> getMetric(@PathVariable String machineId,
                                                       @AuthenticationPrincipal Jwt jwt) {
        return ResponseEntity.status(HttpStatus.OK)
                .body(metricIngestionService.getSystemMetrics(machineId, jwt.getSubject()));
    }

    @GetMapping("/{machineId}/history")
    public ResponseEntity<List<MetricBucketProjection>> getMetricHistory(
            @PathVariable String machineId,
            @RequestParam(defaultValue = "30") Integer minutes,
            @AuthenticationPrincipal Jwt jwt) {
        return ResponseEntity.ok(metricIngestionService.getHistory(machineId, jwt.getSubject(), minutes));
    }

    @PostMapping("/simulate/{machineId}")
    public ResponseEntity<String> simulateMetric(@PathVariable String machineId,
                                                 @AuthenticationPrincipal Jwt jwt) {
        metricIngestionService.simulateIngest(machineId, jwt.getSubject());
        return ResponseEntity.status(HttpStatus.CREATED).body("Simulated metric received");
    }
}
