package com.monitorpc.monitor_pc.mapper;

import com.monitorpc.monitor_pc.dto.MachineResponseDTO;
import com.monitorpc.monitor_pc.model.Machine;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface MachineMapper {

    @Mapping(source = "status", target = "machineStatus")
    MachineResponseDTO toDTO(Machine machine);
}
