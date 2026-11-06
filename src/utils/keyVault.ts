import { CachedKeyVaultRecord, MembershipState } from '../types';

const VAULT_STORAGE_KEY = 'aimshot_key_vault';

/**
 * Loads all valid cached keys from local memory/storage.
 * Automatically purges any expired keys.
 * Returns keys sorted by latest used first (primary).
 */
export function getKeyVault(): CachedKeyVaultRecord[] {
  if (typeof window === 'undefined' || !window.localStorage) {
    return [];
  }

  const raw = localStorage.getItem(VAULT_STORAGE_KEY);
  if (!raw) return [];

  try {
    const list: CachedKeyVaultRecord[] = JSON.parse(raw);
    if (!Array.isArray(list)) return [];

    const now = Date.now();
    // Auto-purge expired keys
    const validKeys = list.filter(k => k.expiryTimestamp && k.expiryTimestamp > now);

    if (validKeys.length !== list.length) {
      localStorage.setItem(VAULT_STORAGE_KEY, JSON.stringify(validKeys));
    }

    // Sort by lastUsedAt descending
    return validKeys.sort((a, b) => b.lastUsedAt - a.lastUsedAt);
  } catch {
    return [];
  }
}

/**
 * Saves the latest active license key in the local vault with expiry and usage metadata.
 * Strictly enforces single latest-active-key precedence: whenever a new key is activated,
 * all older/prior keys in memory and storage are automatically deleted.
 * The app retains ONLY this latest key locally until its server-assigned expiration.
 */
export function saveKeyToVault(
  key: string,
  expiryTimestamp: number,
  planType = 'AimShot Pro',
  boundDeviceId = '9a7d3c8f1e2b4d5a'
): CachedKeyVaultRecord {
  const cleanKey = key.trim();
  const now = Date.now();

  const expiry = new Date(expiryTimestamp);
  const formattedExpiry = expiry.toLocaleDateString('en-US', {
    month: 'short',
    day: 'numeric',
    year: 'numeric'
  });

  const record: CachedKeyVaultRecord = {
    key: cleanKey,
    planType,
    startDate: 'Today',
    expiryDate: formattedExpiry,
    expiryTimestamp,
    boundDeviceId,
    lastUsedAt: now
  };

  // Strictly retain ONLY the latest active key, purging older/prior keys
  const vault = [record];
  localStorage.setItem(VAULT_STORAGE_KEY, JSON.stringify(vault));
  return record;
}

/**
 * Checks if a key exists in local vault and has not expired.
 */
export function findValidKeyInVault(key: string): CachedKeyVaultRecord | null {
  if (!key) return null;
  const clean = key.trim().toLowerCase();
  const vault = getKeyVault();
  const found = vault.find(k => k.key.toLowerCase() === clean);
  if (found && found.expiryTimestamp > Date.now()) {
    return found;
  }
  return null;
}

/**
 * Switches to an existing key in the vault and marks it as primary (latest used).
 */
export function switchActiveKey(key: string): CachedKeyVaultRecord | null {
  const record = findValidKeyInVault(key);
  if (!record) return null;

  record.lastUsedAt = Date.now();
  const vault = getKeyVault().filter(k => k.key.toLowerCase() !== key.trim().toLowerCase());
  vault.unshift(record);
  localStorage.setItem(VAULT_STORAGE_KEY, JSON.stringify(vault));
  return record;
}

/**
 * Authoritatively removes a revoked/expired key from the vault.
 */
export function removeKeyFromVault(key: string): void {
  if (!key) return;
  const clean = key.trim().toLowerCase();
  const vault = getKeyVault().filter(k => k.key.toLowerCase() !== clean);
  localStorage.setItem(VAULT_STORAGE_KEY, JSON.stringify(vault));
}

/**
 * Converts a CachedKeyVaultRecord into an active MembershipState.
 */
export function createMembershipFromRecord(record: CachedKeyVaultRecord): MembershipState {
  const remainingMillis = Math.max(0, record.expiryTimestamp - Date.now());
  const remainingDays = Math.ceil(remainingMillis / (24 * 60 * 60 * 1000));

  return {
    isLoggedIn: true,
    status: 'active',
    planType: record.planType,
    startDate: record.startDate,
    expiryDate: record.expiryDate,
    daysRemaining: remainingDays,
    expiryTimestamp: record.expiryTimestamp,
    featuresUnlocked: true,
    boundDeviceId: record.boundDeviceId,
    key: record.key
  };
}
