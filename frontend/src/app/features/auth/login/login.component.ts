import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ReactiveFormsModule, FormBuilder, FormGroup, Validators } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { AuthService } from '../../../core/services/auth.service';

@Component({
  selector: 'app-login',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, RouterLink],
  templateUrl: './login.component.html',
  styles: [`
    .login-container {
      display: flex;
      justify-content: center;
      align-items: center;
      height: 100vh;
      background: var(--bg-light);
    }
    .login-card {
      background: var(--bg-white);
      border-radius: 12px;
      padding: 40px;
      width: 400px;
      box-shadow: 0 2px 10px rgba(0,0,0,0.1);
    }
    .login-card h1 {
      text-align: center;
      color: var(--primary);
      margin-bottom: 8px;
    }
    .login-card p.subtitle {
      text-align: center;
      color: var(--text-secondary);
      margin-bottom: 24px;
    }
    .form-group {
      margin-bottom: 16px;
    }
    .form-group label {
      display: block;
      margin-bottom: 6px;
      font-weight: 500;
      color: var(--text-primary);
    }
    .form-group input {
      width: 100%;
      padding: 10px 12px;
      border: 1px solid var(--border);
      border-radius: 8px;
      font-size: 14px;
    }
    .form-group input:focus {
      border-color: var(--primary);
    }
    .btn-primary {
      width: 100%;
      padding: 12px;
      background: var(--primary);
      color: white;
      border-radius: 8px;
      font-size: 15px;
      font-weight: 600;
      margin-top: 8px;
    }
    .btn-primary:hover {
      background: var(--primary-dark);
    }
    .btn-primary:disabled {
      opacity: 0.6;
      cursor: not-allowed;
    }
    .error {
      color: var(--danger);
      font-size: 12px;
      margin-top: 4px;
    }
    .link-text {
      text-align: center;
      margin-top: 16px;
      color: var(--text-secondary);
    }
    .link-text a {
      color: var(--primary);
      font-weight: 500;
    }
    .server-error {
      background: #fef2f2;
      color: var(--danger);
      padding: 10px;
      border-radius: 8px;
      margin-bottom: 16px;
      text-align: center;
      font-size: 13px;
    }
  `]
})
export class LoginComponent {
  form: FormGroup;
  loading = false;
  serverError = '';

  constructor(
    private fb: FormBuilder,
    private authService: AuthService,
    private router: Router
  ) {
    this.form = this.fb.group({
      username: ['', [Validators.required, Validators.minLength(3)]],
      password: ['', [Validators.required, Validators.minLength(6)]]
    });
  }

  onSubmit(): void {
    if (this.form.invalid) return;
    this.loading = true;
    this.serverError = '';

    this.authService.login(this.form.value).subscribe({
      next: () => {
        this.router.navigate(['/chat']);
      },
      error: (err: any) => {
        this.loading = false;
        this.serverError = err.error?.message || 'Login failed. Please try again.';
      }
    });
  }
}
