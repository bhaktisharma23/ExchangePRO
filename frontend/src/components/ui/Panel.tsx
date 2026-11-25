import type { ReactNode } from 'react';

interface PanelProps {
  title: string;
  actions?: ReactNode;
  children: ReactNode;
  className?: string;
}

/** Bordered terminal-style panel with a compact header row. */
export function Panel({ title, actions, children, className = '' }: PanelProps) {
  return (
    <section className={`rounded-sm border border-line bg-surface ${className}`} aria-label={title}>
      <header className="flex h-9 items-center justify-between border-b border-line px-3">
        <h2 className="text-[11px] font-semibold uppercase tracking-wider text-fg-muted">{title}</h2>
        {actions}
      </header>
      <div className="p-3">{children}</div>
    </section>
  );
}
