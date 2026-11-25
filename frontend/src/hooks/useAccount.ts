import { useQuery } from '@tanstack/react-query';
import { accountApi } from '@/services/accountApi';
import { useAuthStore } from '@/stores/authStore';

export const accountQueryKey = (userId: string | undefined) => ['account', 'me', userId] as const;

/** The signed-in user's virtual account. The user id is part of the key so caches never leak across users. */
export function useMyAccount() {
  const userId = useAuthStore((s) => s.user?.id);
  return useQuery({
    queryKey: accountQueryKey(userId),
    queryFn: ({ signal }) => accountApi.getMyAccount(signal),
    enabled: userId !== undefined,
  });
}
