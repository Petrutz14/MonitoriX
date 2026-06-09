import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { AlertResponse, AlertRuleRequest, AlertRuleResponse } from '../models/alert.model';

@Injectable({ providedIn: 'root' })
export class AlertService {
  private readonly apiUrl = 'http://localhost:8080/api';

  constructor(private http: HttpClient) {}

  createAlertRule(request: AlertRuleRequest): Observable<AlertRuleResponse> {
    return this.http.post<AlertRuleResponse>(`${this.apiUrl}/alert-rules`, request);
  }

  getAllAlertRules(): Observable<AlertRuleResponse[]> {
    return this.http.get<AlertRuleResponse[]>(`${this.apiUrl}/alert-rules`);
  }

  getGlobalAlertRules(): Observable<AlertRuleResponse[]> {
    return this.http.get<AlertRuleResponse[]>(`${this.apiUrl}/alert-rules/global`);
  }

  getAlertRulesForMachine(machineId: string): Observable<AlertRuleResponse[]> {
    return this.http.get<AlertRuleResponse[]>(`${this.apiUrl}/alert-rules/${machineId}`);
  }

  toggleAlertRule(id: number, enabled: boolean): Observable<AlertRuleResponse> {
    const params = new HttpParams().set('enabled', String(enabled));
    return this.http.patch<AlertRuleResponse>(`${this.apiUrl}/alert-rules/${id}/toggle`, null, { params });
  }

  deleteAlertRule(id: number): Observable<void> {
    return this.http.delete<void>(`${this.apiUrl}/alert-rules/${id}`);
  }

  getAlertsForMachine(machineId: string): Observable<AlertResponse[]> {
    return this.http.get<AlertResponse[]>(`${this.apiUrl}/alerts/${machineId}`);
  }

  getActiveAlertsForMachine(machineId: string): Observable<AlertResponse[]> {
    return this.http.get<AlertResponse[]>(`${this.apiUrl}/alerts/${machineId}/active`);
  }
}
