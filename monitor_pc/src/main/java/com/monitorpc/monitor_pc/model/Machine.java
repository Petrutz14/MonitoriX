package com.monitorpc.monitor_pc.model;

import com.monitorpc.monitor_pc.enums.MachineStatus;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

@Entity
@Table(name = "machine")
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Setter
@Getter
public class Machine {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false,unique = true)
    private String machineId;
    private String displayName;
    private String osName;
    private String ipAddress;
    private Double totalRamGb;
    @Enumerated(EnumType.STRING)
    private MachineStatus status;
    private Instant lastSeen;
    @Builder.Default
    private Instant createdAt=Instant.now();






}
