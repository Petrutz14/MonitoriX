export type MetricType = 'CPU_PERCENT' | 'RAM_PERCENT' | 'DISK_PERCENT';
export type AlertOperator = 'LOWER' | 'LOWER_OR_EQUAL' | 'HIGHER' | 'HIGHER_OR_EQUAL' | 'EQUAL';
export type AlertSeverity = 'LOW' | 'MEDIUM' | 'HIGH' | 'CRITICAL';
export type AlertStatus = 'ONGOING' | 'ACKNOWLEDGED' | 'RESOLVED';

export interface AlertRuleResponse {
  id: number;
  machineId: string | null;
  metricType: MetricType;
  alertOperator: AlertOperator;
  alertSeverity: AlertSeverity;
  threshold: number;
  enabled: boolean;
  createdAt: string;
}

export interface AlertRuleRequest {
  machineId: string | null;
  metricType: MetricType;
  alertOperator: AlertOperator;
  alertSeverity: AlertSeverity;
  threshold: number;
  enabled?: boolean;
}

export interface AlertResponse {
  id: number;
  alertRuleId: number;
  machineId: string;
  displayName: string;
  metricType: MetricType;
  alertSeverity: AlertSeverity;
  alertOperator: AlertOperator;
  threshold: number;
  triggeredValue: number;
  status: AlertStatus;
  triggeredAt: string;
  resolvedAt: string | null;
}
