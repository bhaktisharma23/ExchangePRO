import { useState, type FormEvent } from 'react';
import { Check } from 'lucide-react';
import { Panel } from '@/components/ui/Panel';
import { KeyValueList } from '@/components/ui/KeyValueList';
import { TextField } from '@/components/ui/TextField';
import { Button } from '@/components/ui/Button';
import { FormAlert } from '@/components/ui/FormAlert';
import { useMyAccount } from '@/hooks/useAccount';
import { useUpdateProfile } from '@/hooks/useAuthActions';
import { useAuthStore } from '@/stores/authStore';
import { formatDateTimeIst, formatInr } from '@/utils/format';
import { validateDisplayName } from '@/utils/validation';
import { describeError } from '@/utils/apiErrors';

/** The signed-in user's virtual balances and profile. */
export function AccountPage() {
  const user = useAuthStore((s) => s.user);
  if (!user) return null;

  return (
    <div className="mx-auto max-w-5xl space-y-4">
      <div>
        <h1 className="text-base font-semibold">Account</h1>
        <p className="text-fg-muted">Virtual funds only. Balances cannot be withdrawn or converted to real money.</p>
      </div>
      <div className="grid gap-4 md:grid-cols-2">
        {user.role === 'USER' ? <BalancePanel /> : <AdminNoticePanel />}
        <ProfilePanel key={user.id} />
      </div>
    </div>
  );
}

function BalancePanel() {
  const { data, isPending, error } = useMyAccount();
  return (
    <Panel title="Virtual balance">
      {isPending && (
        <div role="status" className="h-24 animate-pulse rounded-sm bg-surface-2">
          <span className="sr-only">Loading balance</span>
        </div>
      )}
      {error && <FormAlert message={describeError(error)} />}
      {data && (
        <>
          <p className="text-[11px] uppercase tracking-wider text-fg-muted">Available to trade</p>
          <p className="num mb-4 font-mono text-2xl font-semibold" data-testid="available-balance">
            {formatInr(data.availableBalance)}
          </p>
          <KeyValueList
            items={[
              { label: 'Reserved for open orders', value: formatInr(data.reservedBalance) },
              { label: 'Total cash', value: formatInr(data.totalBalance) },
              { label: 'Currency', value: data.currency },
            ]}
          />
        </>
      )}
    </Panel>
  );
}

function AdminNoticePanel() {
  return (
    <Panel title="Virtual balance">
      <p className="text-fg-muted">Administrator accounts monitor the platform and do not hold a trading account.</p>
    </Panel>
  );
}

function ProfilePanel() {
  const user = useAuthStore((s) => s.user)!;
  const update = useUpdateProfile();
  const [displayName, setDisplayName] = useState(user.displayName);
  const [fieldError, setFieldError] = useState<string>();

  const unchanged = displayName.trim() === user.displayName;

  function onSubmit(event: FormEvent) {
    event.preventDefault();
    const error = validateDisplayName(displayName);
    setFieldError(error);
    if (!error) update.mutate(displayName.trim());
  }

  return (
    <Panel title="Profile">
      <KeyValueList
        items={[
          { label: 'E-mail', value: user.email },
          { label: 'Role', value: user.role },
          { label: 'Member since', value: formatDateTimeIst(user.createdAt) },
        ]}
      />
      <form noValidate onSubmit={onSubmit} className="mt-4 space-y-3 border-t border-line pt-4">
        {update.isError && <FormAlert message={describeError(update.error)} />}
        <TextField
          label="Display name"
          value={displayName}
          onChange={(e) => {
            setDisplayName(e.target.value);
            update.reset();
          }}
          error={fieldError}
        />
        <div className="flex items-center gap-3">
          <Button type="submit" variant="secondary" loading={update.isPending} disabled={unchanged}>
            Save
          </Button>
          {update.isSuccess && unchanged && (
            <span role="status" className="inline-flex items-center gap-1 text-up">
              <Check className="h-4 w-4" aria-hidden /> Saved
            </span>
          )}
        </div>
      </form>
    </Panel>
  );
}
