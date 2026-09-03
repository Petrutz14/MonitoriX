import { Component, ChangeDetectorRef } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { AuthService } from '../../services/auth.service';

@Component({
  selector: 'app-login',
  imports: [FormsModule],
  templateUrl: './login.html',
  styleUrl: './login.css',
})
export class LoginComponent {
  mode: 'login' | 'register' = 'login';
  username = '';
  email = '';
  password = '';
  error = '';
  loading = false;

  constructor(
    private authService: AuthService,
    private router: Router,
    private cdr: ChangeDetectorRef
  ) {}

  submit(): void {
    this.error = '';
    this.loading = true;

    const obs = this.mode === 'login'
      ? this.authService.login({ username: this.username, password: this.password })
      : this.authService.register({ username: this.username, email: this.email, password: this.password });

    obs.subscribe({
      next: () => { this.router.navigate(['/dashboard']); },
      error: err => {
        if (err.status === 401) this.error = 'Invalid username or password.';
        else if (err.status === 429) this.error = 'Too many requests. Try again later.';
        else if (err.status === 409) this.error = 'Username already taken.';
        else if (err.status === 400) this.error = err.error?.message || 'Please check your input.';
        else this.error = 'Something went wrong. Try again later.';
        this.loading = false;
        this.cdr.detectChanges();
      }
    });
  }

  toggleMode(): void {
    this.mode = this.mode === 'login' ? 'register' : 'login';
    this.error = '';
    this.cdr.detectChanges();
  }
}
