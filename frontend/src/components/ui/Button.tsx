import type { ButtonHTMLAttributes } from 'react';
import { Loader2 } from 'lucide-react';

interface ButtonProps extends ButtonHTMLAttributes<HTMLButtonElement> {
  variant?: 'primary' | 'secondary';
  loading?: boolean;
}

export function Button({ variant = 'primary', loading = false, disabled, children, className = '', ...rest }: ButtonProps) {
  const styles =
    variant === 'primary'
      ? 'bg-accent text-white hover:bg-accent/90'
      : 'border border-line bg-surface-2 text-fg hover:bg-surface-3';
  return (
    <button
      disabled={disabled || loading}
      className={`inline-flex h-9 items-center justify-center gap-2 rounded-sm px-3 font-semibold disabled:cursor-not-allowed disabled:opacity-50 ${styles} ${className}`}
      {...rest}
    >
      {loading && <Loader2 className="h-4 w-4 animate-spin" aria-hidden />}
      {children}
    </button>
  );
}
