package com.monitorpc.monitor_pc.service;

import com.monitorpc.monitor_pc.dto.AgentRegisterRequestDTO;
import com.monitorpc.monitor_pc.dto.AuthResponseDTO;
import com.monitorpc.monitor_pc.dto.LoginRequestDTO;
import com.monitorpc.monitor_pc.dto.RegisterRequestDTO;
import com.monitorpc.monitor_pc.exception.ResourceNotFound;
import com.monitorpc.monitor_pc.model.User;
import com.monitorpc.monitor_pc.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtEncoder jwtEncoder;
    private final AuthenticationManager authenticationManager;

    @Value("${agent.registration-secret}")
    private String registrationSecret;

    @Transactional
    public AuthResponseDTO register(RegisterRequestDTO request) {
        if (userRepository.existsByUsername(request.username())) {
            throw new IllegalArgumentException("Username already taken");
        }

        User user = User.builder()
                .username(request.username())
                .email(request.email())
                .password(passwordEncoder.encode(request.password()))
                .role("ROLE_USER")
                .build();

        userRepository.save(user);
        return new AuthResponseDTO(issueToken(user.getUsername(), user.getRole()));
    }

    @Transactional
    public AuthResponseDTO registerAgent(AgentRegisterRequestDTO request, String secret) {
        if (!registrationSecret.equals(secret)) {
            throw new IllegalArgumentException("Invalid registration secret");
        }
        if (userRepository.existsByUsername(request.username())) {
            throw new IllegalArgumentException("Username already taken");
        }
        User user = User.builder()
                .username(request.username())
                .email(request.username() + "@monitorix.internal")
                .password(passwordEncoder.encode(request.password()))
                .role("ROLE_AGENT")
                .build();
        userRepository.save(user);
        return new AuthResponseDTO(issueToken(user.getUsername(), user.getRole()));
    }

    public AuthResponseDTO login(LoginRequestDTO request) {
        Authentication auth = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.username(), request.password())
        );

        User user = userRepository.findByUsername(auth.getName())
                .orElseThrow(() -> new ResourceNotFound("User not found"));

        return new AuthResponseDTO(issueToken(user.getUsername(), user.getRole()));
    }

    private String issueToken(String username, String role) {
        Instant now = Instant.now();
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer("monitorix")
                .issuedAt(now)
                .expiresAt(now.plus(1, ChronoUnit.HOURS))
                .subject(username)
                .claim("role", role)
                .build();

        return jwtEncoder.encode(JwtEncoderParameters.from(claims)).getTokenValue();
    }
}
