import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { BehaviorSubject, Observable, tap } from 'rxjs';
import { AuthResponse, LoginRequest, RegisterRequest } from '../models/auth.model';

@Injectable({ providedIn: 'root' })
export class AuthService {
  private readonly baseUrl = 'http://localhost:8080/api/auth';
  private tokenSubject = new BehaviorSubject<string | null>(null);

  token$ = this.tokenSubject.asObservable();

  constructor(private http: HttpClient) {}

  login(request: LoginRequest): Observable<AuthResponse> {
    return this.http.post<AuthResponse>(`${this.baseUrl}/login`, request).pipe(
      tap(res => this.tokenSubject.next(res.token))
    );
  }

  register(request: RegisterRequest): Observable<AuthResponse> {
    return this.http.post<AuthResponse>(`${this.baseUrl}/register`, request).pipe(
      tap(res => this.tokenSubject.next(res.token))
    );
  }

  logout(): void {
    this.tokenSubject.next(null);
  }

  getToken(): string | null {
    return this.tokenSubject.getValue();
  }

  isAuthenticated(): boolean {
    return this.tokenSubject.getValue() !== null;
  }
}
