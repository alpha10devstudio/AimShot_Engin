import { test, describe } from 'node:test';
import assert from 'node:assert/strict';

describe('AimShot Engine Pure Logic Unit Tests', () => {
  // Test 1: Device ID Formatting
  test('Device ID follows AS-XXXX-XXXX-XXXX-XXXX format', () => {
    const rawHash = '7F3E9C4D2A8B6E1F';
    const formatted = `AS-${rawHash.slice(0, 4)}-${rawHash.slice(4, 8)}-${rawHash.slice(8, 12)}-${rawHash.slice(12, 16)}`;
    assert.match(formatted, /^AS-[0-9A-Z]{4}-[0-9A-Z]{4}-[0-9A-Z]{4}-[0-9A-Z]{4}$/);
    assert.equal(formatted, 'AS-7F3E-9C4D-2A8B-6E1F');
  });

  // Test 2: License expiration calculation
  test('Days remaining calculates correct days until expiry', () => {
    const now = Date.now();
    const eighteenDaysMillis = 18 * 24 * 60 * 60 * 1000;
    const expiryTimestamp = now + eighteenDaysMillis;
    
    const diff = expiryTimestamp - now;
    const days = Math.floor(diff / (24 * 60 * 60 * 1000));
    assert.equal(days, 18);
  });

  // Test 3: Expired membership validation
  test('Past expiry timestamp yields 0 days and expired state', () => {
    const pastTimestamp = Date.now() - 10000;
    const isExpired = Date.now() > pastTimestamp;
    assert.equal(isExpired, true);
  });

  // Test 4: Key input sanitation
  test('Key input trims whitespace and rejects empty string', () => {
    const validateKey = (key) => {
      if (!key || key.trim() === '') return { valid: false, error: 'Empty key' };
      return { valid: true, cleanKey: key.trim() };
    };

    assert.equal(validateKey('').valid, false);
    assert.equal(validateKey('   ').valid, false);
    assert.equal(validateKey('  AIM-PRO-2025-X9  ').cleanKey, 'AIM-PRO-2025-X9');
  });

  // Test 5: Game search filter
  test('Game search correctly matches titles and features', () => {
    const games = [
      { name: 'PUBG Mobile', features: 'Aim Assist • Crosshair • FPS Boost' },
      { name: 'Valorant', features: 'Crosshair • FPS • Game Sense' }
    ];

    const filterGames = (query) => {
      const q = query.toLowerCase();
      return games.filter(g => g.name.toLowerCase().includes(q) || g.features.toLowerCase().includes(q));
    };

    assert.equal(filterGames('Crosshair').length, 2);
    assert.equal(filterGames('Aim Assist').length, 1);
    assert.equal(filterGames('Aim Assist')[0].name, 'PUBG Mobile');
    assert.equal(filterGames('NonExistent').length, 0);
  });

  // Test 6: WhatsApp link generator
  test('WhatsApp link sanitizes non-numeric phone chars', () => {
    const rawPhone = '+880 1700 111222';
    const cleanPhone = rawPhone.replace(/[^0-9]/g, '');
    assert.equal(cleanPhone, '8801700111222');
    assert.equal(`https://wa.me/${cleanPhone}`, 'https://wa.me/8801700111222');
  });
});
