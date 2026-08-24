package com.monitorpc.monitor_pc.service;

import com.monitorpc.monitor_pc.enums.MachineStatus;
import com.monitorpc.monitor_pc.model.Machine;
import com.monitorpc.monitor_pc.repository.MachineRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MachineHealthServiceTest {

    @Mock MachineRepository machineRepository;

    @InjectMocks MachineHealthService service;

    @Test
    void offlineChecker_staleMachine_setsOffline() {
        Machine machine = Machine.builder()
                .machineId("stale-host")
                .status(MachineStatus.ONLINE)
                .lastSeen(Instant.now().minusSeconds(120))
                .build();
        when(machineRepository.findAll()).thenReturn(List.of(machine));

        service.offlineChecker();

        assertThat(machine.getStatus()).isEqualTo(MachineStatus.OFFLINE);
    }

    @Test
    void offlineChecker_recentMachine_remainsOnline() {
        Machine machine = Machine.builder()
                .machineId("active-host")
                .status(MachineStatus.ONLINE)
                .lastSeen(Instant.now())
                .build();
        when(machineRepository.findAll()).thenReturn(List.of(machine));

        service.offlineChecker();

        assertThat(machine.getStatus()).isEqualTo(MachineStatus.ONLINE);
    }

    @Test
    void offlineChecker_alreadyOffline_noStatusChange() {
        Machine machine = Machine.builder()
                .machineId("already-offline")
                .status(MachineStatus.OFFLINE)
                .lastSeen(Instant.now().minusSeconds(60))
                .build();
        when(machineRepository.findAll()).thenReturn(List.of(machine));

        service.offlineChecker();

        assertThat(machine.getStatus()).isEqualTo(MachineStatus.OFFLINE);
    }

    @Test
    void offlineChecker_nullLastSeen_noNpe() {
        Machine machine = Machine.builder()
                .machineId("no-seen")
                .status(MachineStatus.ONLINE)
                .lastSeen(null)
                .build();
        when(machineRepository.findAll()).thenReturn(List.of(machine));

        assertThatNoException().isThrownBy(() -> service.offlineChecker());
        assertThat(machine.getStatus()).isEqualTo(MachineStatus.ONLINE);
    }

    @Test
    void offlineChecker_emptyList_doesNothing() {
        when(machineRepository.findAll()).thenReturn(List.of());
        assertThatNoException().isThrownBy(() -> service.offlineChecker());
    }
}
