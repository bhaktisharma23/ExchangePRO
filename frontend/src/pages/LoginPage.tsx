import { useState, type FormEvent } from 'react';
import { Link, useLocation, useNavigate } from 'react-router-dom';
import { AuthCard } from '@/layouts/AuthCard';
import { TextField } from '@/components/ui/TextField';
import { Button } from '@/components/ui/Button';
import { FormAlert } from '@/components/ui/FormAlert';
import { useLogin } from '@/hooks/useAuthActions';
import { compactErrors, validateEmail, type FieldErrors } from '@/utils/validation';
import { describeError } from '@/utils/apiErrors';

type Field = 'email' | 'password';

export function LoginPage() {
  const navigate = useNavigate();
  const location = useLocation();
  const from = (location.state as { from?: string } | null)?.from ?? '/markets';
  const login = useLogin();
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [errors, setErrors] = useState<FieldErrors<Field>>({});

  function onSubmit(event: FormEvent) {
    event.preventDefault();
    // Only presence is checked for the password at login; strength rules apply at registration.
    const found = compactErrors({ email: validateEmail(email), password: password ? undefined : 'Password is required' });
    setErrors(found);
    if (Object.keys(found).length > 0) return;
    login.mutate({ email: email.trim(), password }, { onSuccess: () => navigate(from, { replace: true }) });
  }

  return (
    <AuthCard
      title="Sign in"
      subtitle="Access your virtual trading account."
      footer={
        <>
          New to EXCHANGE PRO?{' '}
          <Link to="/register" className="text-accent hover:underline">
            Create an account
          </Link>
        </>
      }
    >
      <form noValidate onSubmit={onSubmit} className="space-y-4">
        {login.isError && <FormAlert message={describeError(login.error)} />}
        <TextField
          label="E-mail"
          type="email"
          autoComplete="email"
          value={email}
          onChange={(e) => setEmail(e.target.value)}
          error={errors.email}
        />
        <TextField
          label="Password"
          type="password"
          autoComplete="current-password"
          value={password}
          onChange={(e) => setPassword(e.target.value)}
          error={errors.password}
        />
        <Button type="submit" loading={login.isPending} className="w-full">
          Sign in
        </Button>
      </form>
    </AuthCard>
  );
}
