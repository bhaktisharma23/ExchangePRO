import { useState, type ChangeEvent, type FormEvent } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { AuthCard } from '@/layouts/AuthCard';
import { TextField } from '@/components/ui/TextField';
import { Button } from '@/components/ui/Button';
import { FormAlert } from '@/components/ui/FormAlert';
import { useRegister } from '@/hooks/useAuthActions';
import {
  compactErrors,
  validateDisplayName,
  validateEmail,
  validatePassword,
  type FieldErrors,
} from '@/utils/validation';
import { describeError, serverFieldErrors } from '@/utils/apiErrors';

const FIELDS = ['displayName', 'email', 'password'] as const;
type Field = (typeof FIELDS)[number];

export function RegisterPage() {
  const navigate = useNavigate();
  const register = useRegister();
  const [form, setForm] = useState({ displayName: '', email: '', password: '' });
  const [errors, setErrors] = useState<FieldErrors<Field>>({});

  const set = (field: Field) => (e: ChangeEvent<HTMLInputElement>) =>
    setForm((f) => ({ ...f, [field]: e.target.value }));

  function onSubmit(event: FormEvent) {
    event.preventDefault();
    const found = compactErrors({
      displayName: validateDisplayName(form.displayName),
      email: validateEmail(form.email),
      password: validatePassword(form.password),
    });
    setErrors(found);
    if (Object.keys(found).length > 0) return;
    register.mutate(
      { displayName: form.displayName.trim(), email: form.email.trim(), password: form.password },
      {
        onSuccess: () => navigate('/account', { replace: true }),
        onError: (error) => setErrors(serverFieldErrors(error, FIELDS)),
      },
    );
  }

  return (
    <AuthCard
      title="Create account"
      subtitle="Start with ₹1,00,000 in virtual funds. No real money is involved."
      footer={
        <>
          Already registered?{' '}
          <Link to="/login" className="text-accent hover:underline">
            Sign in
          </Link>
        </>
      }
    >
      <form noValidate onSubmit={onSubmit} className="space-y-4">
        {register.isError && <FormAlert message={describeError(register.error)} />}
        <TextField
          label="Display name"
          autoComplete="nickname"
          value={form.displayName}
          onChange={set('displayName')}
          error={errors.displayName}
        />
        <TextField
          label="E-mail"
          type="email"
          autoComplete="email"
          value={form.email}
          onChange={set('email')}
          error={errors.email}
        />
        <TextField
          label="Password"
          type="password"
          autoComplete="new-password"
          value={form.password}
          onChange={set('password')}
          error={errors.password}
          hint="At least 8 characters, with a letter and a digit."
        />
        <Button type="submit" loading={register.isPending} className="w-full">
          Create account
        </Button>
      </form>
    </AuthCard>
  );
}
