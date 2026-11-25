/**
 * Mirrors com.exchangepro.account.AccountResponse.
 * Amounts are decimal STRINGS (e.g. "100000.0000"): never convert them to JavaScript numbers for arithmetic.
 */
export interface Account {
  accountId: string;
  currency: 'INR';
  availableBalance: string;
  reservedBalance: string;
  totalBalance: string;
}
