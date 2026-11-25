import { useMutation, useQueryClient } from '@tanstack/react-query';
import { authApi } from '@/services/authApi';
import { userApi } from '@/services/userApi';
import { useAuthStore } from '@/stores/authStore';
import type { LoginRequest, RegisterRequest } from '@/types/auth';

export function useLogin() {
  return useMutation({ mutationFn: (request: LoginRequest) => authApi.login(request) });
}

export function useRegister() {
  return useMutation({ mutationFn: (request: RegisterRequest) => authApi.register(request) });
}

/** Logs out and drops every cached query so the next user never sees the previous user's data. */
export function useLogout() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: () => authApi.logout(),
    onSettled: () => queryClient.clear(),
  });
}

export function useUpdateProfile() {
  return useMutation({
    mutationFn: (displayName: string) => userApi.updateMe(displayName),
    onSuccess: (user) => useAuthStore.getState().setUser(user),
  });
}
