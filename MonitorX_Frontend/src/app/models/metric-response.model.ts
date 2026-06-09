import { MachineStatus } from "./machine-status.type";

export interface TopProcess {
    pid: number;
    name: string;
    cpuPercent: number;
    ramPercent: number;
    ramUsedMb: number;
}

export interface DiskPartition {
    device: string;
    mountPoint: string;
    fileSystem: string;
    totalGb: number;
    usedGb: number;
    percent: number;
}

export interface MetricResponse {
    machineId: string;
    displayName: string;
    cpuPercent: number;
    ramPercent: number;
    ramUsedGb: number;
    diskPercent: number;
    diskFreeGb: number;
    osName: string;
    ipAddress: string;
    totalRamGb: number;
    uptimeSeconds: number;
    topProcesses: TopProcess[];
    diskPartitions: DiskPartition[];
    recordedAt: string;
    machineStatus: MachineStatus;
}