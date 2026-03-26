import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ReactiveFormsModule, FormBuilder, FormGroup, Validators } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { AuthService } from '../../../core/services/auth.service';

@Component({
  selector: 'app-register',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, RouterLink],
  templateUrl: './register.component.html',
  styles: [`
    .register-container {
      display: flex;
      justify-content: center;
      align-items: center;
      height: 100vh;
      background: var(--bg-light);
    }
    .register-card {
      background: var(--bg-white);
      border-radius: 12px;
      padding: 40px;
      width: 420px;
      box-shadow: 0 2px 10px rgba(0,0,0,0.1);
    }
    .register-card h1 {
      text-align: center;
      color: var(--primary);
      margin-bottom: 8px;
    }
    .register-card p.subtitle {
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
    }
    .form-group input {
      width: 100%;
      padding: 10px 12px;
      border: 1px solid var(--border);
      border-radius: 8px;
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
    .btn-primary:hover { background: var(--primary-dark); }
    .btn-primary:disabled { opacity: 0.6; cursor: not-allowed; }
    .error { color: var(--danger); font-size: 12px; margin-top: 4px; }
    .link-text { text-align: center; margin-top: 16px; color: var(--text-secondary); }
    .link-text a { color: var(--primary); font-weight: 500; }
    .server-error {
      background: #fef2f2; color: var(--danger); padding: 10px;
      border-radius: 8px; margin-bottom: 16px; text-align: center; font-size: 13px;
    }
    .success-message {
      background: #f0fdf4; color: #16a34a; padding: 10px;
      border-radius: 8px; margin-bottom: 16px; text-align: center; font-size: 13px;
    }
    .otp-info {
      text-align: center; color: var(--text-secondary); margin-bottom: 20px; font-size: 14px;
    }
    .otp-actions {
      display: flex; justify-content: space-between; margin-top: 16px;
    }
    .btn-link {
      background: none; border: none; color: var(--primary); cursor: pointer;
      font-size: 13px; font-weight: 500; padding: 0;
    }
    .btn-link:hover { text-decoration: underline; }
    .btn-link:disabled { color: var(--text-secondary); cursor: not-allowed; text-decoration: none; }
  `]
})
export class RegisterComponent {
  form: FormGroup;
  otpForm: FormGroup;
  loading = false;
  serverError = '';
  successMessage = '';
  step: 'register' | 'verify' = 'register';
  pendingEmail = '';
  resendCooldown = 0;
  private resendTimer: any;

  constructor(
    private fb: FormBuilder,
    private authService: AuthService,
    private router: Router
  ) {
    this.form = this.fb.group({
      username: ['', [Validators.required, Validators.minLength(3), Validators.maxLength(50)]],
      email: ['', [Validators.required, Validators.email]],
      password: ['', [Validators.required, Validators.minLength(6)]],
      displayName: ['']
    });
    this.otpForm = this.fb.group({
      otp: ['', [Validators.required, Validators.minLength(6), Validators.maxLength(6)]]
    });
  }

  onSubmit(): void {
    if (this.form.invalid) return;
    this.loading = true;
    this.serverError = '';

    this.authService.register(this.form.value).subscribe({
      next: () => {
        this.loading = false;
        this.pendingEmail = this.form.value.email;
        this.step = 'verify';
        this.startResendCooldown();
      },
      error: (err: any) => {
        this.loading = false;
        this.serverError = err.error?.message || 'Registration failed. Please try again.';
      }
    });
  }

  verifyOtp(): void {
    if (this.otpForm.invalid) return;
    this.loading = true;
    this.serverError = '';

    this.authService.verifyOtp(this.pendingEmail, this.otpForm.value.otp).subscribe({
      next: () => {
        this.router.navigate(['/chat']);
      },
      error: (err: any) => {
        this.loading = false;
        this.serverError = err.error?.message || 'Invalid OTP. Please try again.';
      }
    });
  }

  resendOtp(): void {
    if (this.resendCooldown > 0) return;
    this.serverError = '';
    this.successMessage = '';

    this.authService.resendOtp(this.pendingEmail).subscribe({
      next: () => {
        this.successMessage = 'OTP resent to your email';
        this.startResendCooldown();
      },
      error: (err: any) => {
        this.serverError = err.error?.message || 'Failed to resend OTP.';
      }
    });
  }

  goBack(): void {
    this.step = 'register';
    this.otpForm.reset();
    this.serverError = '';
    this.successMessage = '';
    clearInterval(this.resendTimer);
  }

  private startResendCooldown(): void {
    this.resendCooldown = 30;
    clearInterval(this.resendTimer);
    this.resendTimer = setInterval(() => {
      this.resendCooldown--;
      if (this.resendCooldown <= 0) clearInterval(this.resendTimer);
    }, 1000);
  }
}
