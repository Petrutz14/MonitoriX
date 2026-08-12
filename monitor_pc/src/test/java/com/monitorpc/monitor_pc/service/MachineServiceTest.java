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

        when(machineRepository.findAll()).thenReturn(List.of(m1, m2));
        when(machineMapper.toDTO(m1)).thenReturn(dto1);
        when(machineMapper.toDTO(m2)).thenReturn(dto2);

        List<MachineResponseDTO> result = service.getAllMachines();

        assertThat(result).containsExactly(dto1, dto2);
    }

    @Test
    void getMachine_found_returnsDTO() {
        Machine machine = buildMachine("host-1");
        MachineResponseDTO dto = mock(MachineResponseDTO.class);

        when(machineRepository.findByMachineId("host-1")).thenReturn(Optional.of(machine));
        when(machineMapper.toDTO(machine)).thenReturn(dto);

        assertThat(service.getMachine("host-1")).isEqualTo(dto);
    }

    @Test
    void getMachine_notFound_throws() {
        when(machineRepository.findByMachineId("ghost")).thenReturn(Optional.empty());
        assertThrows(ResourceNotFound.class, () -> service.getMachine("ghost"));
    }

    @Test
    void updateDisplayName_found_updatesAndReturns() {
        Machine machine = buildMachine("host-1");
        MachineResponseDTO dto = mock(MachineResponseDTO.class);
        MachineUpdateDTO update = new MachineUpdateDTO();
        update.setDisplayName("New Name");

        when(machineRepository.findByMachineId("host-1")).thenReturn(Optional.of(machine));
        when(machineMapper.toDTO(machine)).thenReturn(dto);

        MachineResponseDTO result = service.updateDisplayName("host-1", update);

        assertThat(machine.getDisplayName()).isEqualTo("New Name");
        assertThat(result).isEqualTo(dto);
    }

    @Test
    void updateDisplayName_notFound_throws() {
        when(machineRepository.findByMachineId("ghost")).thenReturn(Optional.empty());
        assertThrows(ResourceNotFound.class, () ->
                service.updateDisplayName("ghost", new MachineUpdateDTO()));
    }
}
