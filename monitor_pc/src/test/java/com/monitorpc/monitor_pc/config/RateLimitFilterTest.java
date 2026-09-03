package com.monitorpc.monitor_pc.config;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class RateLimitFilterTest {

    private MockMvc mockMvc;

    @RestController
    static class StubController {
        @PostMapping("/api/auth/login")
        ResponseEntity<String> login(@RequestBody String body) { return ResponseEntity.ok("ok"); }

        @PostMapping("/api/auth/register")
        ResponseEntity<String> register(@RequestBody String body) { return ResponseEntity.status(201).body("created"); }

        @PostMapping("/api/metrics")
        ResponseEntity<String> metrics(@RequestBody String body) { return ResponseEntity.ok("ok"); }

        @PostMapping("/api/other")
        ResponseEntity<String> other() { return ResponseEntity.ok("ok"); }

        @GetMapping("/api/auth/login")
        ResponseEntity<String> loginGet() { return ResponseEntity.ok("ok"); }
    }

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
                .standaloneSetup(new StubController())
                .addFilters(new RateLimitFilter())
                .build();
    }

    // --- login ---

    @Test
    void login_withinLimit_passes() throws Exception {
        for (int i = 0; i < 10; i++) {
            mockMvc.perform(post("/api/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("\"body\""))
                    .andExpect(status().isOk());
        }
    }

    @Test
    void login_exceedsLimit_returns429() throws Exception {
        exhaust("/api/auth/login", 10);
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("\"body\""))
                .andExpect(status().isTooManyRequests())
                .andExpect(header().exists("X-Rate-Limit-Retry-After-Seconds"));
    }

    @Test
    void login_firstRequest_remainingIs9() throws Exception {
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("\"body\""))
                .andExpect(status().isOk())
                .andExpect(header().string("X-Rate-Limit-Remaining", "9"));
    }

    // --- register ---

    @Test
    void register_withinLimit_passes() throws Exception {
        for (int i = 0; i < 5; i++) {
            mockMvc.perform(post("/api/auth/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("\"body\""))
                    .andExpect(status().isCreated());
        }
    }

    @Test
    void register_exceedsLimit_returns429() throws Exception {
        exhaust("/api/auth/register", 5);
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("\"body\""))
                .andExpect(status().isTooManyRequests())
                .andExpect(header().exists("X-Rate-Limit-Retry-After-Seconds"));
    }

    @Test
    void register_firstRequest_remainingIs4() throws Exception {
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("\"body\""))
                .andExpect(status().isCreated())
                .andExpect(header().string("X-Rate-Limit-Remaining", "4"));
    }

    // --- metrics ---

    @Test
    void metrics_withinLimit_passes() throws Exception {
        for (int i = 0; i < 30; i++) {
            mockMvc.perform(post("/api/metrics")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("\"body\""))
                    .andExpect(status().isOk());
        }
    }

    @Test
    void metrics_exceedsLimit_returns429() throws Exception {
        exhaust("/api/metrics", 30);
        mockMvc.perform(post("/api/metrics")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("\"body\""))
                .andExpect(status().isTooManyRequests());
    }

    // --- filter bypass cases ---

    @Test
    void nonRateLimitedPath_notBlocked() throws Exception {
        mockMvc.perform(post("/api/other")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());
    }

    @Test
    void getRequest_onRateLimitedPath_notBlocked() throws Exception {
        mockMvc.perform(get("/api/auth/login"))
                .andExpect(status().isOk());
    }

    @Test
    void differentIps_haveSeparateBuckets() throws Exception {
        exhaust("/api/auth/login", 10);

        // different IP gets fresh bucket
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("\"body\"")
                        .header("X-Forwarded-For", "10.0.0.99"))
                .andExpect(status().isOk())
                .andExpect(header().string("X-Rate-Limit-Remaining", "9"));
    }

    private void exhaust(String path, int times) throws Exception {
        for (int i = 0; i < times; i++) {
            mockMvc.perform(post(path)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("\"body\""));
        }
    }
}
