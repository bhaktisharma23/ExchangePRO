import { AlertCircle } from 'lucide-react';

export function FormAlert({ message }: { message: string }) {
  return (
    <div role="alert" className="flex items-start gap-2 rounded-sm border border-down/40 bg-down/10 px-3 py-2 text-down">
      <AlertCircle className="mt-0.5 h-4 w-4 shrink-0" aria-hidden />
      <span>{message}</span>
    </div>
  );
}
