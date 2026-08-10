package com.monitorpc.monitor_pc.controller;

import com.monitorpc.monitor_pc.dto.AgentRegisterRequestDTO;
import com.monitorpc.monitor_pc.dto.AuthResponseDTO;
import com.monitorpc.monitor_pc.dto.LoginRequestDTO;
import com.monitorpc.monitor_pc.dto.RegisterRequestDTO;
import com.monitorpc.monitor_pc.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import static org.springframework.http.HttpStatus.CREATED;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/register")
    public ResponseEntity<AuthResponseDTO> register(@Valid @RequestBody RegisterRequestDTO request) {
        return ResponseEntity.ok(authService.register(request));
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponseDTO> login(@Valid @RequestBody LoginRequestDTO request) {
        return ResponseEntity.ok(authService.login(request));
    }

    @PostMapping("/register-agent")
    public ResponseEntity<AuthResponseDTO> registerAgent(
            @Valid @RequestBody AgentRegisterRequestDTO request,
            @RequestHeader("X-Agent-Secret") String secret) {
        return ResponseEntity.status(CREATED).body(authService.registerAgent(request, secret));
    }
}
