import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { MetricResponse } from '../models/metric-response.model';

@Injectable({ providedIn: 'root' })
export class MetricService {
  private readonly apiUrl = 'http://localhost:8080/api/metrics';

  constructor(private http: HttpClient) {}

  getLatestMetric(machineId: string): Observable<MetricResponse> {
    return this.http.get<MetricResponse>(`${this.apiUrl}/${machineId}`);
  }

  getMetricHistory(machineId: string, minutes = 30): Observable<MetricResponse[]> {
    const params = new HttpParams().set('minutes', String(minutes));
    return this.http.get<MetricResponse[]>(`${this.apiUrl}/${machineId}/history`, { params });
  }
}
