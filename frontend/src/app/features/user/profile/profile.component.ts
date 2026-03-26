import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { UserService } from '../../../core/services/user.service';
import { AuthService } from '../../../core/services/auth.service';
import { User } from '../../../core/models/user.model';

@Component({
  selector: 'app-profile',
  standalone: true,
  imports: [CommonModule, FormsModule],
  template: `
    <div class="profile-container">
      <div class="profile-card">
        <button class="back-btn" (click)="goBack()">← Back to Chat</button>
        <div class="avatar-large">{{ user?.displayName?.charAt(0) || user?.username?.charAt(0) || '?' }}</div>
        <h2>{{ user?.displayName || user?.username }}</h2>
        <p class="email">{{ user?.email }}</p>
        <p class="username">&#64;{{ user?.username }}</p>

        <div class="form-group">
          <label>Display Name</label>
          <input [(ngModel)]="displayName" placeholder="Update display name">
        </div>
        <button class="btn-primary" (click)="updateProfile()">
          {{ saving ? 'Saving...' : 'Update Profile' }}
        </button>
      </div>
    </div>
  `,
  styles: [`
    .profile-container {
      display: flex;
      justify-content: center;
      align-items: center;
      height: 100vh;
      background: var(--bg-light);
    }
    .profile-card {
      background: var(--bg-white);
      border-radius: 12px;
      padding: 40px;
      width: 400px;
      text-align: center;
      box-shadow: 0 2px 10px rgba(0,0,0,0.1);
    }
    .back-btn {
      background: none;
      color: var(--primary);
      font-size: 14px;
      margin-bottom: 20px;
    }
    .avatar-large {
      width: 80px;
      height: 80px;
      border-radius: 50%;
      background: var(--primary);
      color: white;
      display: flex;
      align-items: center;
      justify-content: center;
      font-size: 32px;
      font-weight: 600;
      margin: 0 auto 16px;
    }
    h2 { margin-bottom: 4px; }
    .email { color: var(--text-secondary); font-size: 13px; }
    .username { color: var(--text-secondary); font-size: 13px; margin-bottom: 24px; }
    .form-group {
      text-align: left;
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
    .btn-primary {
      width: 100%;
      padding: 12px;
      background: var(--primary);
      color: white;
      border-radius: 8px;
      font-size: 15px;
      font-weight: 600;
    }
    .btn-primary:hover { background: var(--primary-dark); }
  `]
})
export class ProfileComponent implements OnInit {
  user: User | null = null;
  displayName = '';
  saving = false;

  constructor(
    private userService: UserService,
    private authService: AuthService,
    private router: Router
  ) {}

  ngOnInit(): void {
    this.userService.getCurrentUser().subscribe({
      next: (res) => {
        this.user = res.data;
        this.displayName = res.data.displayName || '';
      }
    });
  }

  updateProfile(): void {
    this.saving = true;
    this.userService.updateProfile(this.displayName).subscribe({
      next: (res) => {
        this.user = res.data;
        this.saving = false;
      },
      error: () => this.saving = false
    });
  }

  goBack(): void {
    this.router.navigate(['/chat']);
  }
}
