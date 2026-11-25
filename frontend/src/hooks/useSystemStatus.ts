import { useQuery } from '@tanstack/react-query';
import { systemApi } from '@/services/systemApi';

export const SYSTEM_STATUS_REFRESH_MS = 15_000;

/** Fetches the backend system status and refreshes it periodically. */
export function useSystemStatus() {
  return useQuery({
    queryKey: ['system', 'status'],
    queryFn: ({ signal }) => systemApi.getStatus(signal),
    refetchInterval: SYSTEM_STATUS_REFRESH_MS,
    retry: 1,
  });
}
