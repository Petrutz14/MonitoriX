import { TestBed } from '@angular/core/testing';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { provideHttpClient } from '@angular/common/http';
import { AuthService } from './auth.service';

describe('AuthService', () => {
  let service: AuthService;
  let http: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting()],
    });
    service = TestBed.inject(AuthService);
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => http.verify());

  it('is unauthenticated by default', () => {
    expect(service.isAuthenticated()).toBe(false);
  });

  it('stores token after login', () => {
    service.login({ username: 'peter', password: 'pass' }).subscribe();
    const req = http.expectOne(r => r.url.includes('/login'));
    req.flush({ token: 'fake-jwt' });
    expect(service.isAuthenticated()).toBe(true);
    expect(service.getToken()).toBe('fake-jwt');
  });

  it('clears token on logout', () => {
    service.login({ username: 'peter', password: 'pass' }).subscribe();
    http.expectOne(r => r.url.includes('/login')).flush({ token: 'fake-jwt' });
    service.logout();
    expect(service.isAuthenticated()).toBe(false);
  });
});
