package com.monitorpc.monitor_pc.service;

import com.monitorpc.monitor_pc.dto.*;
import com.monitorpc.monitor_pc.exception.ResourceNotFound;
import com.monitorpc.monitor_pc.model.User;
import com.monitorpc.monitor_pc.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class AuthServiceTest {

    @Mock UserRepository userRepository;
    @Mock PasswordEncoder passwordEncoder;
    @Mock JwtEncoder jwtEncoder;
    @Mock AuthenticationManager authenticationManager;

    @InjectMocks AuthService service;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(service, "registrationSecret", "test_secret");
        Jwt jwt = mock(Jwt.class);
        when(jwt.getTokenValue()).thenReturn("mocked-token");
        when(jwtEncoder.encode(any())).thenReturn(jwt);
    }

    @Test
    void register_newUser_returnsToken() {
        when(userRepository.existsByUsername("newuser")).thenReturn(false);
        when(passwordEncoder.encode("password123")).thenReturn("hashed");
        when(userRepository.save(any(User.class))).thenAnswer(i -> i.getArgument(0));

        AuthResponseDTO result = service.register(
                new RegisterRequestDTO("newuser", "new@test.com", "password123"));

        assertThat(result.token()).isEqualTo("mocked-token");
        verify(userRepository).save(argThat(u -> u.getRole().equals("ROLE_USER")));
    }

    @Test
    void register_duplicateUsername_throwsIllegalArgument() {
        when(userRepository.existsByUsername("existing")).thenReturn(true);

        assertThrows(IllegalArgumentException.class, () ->
                service.register(new RegisterRequestDTO("existing", "e@test.com", "password123")));

        verify(userRepository, never()).save(any());
    }

    @Test
    void registerAgent_validSecret_returnsToken() {
        when(userRepository.existsByUsername("agent-host")).thenReturn(false);
        when(passwordEncoder.encode("agentpass1")).thenReturn("hashed");
        when(userRepository.save(any(User.class))).thenAnswer(i -> i.getArgument(0));

        AuthResponseDTO result = service.registerAgent(
                new AgentRegisterRequestDTO("agent-host", "agentpass1"), "test_secret");

        assertThat(result.token()).isEqualTo("mocked-token");
        verify(userRepository).save(argThat(u -> u.getRole().equals("ROLE_AGENT")));
    }

    @Test
    void registerAgent_wrongSecret_throwsIllegalArgument() {
        assertThrows(IllegalArgumentException.class, () ->
                service.registerAgent(
                        new AgentRegisterRequestDTO("agent-host", "agentpass1"), "wrong_secret"));

        verify(userRepository, never()).save(any());
    }

    @Test
    void login_validCredentials_returnsToken() {
        Authentication auth = mock(Authentication.class);
        when(auth.getName()).thenReturn("testuser");
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenReturn(auth);

        User user = User.builder().username("testuser").role("ROLE_USER").build();
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(user));

        AuthResponseDTO result = service.login(new LoginRequestDTO("testuser", "password123"));

        assertThat(result.token()).isEqualTo("mocked-token");
    }

    @Test
    void login_userNotFound_throwsResourceNotFound() {
        Authentication auth = mock(Authentication.class);
        when(auth.getName()).thenReturn("ghost");
        when(authenticationManager.authenticate(any())).thenReturn(auth);
        when(userRepository.findByUsername("ghost")).thenReturn(Optional.empty());

        assertThrows(ResourceNotFound.class, () ->
                service.login(new LoginRequestDTO("ghost", "password123")));
    }
}
