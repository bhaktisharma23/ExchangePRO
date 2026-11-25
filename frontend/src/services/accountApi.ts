import type { Account } from '@/types/account';
import { apiRequest } from './apiClient';

export const accountApi = {
  getMyAccount: (signal?: AbortSignal) => apiRequest<Account>('/api/v1/accounts/me', { signal }),
};
