import type { ComponentState } from '@/types/system';

/** Coloured dot + text for UP/DOWN. Text is always present so state is not conveyed by colour alone. */
export function StatusIndicator({ state }: { state: ComponentState }) {
  const up = state === 'UP';
  return (
    <span className={`inline-flex items-center gap-1.5 font-semibold ${up ? 'text-up' : 'text-down'}`}>
      <span aria-hidden className={`h-2 w-2 rounded-full ${up ? 'bg-up' : 'bg-down'}`} />
      {state}
    </span>
  );
}
