import type { UserProfile } from '@/types/auth';
import { apiRequest } from './apiClient';

export const userApi = {
  getMe: (signal?: AbortSignal) => apiRequest<UserProfile>('/api/v1/users/me', { signal }),
  updateMe: (displayName: string) =>
    apiRequest<UserProfile>('/api/v1/users/me', { method: 'PATCH', body: { displayName } }),
};
