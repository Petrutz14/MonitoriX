package com.monitorpc.monitor_pc.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record AgentRegisterRequestDTO(
        @NotBlank @Size(min = 3, max = 50) String username,
        @NotBlank @Size(min = 8) String password,
        String humanUsername
) {}
