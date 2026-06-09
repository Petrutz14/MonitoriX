import { Component, OnInit, OnDestroy, ChangeDetectorRef } from '@angular/core';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { DatePipe, DecimalPipe } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { forkJoin, Subscription } from 'rxjs';
import { MetricService } from '../../services/metric.service';
import { MachineService } from '../../services/machine.service';
import { AlertService } from '../../services/alert.service';
import { WebSocketService } from '../../services/websocket.service';
import { MetricResponse } from '../../models/metric-response.model';
import { MachineResponse } from '../../models/machine-response.model';
import { AlertResponse, AlertRuleRequest, AlertRuleResponse } from '../../models/alert.model';

@Component({
  selector: 'app-machine-detail',
  imports: [DatePipe, DecimalPipe, RouterLink, FormsModule],
  templateUrl: './machine-detail.html',
  styleUrl: './machine-detail.css',
})
export class MachineDetail implements OnInit, OnDestroy {
  machineId = '';
  machine: MachineResponse | null = null;
  metric: MetricResponse | null = null;

  // display name editing
  editing = false;
  editName = '';
  saving = false;
  saveError = '';

  // alerts
  alertRules: AlertRuleResponse[] = [];
  activeAlerts: AlertResponse[] = [];
  recentAlerts: AlertResponse[] = [];
  showRuleForm = false;
  isGlobalRule = false;
  newRule: AlertRuleRequest = this.emptyRule();
  ruleFormError = '';
  ruleFormSaving = false;

  private wsSub?: Subscription;
  private alertSub?: Subscription;

  constructor(
    private route: ActivatedRoute,
    private metricService: MetricService,
    private machineService: MachineService,
    private alertService: AlertService,
    private webSocketService: WebSocketService,
    private cdr: ChangeDetectorRef
  ) {}

  ngOnInit(): void {
    this.machineId = this.route.snapshot.paramMap.get('machineId') ?? '';

    this.machineService.getMachine(this.machineId).subscribe({
      next: m => { this.machine = m; this.cdr.detectChanges(); },
      error: err => console.error(err)
    });

    this.metricService.getLatestMetric(this.machineId).subscribe({
      next: d => { this.metric = d; this.cdr.detectChanges(); },
      error: () => {}
    });

    this.loadAlertData();

    this.webSocketService.connect();

    this.wsSub = this.webSocketService.metric$.subscribe(data => {
      if (data.machineId === this.machineId) {
        this.metric = data;
        if (this.machine) this.machine.machineStatus = data.machineStatus;
        this.cdr.detectChanges();
      }
    });

    this.alertSub = this.webSocketService.alert$.subscribe(alert => {
      if (alert.machineId !== this.machineId) return;
      if (alert.status === 'ONGOING') {
        if (!this.activeAlerts.find(a => a.id === alert.id)) {
          this.activeAlerts = [alert, ...this.activeAlerts];
        }
        if (!this.recentAlerts.find(a => a.id === alert.id)) {
          this.recentAlerts = [alert, ...this.recentAlerts].slice(0, 20);
        }
      } else {
        this.activeAlerts = this.activeAlerts.filter(a => a.id !== alert.id);
        const idx = this.recentAlerts.findIndex(a => a.id === alert.id);
        if (idx >= 0) {
          this.recentAlerts = [...this.recentAlerts];
          this.recentAlerts[idx] = alert;
        }
      }
      this.cdr.detectChanges();
    });
  }

  private loadAlertData(): void {
    forkJoin({
      machineRules: this.alertService.getAlertRulesForMachine(this.machineId),
      globalRules: this.alertService.getGlobalAlertRules(),
      active: this.alertService.getActiveAlertsForMachine(this.machineId),
      recent: this.alertService.getAlertsForMachine(this.machineId),
    }).subscribe({
      next: ({ machineRules, globalRules, active, recent }) => {
        this.alertRules = [...machineRules, ...globalRules];
        this.activeAlerts = active;
        this.recentAlerts = recent.slice(0, 20);
        this.cdr.detectChanges();
      },
      error: err => console.error('Failed to load alert data', err)
    });
  }

  private emptyRule(): AlertRuleRequest {
    return { machineId: null, metricType: 'CPU_PERCENT', alertOperator: 'HIGHER', alertSeverity: 'MEDIUM', threshold: 80 };
  }

  toggleRuleForm(): void {
    this.showRuleForm = !this.showRuleForm;
    if (this.showRuleForm) {
      this.newRule = this.emptyRule();
      this.isGlobalRule = false;
      this.ruleFormError = '';
    }
  }

  cancelRuleForm(): void {
    this.showRuleForm = false;
    this.ruleFormError = '';
  }

  submitRule(): void {
    this.ruleFormSaving = true;
    this.ruleFormError = '';
    const request: AlertRuleRequest = { ...this.newRule, machineId: this.isGlobalRule ? null : this.machineId };
    this.alertService.createAlertRule(request).subscribe({
      next: rule => {
        this.alertRules = [rule, ...this.alertRules];
        this.showRuleForm = false;
        this.ruleFormSaving = false;
        this.newRule = this.emptyRule();
        this.cdr.detectChanges();
      },
      error: () => {
        this.ruleFormError = 'Failed to create rule. Try again.';
        this.ruleFormSaving = false;
        this.cdr.detectChanges();
      }
    });
  }

  toggleRule(rule: AlertRuleResponse): void {
    this.alertService.toggleAlertRule(rule.id, !rule.enabled).subscribe({
      next: updated => {
        const idx = this.alertRules.findIndex(r => r.id === rule.id);
        if (idx >= 0) { this.alertRules = [...this.alertRules]; this.alertRules[idx] = updated; }
        this.cdr.detectChanges();
      },
      error: err => console.error(err)
    });
  }

  deleteRule(rule: AlertRuleResponse): void {
    this.alertService.deleteAlertRule(rule.id).subscribe({
      next: () => { this.alertRules = this.alertRules.filter(r => r.id !== rule.id); this.cdr.detectChanges(); },
      error: err => console.error(err)
    });
  }

  // display name editing
  startEdit(): void {
    this.editName = this.machine?.displayName ?? '';
    this.saveError = '';
    this.editing = true;
  }

  cancelEdit(): void {
    this.editing = false;
    this.saveError = '';
  }

  saveEdit(): void {
    if (!this.editName.trim() || !this.machine) return;
    this.saving = true;
    this.machineService.updateDisplayName(this.machineId, this.editName.trim()).subscribe({
      next: updated => {
        this.machine = updated;
        this.editing = false;
        this.saving = false;
        this.cdr.detectChanges();
      },
      error: () => {
        this.saveError = 'Save failed. Try again.';
        this.saving = false;
        this.cdr.detectChanges();
      }
    });
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
