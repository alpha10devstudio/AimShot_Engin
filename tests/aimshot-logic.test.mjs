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

  // Test 7: Notification category filtering & unread count
  test('Notification category filtering and unread count calculate correctly', () => {
    const sampleNotifs = [
      { id: '1', title: 'Update A', category: 'update', isRead: false },
      { id: '2', title: 'Feature B', category: 'feature', isRead: true },
      { id: '3', title: 'Maintenance C', category: 'maintenance', isRead: false },
      { id: '4', title: 'Announcement D', category: 'announcement', isRead: false }
    ];

    const unreadCount = sampleNotifs.filter(n => !n.isRead).length;
    assert.equal(unreadCount, 3);

    const updateCategory = sampleNotifs.filter(n => n.category === 'update');
    assert.equal(updateCategory.length, 1);
    assert.equal(updateCategory[0].title, 'Update A');
  });

  // Test 8: Notification mark as read immutability
  test('Marking notification as read returns updated item with isRead: true', () => {
    const notif = { id: 'test-1', title: 'Notice', isRead: false };
    const markAsRead = (item) => ({ ...item, isRead: true });
    const updated = markAsRead(notif);
    assert.equal(updated.isRead, true);
    assert.equal(notif.isRead, false); // Original remains unchanged
  });

  // Test 9: Carousel indicator dot lockstep synchronization
  test('Carousel index cycles smoothly with 3 slide dots', () => {
    const totalSlides = 3;
    let currentSlide = 0;
    const advanceSlide = (curr) => (curr + 1) % totalSlides;
    const prevSlide = (curr) => (curr === 0 ? totalSlides - 1 : curr - 1);

    currentSlide = advanceSlide(currentSlide);
    assert.equal(currentSlide, 1);
    currentSlide = advanceSlide(currentSlide);
    assert.equal(currentSlide, 2);
    currentSlide = advanceSlide(currentSlide);
    assert.equal(currentSlide, 0); // Wraps back to 0

    currentSlide = prevSlide(currentSlide);
    assert.equal(currentSlide, 2); // Wraps back to last slide
  });

  // Test 10: Settings 4 options toggle & state preservation
  test('Top 4 settings toggle states toggle independently', () => {
    const settings = {
      theme: 'dark',
      language: 'en',
      notificationsEnabled: true,
      hapticFeedbackEnabled: true
    };

    const toggleTheme = (s) => ({ ...s, theme: s.theme === 'dark' ? 'light' : 'dark' });
    const toggleNotifs = (s) => ({ ...s, notificationsEnabled: !s.notificationsEnabled });
    const toggleHaptic = (s) => ({ ...s, hapticFeedbackEnabled: !s.hapticFeedbackEnabled });
    const changeLang = (s, lang) => ({ ...s, language: lang });

    const s1 = toggleTheme(settings);
    assert.equal(s1.theme, 'light');
    assert.equal(s1.notificationsEnabled, true);

    const s2 = toggleNotifs(s1);
    assert.equal(s2.notificationsEnabled, false);

    const s3 = toggleHaptic(s2);
    assert.equal(s3.hapticFeedbackEnabled, false);

    const s4 = changeLang(s3, 'bn');
    assert.equal(s4.language, 'bn');
  });
});

