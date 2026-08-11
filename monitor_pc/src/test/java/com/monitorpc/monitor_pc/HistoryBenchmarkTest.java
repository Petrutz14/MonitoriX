package com.monitorpc.monitor_pc;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.resttestclient.TestRestTemplate;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureTestRestTemplate;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.http.*;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.*;

@Tag("benchmark")
@Testcontainers
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureTestRestTemplate
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class HistoryBenchmarkTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16");

    @LocalServerPort
    int port;

    @Autowired
    TestRestTemplate rest;

    private static final String MACHINE_ID  = "bench-machine";
    private static final String AGENT_SECRET = "demo_secret";
    private static final int    INSERT_COUNT  = 500;
    private static final int    BENCH_RUNS    = 10;

    private String userToken;
    private String agentToken;

    // ── auth ────────────────────────────────────────────────────────────────

    @BeforeAll
    void setup() {
        rest.postForEntity(url("/api/auth/register"),
                body(Map.of("username", "benchuser", "email", "bench@test.com", "password", "Bench1234!")),
                String.class);

        userToken = login("benchuser", "Bench1234!");

        HttpHeaders agentHeaders = new HttpHeaders();
        agentHeaders.setContentType(MediaType.APPLICATION_JSON);
        agentHeaders.set("X-Agent-Secret", AGENT_SECRET);
        rest.exchange(url("/api/auth/register-agent"),
                HttpMethod.POST,
                new HttpEntity<>(Map.of("username", MACHINE_ID, "password", "agent-secret-change-me"), agentHeaders),
                String.class);

        agentToken = login(MACHINE_ID, "agent-secret-change-me");

        populate();
    }

    private String login(String username, String password) {
        ResponseEntity<Map> response = rest.postForEntity(
                url("/api/auth/login"),
                body(Map.of("username", username, "password", password)),
                Map.class);
        return (String) Objects.requireNonNull(response.getBody()).get("token");
    }

    private void populate() {
        System.out.printf("%nPopulating %d metrics...%n", INSERT_COUNT);
        long t0 = System.currentTimeMillis();
        HttpHeaders headers = bearerHeaders(agentToken);

        for (int i = 0; i < INSERT_COUNT; i++) {
            rest.exchange(url("/api/metrics"), HttpMethod.POST,
                    new HttpEntity<>(payload(i), headers), String.class);
            if ((i + 1) % 100 == 0)
                System.out.printf("  %d/%d — %.1fs%n", i + 1, INSERT_COUNT,
                        (System.currentTimeMillis() - t0) / 1000.0);
        }
        System.out.printf("Done in %.2fs%n", (System.currentTimeMillis() - t0) / 1000.0);
    }

    // ── benchmark ────────────────────────────────────────────────────────────

    @Test
    void benchmarkHistory() {
        for (int minutes : new int[]{30, 60, 1440}) {
            benchmarkEndpoint(minutes);
        }
    }

    private void benchmarkEndpoint(int minutes) {
        HttpHeaders headers = bearerHeaders(userToken);
        String endpoint = url("/api/metrics/" + MACHINE_ID + "/history?minutes=" + minutes);
        List<Long> times = new ArrayList<>();
        int rows = 0;

        System.out.printf("%nGET /history?minutes=%d — %d runs%n", minutes, BENCH_RUNS);
        for (int i = 0; i < BENCH_RUNS; i++) {
            long t0 = System.nanoTime();
            ResponseEntity<List> resp = rest.exchange(endpoint, HttpMethod.GET,
                    new HttpEntity<>(headers), List.class);
            long ms = (System.nanoTime() - t0) / 1_000_000;
            times.add(ms);
            if (resp.getBody() != null) rows = resp.getBody().size();
        }

        Collections.sort(times);
        long median = times.get(BENCH_RUNS / 2);
        long mean   = (long) times.stream().mapToLong(Long::longValue).average().orElse(0);
        System.out.printf("  Rows     : %d%n", rows);
        System.out.printf("  Min      : %dms%n", times.get(0));
        System.out.printf("  Max      : %dms%n", times.get(times.size() - 1));
        System.out.printf("  Median   : %dms%n", median);
        System.out.printf("  Mean     : %dms%n", mean);
    }

    // ── helpers ──────────────────────────────────────────────────────────────

    private String url(String path) {
        return "http://localhost:" + port + path;
    }

    private HttpEntity<Map<String, Object>> body(Map<String, Object> map) {
        HttpHeaders h = new HttpHeaders();
        h.setContentType(MediaType.APPLICATION_JSON);
        return new HttpEntity<>(map, h);
    }

    private HttpHeaders bearerHeaders(String token) {
        HttpHeaders h = new HttpHeaders();
        h.setContentType(MediaType.APPLICATION_JSON);
        h.setBearerAuth(token);
        return h;
    }

    private Map<String, Object> payload(int i) {
        Random rng = new Random(i);
        List<Map<String, Object>> processes = new ArrayList<>();
        for (int p = 0; p < 5; p++) {
            processes.add(Map.of(
                    "pid", 1000 + p, "name", "proc" + p,
                    "cpuPercent", rng.nextDouble() * 20,
                    "ramPercent", rng.nextDouble() * 10,
                    "ramUsedMb",  rng.nextDouble() * 500));
        }
        List<Map<String, Object>> partitions = List.of(
                Map.of("device", "/dev/sda1", "mountPoint", "/", "fileSystem", "ext4",
                        "totalGb", 500.0, "usedGb", 200.0, "percent", 40.0),
                Map.of("device", "/dev/sdb1", "mountPoint", "/data", "fileSystem", "ext4",
                        "totalGb", 1000.0, "usedGb", 300.0, "percent", 30.0));

        Map<String, Object> map = new LinkedHashMap<>();
        map.put("machineId",      MACHINE_ID);
        map.put("displayName",    "Benchmark Machine");
        map.put("osName",         "Linux 5.15");
        map.put("ipAddress",      "192.168.1.99");
        map.put("totalRamGb",     16.0);
        map.put("cpuPercent",     rng.nextDouble() * 90);
        map.put("ramPercent",     rng.nextDouble() * 80);
        map.put("diskPercent",    rng.nextDouble() * 70);
        map.put("ramUsedGb",      rng.nextDouble() * 12);
        map.put("diskFreeGb",     rng.nextDouble() * 200);
        map.put("uptimeSeconds",  3600L + i * 15L);
        map.put("topProcesses",   processes);
        map.put("diskPartitions", partitions);
        return map;
    }
}
