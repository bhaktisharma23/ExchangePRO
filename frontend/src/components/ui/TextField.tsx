import { useId, type InputHTMLAttributes } from 'react';

interface TextFieldProps extends Omit<InputHTMLAttributes<HTMLInputElement>, 'id'> {
  label: string;
  error?: string;
  hint?: string;
}

/** Labelled input with accessible error text (aria-invalid + aria-describedby). */
export function TextField({ label, error, hint, className = '', ...input }: TextFieldProps) {
  const id = useId();
  const describedBy = error ? `${id}-error` : hint ? `${id}-hint` : undefined;
  return (
    <div className={className}>
      <label htmlFor={id} className="mb-1 block text-[11px] font-medium uppercase tracking-wider text-fg-muted">
        {label}
      </label>
      <input
        id={id}
        aria-invalid={error ? true : undefined}
        aria-describedby={describedBy}
        className={`h-9 w-full rounded-sm border bg-surface-2 px-2.5 text-fg outline-none placeholder:text-fg-muted/60 focus:border-accent ${
          error ? 'border-down' : 'border-line'
        }`}
        {...input}
      />
      {error && (
        <p id={`${id}-error`} className="mt-1 text-[12px] text-down">
          {error}
        </p>
      )}
      {!error && hint && (
        <p id={`${id}-hint`} className="mt-1 text-[12px] text-fg-muted">
          {hint}
        </p>
      )}
    </div>
  );
}
