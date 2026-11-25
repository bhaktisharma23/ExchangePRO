import type { SystemStatus } from '@/types/system';
import { apiRequest } from './apiClient';

export const systemApi = {
  getStatus: (signal?: AbortSignal) => apiRequest<SystemStatus>('/api/v1/system/status', { signal }),
};
