import { MachineStatus } from './machine-status.type';

export interface MachineResponse {
    machineId: string;
    displayName: string;
    osName: string;
    ipAddress: string;
    totalRamGb: number;
    machineStatus: MachineStatus;
    lastSeen: string;
}
