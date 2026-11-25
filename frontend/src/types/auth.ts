/** Mirrors com.exchangepro.user.Role. */
export type Role = 'USER' | 'ADMIN';

/** Mirrors com.exchangepro.user.UserProfileResponse. */
export interface UserProfile {
  id: string;
  email: string;
  displayName: string;
  role: Role;
  enabled: boolean;
  createdAt: string;
}

/** Mirrors com.exchangepro.auth.AuthResponse. The refresh token is never in the body (HttpOnly cookie). */
export interface AuthResponse {
  accessToken: string;
  tokenType: 'Bearer';
  expiresIn: number;
  user: UserProfile;
}

export interface LoginRequest {
  email: string;
  password: string;
}

export interface RegisterRequest {
  email: string;
  password: string;
  displayName: string;
}
