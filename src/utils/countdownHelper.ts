import { Language } from '../data/translations';

const BN_DIGITS = ['০', '১', '২', '৩', '৪', '৫', '৬', '৭', '৮', '৯'];
const HI_DIGITS = ['०', '१', '२', '३', '४', '५', '६', '७', '८', '९'];

/**
 * Localizes Western ASCII numeric characters (0-9) to the targeted language script.
 */
export function toLocalizedDigits(val: number | string, lang: Language): string {
  const str = String(val);
  if (lang === 'bn') {
    return str.replace(/[0-9]/g, (d) => BN_DIGITS[parseInt(d, 10)] ?? d);
  }
  // Standard Arabic numerals or Hindi numerals
  return str;
}

export interface CountdownParts {
  days: number;
  hours: number;
  minutes: number;
  seconds: number;
  totalSeconds: number;
  isExpired: boolean;
  formattedString: string;
}

/**
 * Calculates live remaining countdown time given an expiration timestamp.
 * Returns exact formatted template: `[X] দিন [HH]:[MM]:[SS]` (e.g. `০ দিন ১২:৪৫:৩০সে` or `18 Days 14:22:15s`)
 */
export function calculateLiveCountdown(
  expiryTimestamp: number | undefined,
  lang: Language = 'en',
  nowTimestamp: number = Date.now()
): CountdownParts {
  if (!expiryTimestamp || expiryTimestamp <= 0) {
    const zeroDays = toLocalizedDigits('0', lang);
    const zeroTime = `${toLocalizedDigits('00', lang)}:${toLocalizedDigits('00', lang)}:${toLocalizedDigits('00', lang)}`;
    const dayLabel = lang === 'bn' ? 'দিন' : lang === 'hi' ? 'दिन' : 'Days';
    const secSuffix = lang === 'bn' ? 'সে' : lang === 'hi' ? 'से' : 's';

    return {
      days: 0,
      hours: 0,
      minutes: 0,
      seconds: 0,
      totalSeconds: 0,
      isExpired: true,
      formattedString: `${zeroDays} ${dayLabel} ${zeroTime}${secSuffix}`
    };
  }

  const diffMs = Math.max(0, expiryTimestamp - nowTimestamp);
  const totalSeconds = Math.floor(diffMs / 1000);
  const isExpired = totalSeconds <= 0;

  const days = Math.floor(totalSeconds / 86400);
  const hours = Math.floor((totalSeconds % 86400) / 3600);
  const minutes = Math.floor((totalSeconds % 3600) / 60);
  const seconds = Math.floor(totalSeconds % 60);

  const pad = (n: number) => String(n).padStart(2, '0');

  const dayStr = toLocalizedDigits(days, lang);
  const hourStr = toLocalizedDigits(pad(hours), lang);
  const minStr = toLocalizedDigits(pad(minutes), lang);
  const secStr = toLocalizedDigits(pad(seconds), lang);

  const dayLabel = lang === 'bn' ? 'দিন' : lang === 'hi' ? 'दिन' : 'Days';
  const secSuffix = lang === 'bn' ? 'সে' : lang === 'hi' ? 'সে' : 's';

  // Format: `[X] দিন [HH]:[MM]:[SS]` (e.g. `০ দিন ১২:৪৫:৩০সে` or `18 Days 14:22:15s`)
  const formattedString = `${dayStr} ${dayLabel} ${hourStr}:${minStr}:${secStr}${secSuffix}`;

  return {
    days,
    hours,
    minutes,
    seconds,
    totalSeconds,
    isExpired,
    formattedString
  };
}
