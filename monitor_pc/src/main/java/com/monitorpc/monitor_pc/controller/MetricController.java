package com.monitorpc.monitor_pc.controller;

import com.monitorpc.monitor_pc.dto.AgentPayloadDTO;
import com.monitorpc.monitor_pc.dto.MetricBucketProjection;
import com.monitorpc.monitor_pc.dto.MetricResponseDTO;
import com.monitorpc.monitor_pc.service.MetricIngestionService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/metrics")
@RequiredArgsConstructor
@CrossOrigin(origins = "http://localhost:4200")
public class MetricController {
    private final MetricIngestionService metricIngestionService;

    //Endpoint to enter metrics in system
    @PostMapping
    public ResponseEntity<String> createMetric(@RequestBody AgentPayloadDTO agentPayloadDTO){
        metricIngestionService.ingest(agentPayloadDTO);
        return ResponseEntity.status(HttpStatus.CREATED).body("Metric received");

    }

    //Get latest metric for specific machine
    @GetMapping("/{machineId}")
    public ResponseEntity<MetricResponseDTO> getMetric(@PathVariable String machineId){
        MetricResponseDTO metricResponseDTO = metricIngestionService.getSystemMetrics(machineId);
        return ResponseEntity.status(HttpStatus.OK).body(metricResponseDTO);

    }

    @GetMapping("/{machineId}/history")
    public ResponseEntity<List<MetricBucketProjection>> getMetricHistory(
            @PathVariable String machineId,
            @RequestParam(defaultValue = "30") Integer minutes) {
        return ResponseEntity.ok(metricIngestionService.getHistory(machineId, minutes));
    }

}
