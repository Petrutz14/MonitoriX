import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { BehaviorSubject, Observable, tap } from 'rxjs';
import { AuthResponse, LoginRequest, RegisterRequest } from '../models/auth.model';
import { environment } from '../../environments/environment';

const TOKEN_KEY = 'monitorix_token';

@Injectable({ providedIn: 'root' })
export class AuthService {
  private readonly baseUrl = `${environment.apiUrl}/api/auth`;
  private tokenSubject = new BehaviorSubject<string | null>(sessionStorage.getItem(TOKEN_KEY));

  token$ = this.tokenSubject.asObservable();

  constructor(private http: HttpClient) {}

  login(request: LoginRequest): Observable<AuthResponse> {
    return this.http.post<AuthResponse>(`${this.baseUrl}/login`, request).pipe(
      tap(res => this.setToken(res.token))
    );
  }

  register(request: RegisterRequest): Observable<AuthResponse> {
    return this.http.post<AuthResponse>(`${this.baseUrl}/register`, request).pipe(
      tap(res => this.setToken(res.token))
    );
  }

  logout(): void {
    sessionStorage.removeItem(TOKEN_KEY);
    this.tokenSubject.next(null);
  }

  getToken(): string | null {
    return this.tokenSubject.getValue();
  }

  isAuthenticated(): boolean {
    return this.tokenSubject.getValue() !== null;
  }

  private setToken(token: string): void {
    sessionStorage.setItem(TOKEN_KEY, token);
    this.tokenSubject.next(token);
  }
}
