package com.monitorpc.monitor_pc.service;

import com.monitorpc.monitor_pc.dto.MachineResponseDTO;
import com.monitorpc.monitor_pc.dto.MachineUpdateDTO;
import com.monitorpc.monitor_pc.enums.MachineStatus;
import com.monitorpc.monitor_pc.exception.ResourceNotFound;
import com.monitorpc.monitor_pc.mapper.MachineMapper;
import com.monitorpc.monitor_pc.model.Machine;
import com.monitorpc.monitor_pc.repository.MachineRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MachineServiceTest {

    @Mock MachineRepository machineRepository;
    @Mock MachineMapper machineMapper;

    @InjectMocks MachineService service;

    private static final String USER = "test-user";

    private Machine buildMachine(String machineId) {
        return Machine.builder()
                .machineId(machineId).displayName(machineId)
                .status(MachineStatus.ONLINE).lastSeen(Instant.now()).build();
    }

    @Test
    void getAllMachines_returnsMappedList() {
        Machine m1 = buildMachine("host-1");
        Machine m2 = buildMachine("host-2");
        MachineResponseDTO dto1 = mock(MachineResponseDTO.class);
        MachineResponseDTO dto2 = mock(MachineResponseDTO.class);

        when(machineRepository.findByOwnerUsername(USER)).thenReturn(List.of(m1, m2));
        when(machineMapper.toDTO(m1)).thenReturn(dto1);
        when(machineMapper.toDTO(m2)).thenReturn(dto2);

        List<MachineResponseDTO> result = service.getAllMachines(USER);

        assertThat(result).containsExactly(dto1, dto2);
    }

    @Test
    void getMachine_found_returnsDTO() {
        Machine machine = buildMachine("host-1");
        MachineResponseDTO dto = mock(MachineResponseDTO.class);

        when(machineRepository.findByMachineIdAndOwnerUsername("host-1", USER)).thenReturn(Optional.of(machine));
        when(machineMapper.toDTO(machine)).thenReturn(dto);

        assertThat(service.getMachine("host-1", USER)).isEqualTo(dto);
    }

    @Test
    void getMachine_notFound_throws() {
        when(machineRepository.findByMachineIdAndOwnerUsername("ghost", USER)).thenReturn(Optional.empty());
        assertThrows(ResourceNotFound.class, () -> service.getMachine("ghost", USER));
    }

    @Test
    void updateDisplayName_found_updatesAndReturns() {
        Machine machine = buildMachine("host-1");
        MachineResponseDTO dto = mock(MachineResponseDTO.class);
        MachineUpdateDTO update = new MachineUpdateDTO();
        update.setDisplayName("New Name");

        when(machineRepository.findByMachineIdAndOwnerUsername("host-1", USER)).thenReturn(Optional.of(machine));
        when(machineMapper.toDTO(machine)).thenReturn(dto);

        MachineResponseDTO result = service.updateDisplayName("host-1", USER, update);

        assertThat(machine.getDisplayName()).isEqualTo("New Name");
        assertThat(result).isEqualTo(dto);
    }

    @Test
    void updateDisplayName_notFound_throws() {
        when(machineRepository.findByMachineIdAndOwnerUsername("ghost", USER)).thenReturn(Optional.empty());
        assertThrows(ResourceNotFound.class, () ->
                service.updateDisplayName("ghost", USER, new MachineUpdateDTO()));
    }
}
