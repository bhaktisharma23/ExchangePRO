import type { ReactNode } from 'react';

/** Centered panel used by the login and registration pages. */
export function AuthCard({ title, subtitle, children, footer }: {
  title: string;
  subtitle: string;
  children: ReactNode;
  footer: ReactNode;
}) {
  return (
    <div className="mx-auto mt-10 w-full max-w-sm">
      <div className="rounded-sm border border-line bg-surface">
        <div className="border-b border-line px-5 py-4">
          <h1 className="text-base font-semibold">{title}</h1>
          <p className="text-fg-muted">{subtitle}</p>
        </div>
        <div className="p-5">{children}</div>
      </div>
      <div className="mt-3 text-center text-fg-muted">{footer}</div>
    </div>
  );
}
