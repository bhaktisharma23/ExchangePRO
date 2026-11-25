/** Mirrors com.exchangepro.system.SystemStatusResponse on the backend. */
export type ComponentState = 'UP' | 'DOWN';

export interface DatabaseStatus {
  state: ComponentState;
  productVersion?: string;
  schemaVersion?: string;
}

export interface MarketDataStatus {
  mode: 'SIMULATION' | 'REAL';
  label: string;
  realTime: boolean;
}

export interface SystemStatus {
  application: string;
  version: string;
  activeProfiles: string[];
  serverTime: string;
  database: DatabaseStatus;
  marketData: MarketDataStatus;
}
