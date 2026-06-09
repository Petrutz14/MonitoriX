import { Component, OnInit, OnDestroy, ChangeDetectorRef } from '@angular/core';
import { DatePipe, DecimalPipe } from '@angular/common';
import { RouterLink } from '@angular/router';
import { catchError, forkJoin, of, Subscription } from 'rxjs';
import { MetricService } from '../../services/metric.service';
import { MachineService } from '../../services/machine.service';
import { AlertService } from '../../services/alert.service';
import { WebSocketService } from '../../services/websocket.service';
import { MetricResponse } from '../../models/metric-response.model';
import { MachineResponse } from '../../models/machine-response.model';
import { AlertResponse } from '../../models/alert.model';

@Component({
  selector: 'app-dashboard',
  imports: [DatePipe, DecimalPipe, RouterLink],
  templateUrl: './dashboard.html',
  styleUrl: './dashboard.css',
})
export class Dashboard implements OnInit, OnDestroy {
  machines: MachineResponse[] = [];
  metrics = new Map<string, MetricResponse>();
  activeAlerts = new Map<string, AlertResponse[]>();
  toasts: { id: number; alert: AlertResponse }[] = [];
  loading = true;

  private toastCounter = 0;
  private wsSub?: Subscription;
  private alertSub?: Subscription;

  constructor(
    private metricService: MetricService,
    private machineService: MachineService,
    private alertService: AlertService,
    private webSocketService: WebSocketService,
    private cdr: ChangeDetectorRef
  ) {}

  ngOnInit(): void {
    this.machineService.getMachines().subscribe({
      next: machines => {
        this.machines = machines;
        this.loading = false;
        this.cdr.detectChanges();

        machines.forEach(m => {
          this.metricService.getLatestMetric(m.machineId).subscribe({
            next: data => { this.metrics.set(m.machineId, data); this.cdr.detectChanges(); },
            error: () => {}
          });
        });

        if (machines.length > 0) {
          forkJoin(
            machines.map(m => this.alertService.getActiveAlertsForMachine(m.machineId).pipe(catchError(() => of([]))))
          ).subscribe(results => {
            results.forEach((alerts, i) => {
              if (alerts.length > 0) this.activeAlerts.set(machines[i].machineId, alerts);
            });
            this.cdr.detectChanges();
          });
        }
      },
      error: err => { console.error('Failed to load machines:', err); this.loading = false; this.cdr.detectChanges(); }
    });

    this.webSocketService.connect();

    // Update metrics AND machine status live
    this.wsSub = this.webSocketService.metric$.subscribe(data => {
      this.metrics.set(data.machineId, data);
      const machine = this.machines.find(m => m.machineId === data.machineId);
      if (machine && machine.machineStatus !== data.machineStatus) {
        machine.machineStatus = data.machineStatus;
        this.machines = [...this.machines]; // trigger trackBy refresh
      }
      this.cdr.detectChanges();
    });

    this.alertSub = this.webSocketService.alert$.subscribe(alert => {
      if (alert.status === 'ONGOING') {
        const current = this.activeAlerts.get(alert.machineId) ?? [];
        if (!current.find(a => a.id === alert.id)) {
          this.activeAlerts.set(alert.machineId, [alert, ...current]);
        }
        this.showToast(alert);
      } else {
        const current = this.activeAlerts.get(alert.machineId) ?? [];
        const updated = current.filter(a => a.id !== alert.id);
        if (updated.length === 0) this.activeAlerts.delete(alert.machineId);
        else this.activeAlerts.set(alert.machineId, updated);
      }
      this.cdr.detectChanges();
    });
  }

  private showToast(alert: AlertResponse): void {
    const id = ++this.toastCounter;
    this.toasts = [...this.toasts, { id, alert }];
    this.cdr.detectChanges();
    setTimeout(() => {
      this.toasts = this.toasts.filter(t => t.id !== id);
      this.cdr.detectChanges();
    }, 6000);
  }

  dismissToast(id: number): void {
    this.toasts = this.toasts.filter(t => t.id !== id);
  }

  get allActiveAlerts(): AlertResponse[] {
    const sevOrder: Record<string, number> = { CRITICAL: 0, HIGH: 1, MEDIUM: 2, LOW: 3 };
    const all: AlertResponse[] = [];
    this.activeAlerts.forEach(alerts => all.push(...alerts));
    return all.sort((a, b) => {
      const s = (sevOrder[a.alertSeverity] ?? 3) - (sevOrder[b.alertSeverity] ?? 3);
      return s !== 0 ? s : new Date(b.triggeredAt).getTime() - new Date(a.triggeredAt).getTime();
    });
  }

  getMetric(machineId: string): MetricResponse | undefined {
    return this.metrics.get(machineId);
  }

  getAlertCount(machineId: string): number {
    return this.activeAlerts.get(machineId)?.length ?? 0;
  }

  isStale(status: string): boolean {
    return status === 'OFFLINE' || status === 'IDLE';
  }

  getAlertClass(value: number): string {
    if (value >= 85) return 'danger';
    if (value >= 60) return 'warn';
    return 'ok';
  }

  getSeverityClass(severity: string): string {
    switch (severity) {
      case 'CRITICAL': return 'sev-critical';
      case 'HIGH':     return 'sev-high';
      case 'MEDIUM':   return 'sev-medium';
      default:         return 'sev-low';
    }
  }

  formatMetricType(type: string): string {
    switch (type) {
      case 'CPU_PERCENT':  return 'CPU';
      case 'RAM_PERCENT':  return 'RAM';
      case 'DISK_PERCENT': return 'Disk';
      default: return type;
    }
  }

  formatOperator(op: string): string {
    switch (op) {
      case 'HIGHER':          return '>';
      case 'HIGHER_OR_EQUAL': return '≥';
      case 'LOWER':           return '<';
      case 'LOWER_OR_EQUAL':  return '≤';
      case 'EQUAL':           return '=';
      default: return op;
    }
  }

  formatTimeAgo(timestamp: string): string {
    const diff = Date.now() - new Date(timestamp).getTime();
    const mins  = Math.floor(diff / 60000);
    const hours = Math.floor(diff / 3600000);
    const days  = Math.floor(diff / 86400000);
    if (days  > 0) return `${days}d ago`;
    if (hours > 0) return `${hours}h ago`;
    if (mins  > 0) return `${mins}m ago`;
    return 'just now';
  }

  formatUptime(seconds: number): string {
    const d = Math.floor(seconds / 86400);
    const h = Math.floor((seconds % 86400) / 3600);
    const m = Math.floor((seconds % 3600) / 60);
    if (d > 0) return `${d}d ${h}h ${m}m`;
    if (h > 0) return `${h}h ${m}m`;
    return `${m}m`;
  }

  ngOnDestroy(): void {
    this.wsSub?.unsubscribe();
    this.alertSub?.unsubscribe();
    this.webSocketService.disconnect();
  }
}
