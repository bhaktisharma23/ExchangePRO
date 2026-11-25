import type { ReactNode } from 'react';

export interface KeyValueItem {
  label: string;
  value: ReactNode;
}

/** Dense two-column label/value list used inside panels. */
export function KeyValueList({ items }: { items: KeyValueItem[] }) {
  return (
    <dl className="grid grid-cols-[minmax(120px,auto)_1fr] gap-x-4 gap-y-2">
      {items.map((item) => (
        <div key={item.label} className="contents">
          <dt className="text-fg-muted">{item.label}</dt>
          <dd className="num text-right font-mono text-fg">{item.value}</dd>
        </div>
      ))}
    </dl>
  );
}
