import { Injectable } from '@angular/core';
import { Client } from '@stomp/stompjs';
import { Subject } from 'rxjs';
import { MetricResponse } from '../models/metric-response.model';
import { AlertResponse } from '../models/alert.model';

@Injectable({ providedIn: 'root' })
export class WebSocketService {
  private client: Client;
  private metricSubject = new Subject<MetricResponse>();
  private alertSubject = new Subject<AlertResponse>();
  private refCount = 0;

  metric$ = this.metricSubject.asObservable();
  alert$ = this.alertSubject.asObservable();

  constructor() {
    this.client = new Client({
      brokerURL: 'ws://localhost:8080/ws',
      onConnect: () => {
        this.client.subscribe('/topic/metrics', message => {
          this.metricSubject.next(JSON.parse(message.body));
        });
        this.client.subscribe('/topic/alerts', message => {
          this.alertSubject.next(JSON.parse(message.body));
        });
      },
      onStompError: frame => console.error('STOMP error:', frame),
      onWebSocketError: event => console.error('WebSocket error:', event),
    });
  }

  connect(): void {
    this.refCount++;
    if (this.refCount === 1) this.client.activate();
  }

  disconnect(): void {
    this.refCount = Math.max(0, this.refCount - 1);
    if (this.refCount === 0) this.client.deactivate();
  }
}
