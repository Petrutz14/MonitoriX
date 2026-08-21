import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { MetricResponse } from '../models/metric-response.model';
import { MetricBucketResponse } from '../models/metric-bucket-response.model';
import { environment } from '../../environments/environment';

@Injectable({ providedIn: 'root' })
export class MetricService {
  private readonly apiUrl = `${environment.apiUrl}/api/metrics`;

  constructor(private http: HttpClient) {}

  getLatestMetric(machineId: string): Observable<MetricResponse> {
    return this.http.get<MetricResponse>(`${this.apiUrl}/${machineId}`);
  }

  getMetricHistory(machineId: string, minutes = 30): Observable<MetricBucketResponse[]> {
    const params = new HttpParams().set('minutes', String(minutes));
    return this.http.get<MetricBucketResponse[]>(`${this.apiUrl}/${machineId}/history`, { params });
  }

  simulateMetric(machineId: string): Observable<string> {
    return this.http.post(`${this.apiUrl}/simulate/${machineId}`, null, { responseType: 'text' });
  }
}
