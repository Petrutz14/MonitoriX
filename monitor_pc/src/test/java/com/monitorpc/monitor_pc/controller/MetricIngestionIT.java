package com.monitorpc.monitor_pc.controller;

import com.monitorpc.monitor_pc.enums.MachineStatus;
import com.monitorpc.monitor_pc.repository.MachineRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.assertj.MockMvcTester;
import org.testcontainers.containers.PostgreSQLContainer;
import org.springframework.test.context.ActiveProfiles;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;

@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
@ActiveProfiles("test")
class MetricIngestionIT {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16");

    @Autowired
    MockMvcTester mvc;

    @Autowired
    MachineRepository machineRepository;

    private static final String AGENT_USERNAME = "it-agent";
    private static final String AGENT_SECRET   = "demo_secret";
    private static final String MACHINE_ID     = "it-test-host";

    @AfterEach
    void cleanup() {
        machineRepository.findByMachineId(MACHINE_ID).ifPresent(machineRepository::delete);
    }

    @Test
    void postMetrics_createsMachineAndSetsOnline() {
        // Register the agent user so ingest() can look up the owner
        assertThat(
            mvc.post().uri("/api/auth/register-agent")
                .header("X-Agent-Secret", AGENT_SECRET)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"username":"%s","password":"it-pass-123"}
                    """.formatted(AGENT_USERNAME))
        ).hasStatus(HttpStatus.CREATED);

        // POST metrics using a mock JWT with ROLE_AGENT (matching SecurityConfig authority mapping)
        assertThat(
            mvc.post().uri("/api/metrics")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "machineId":      "%s",
                      "cpuPercent":     42.5,
                      "ramPercent":     60.0,
                      "diskPercent":    30.0,
                      "osName":         "Linux",
                      "ipAddress":      "10.0.0.1",
                      "totalRamGb":     16.0,
                      "ramUsedGb":      9.6,
                      "diskFreeGb":     200.0,
                      "uptimeSeconds":  3600,
                      "topProcesses":   [],
                      "diskPartitions": []
                    }
                    """.formatted(MACHINE_ID))
                .with(jwt().jwt(j -> j.subject(AGENT_USERNAME).claim("role", "ROLE_AGENT")))
        ).hasStatus(HttpStatus.CREATED);

        // Assert machine was upserted and marked ONLINE
        var machine = machineRepository.findByMachineId(MACHINE_ID);
        assertThat(machine).isPresent();
        assertThat(machine.get().getStatus()).isEqualTo(MachineStatus.ONLINE);
    }

    @Test
    void postMetrics_withoutAuth_returns401() {
        assertThat(
            mvc.post().uri("/api/metrics")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"machineId":"no-auth-host","cpuPercent":10.0,"ramPercent":10.0,
                     "diskPercent":10.0,"osName":"Linux","ipAddress":"1.1.1.1",
                     "totalRamGb":8.0,"ramUsedGb":1.0,"diskFreeGb":100.0,
                     "uptimeSeconds":60,"topProcesses":[],"diskPartitions":[]}
                    """)
        ).hasStatus(HttpStatus.UNAUTHORIZED);
    }
}
