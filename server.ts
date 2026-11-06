import express from 'express';
import type { Request, Response } from 'express';
import { createServer as createViteServer } from 'vite';
import path from 'path';
import fs from 'fs';
import crypto from 'crypto';
import { fileURLToPath } from 'url';

const __filename = fileURLToPath(import.meta.url);
const __dirname = path.dirname(__filename);

export interface NotificationRecord {
  id: string;
  title: string;
  message: string;
  category: 'announcement' | 'update' | 'feature' | 'maintenance';
  timestamp: string;
  isRead: boolean;
  minVersion?: string;
  actionUrl?: string;
  priority: 'normal' | 'high' | 'urgent';
  isActive: boolean;
}

// In-memory notifications store initialized with realistic official announcements
let notificationsDatabase: NotificationRecord[] = [
  {
    id: 'notif-101',
    title: 'AimShot Engine v1.0.0 Global Release',
    message: 'Welcome to AimShot Engine! Enhanced auto-aim guidelines, millimeter precision physics, and 120 FPS frame pacing are active.',
    category: 'announcement',
    timestamp: 'Today, 10:24 AM',
    isRead: false,
    minVersion: '1.0.0',
    priority: 'high',
    isActive: true
  },
  {
    id: 'notif-102',
    title: 'Carrom Disc Pool Guideline Calibration',
    message: 'New millimeter precision laser physics engine calibrated for smooth bank shots and double rebound angles.',
    category: 'update',
    timestamp: 'Yesterday',
    isRead: false,
    minVersion: '1.0.0',
    priority: 'normal',
    isActive: true
  },
  {
    id: 'notif-103',
    title: 'Anti-Ban Shield v2.4 Active',
    message: 'Kernel touch hook isolation verified against latest mobile anti-cheat updates. 100% undetectable safe overlay kernel.',
    category: 'feature',
    timestamp: '2 days ago',
    isRead: false,
    minVersion: '1.0.0',
    priority: 'high',
    isActive: true
  },
  {
    id: 'notif-104',
    title: 'Cloud Server Optimization Notice',
    message: 'Routine cloud verification server maintenance scheduled for 03:00 UTC. In-app offline license verification remains 100% functional.',
    category: 'maintenance',
    timestamp: '3 days ago',
    isRead: false,
    minVersion: '1.0.0',
    priority: 'normal',
    isActive: true
  }
];

async function startServer() {
  const app = express();
  const PORT = 3000;

  app.use(express.json());

  // APK Binary direct serving route
  app.get(['/AimShot-Engine-v1.0.0.apk', '/download/apk'], (req: Request, res: Response) => {
    const rootApk = path.resolve(__dirname, 'AimShot-Engine-v1.0.0.apk');
    const publicApk = path.resolve(__dirname, 'public/AimShot-Engine-v1.0.0.apk');
    const debugApk = path.resolve(__dirname, 'app/build/outputs/apk/debug/app-debug.apk');

    const filePath = fs.existsSync(rootApk)
      ? rootApk
      : fs.existsSync(publicApk)
      ? publicApk
      : fs.existsSync(debugApk)
      ? debugApk
      : null;

    if (filePath) {
      const stat = fs.statSync(filePath);
      res.setHeader('Content-Type', 'application/vnd.android.package-archive');
      res.setHeader('Content-Disposition', 'attachment; filename="AimShot-Engine-v1.0.0.apk"');
      res.setHeader('Content-Length', stat.size);
      res.setHeader('Cache-Control', 'no-cache');
      fs.createReadStream(filePath).pipe(res);
    } else {
      res.status(404).json({ error: 'APK file not found on server' });
    }
  });

  // Health check endpoint
  app.get('/api/health', (req: Request, res: Response) => {
    res.json({
      status: 'ok',
      engine: 'AimShot Engine',
      version: '1.0.0',
      timestamp: new Date().toISOString()
    });
  });

  // License Activation Proxy for web preview & authoritative verification
  let cachedAntiBotCookie: string | null = null;

  function solveAntiBot(html: string): string | null {
    try {
      const matches = [...html.matchAll(/toNumbers\(\s*\"([0-9a-fA-F]{32})\"\s*\)/g)].map(m => m[1]);
      if (matches.length < 3) return null;
      const key = Buffer.from(matches[0], 'hex');
      const iv = Buffer.from(matches[1], 'hex');
      const cipherText = Buffer.from(matches[2], 'hex');
      const decipher = crypto.createDecipheriv('aes-128-cbc', key, iv);
      decipher.setAutoPadding(false);
      const decrypted = Buffer.concat([decipher.update(cipherText), decipher.final()]);
      return '__test=' + decrypted.toString('hex');
    } catch {
      return null;
    }
  }

  app.post('/api/verify-license', async (req: Request, res: Response) => {
    try {
      const { license_key, device_id, device_model, app_version } = req.body || {};
      if (!license_key || !String(license_key).trim()) {
        return res.status(400).json({
          valid: false,
          status: 'invalid',
          message: 'Verification key cannot be empty.'
        });
      }

      const postBody = JSON.stringify({
        license_key: String(license_key).trim(),
        device_id: device_id || '9a7d3c8f1e2b4d5a',
        device_model: device_model || 'Samsung Galaxy S23 Ultra',
        app_version: app_version || '2.4.0'
      });

      const makeRequest = async (cookie?: string | null) => {
        const headers: Record<string, string> = {
          'Content-Type': 'application/json',
          'Accept': 'application/json',
          'User-Agent': 'Mozilla/5.0 (Linux; Android 14; Mobile) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Mobile Safari/537.36'
        };
        if (cookie) {
          headers['Cookie'] = cookie;
        }
        return await fetch('https://alpha10.freedev.app/api/index.php?action=activate_license', {
          method: 'POST',
          headers,
          body: postBody
        });
      };

      let upstreamRes = await makeRequest(cachedAntiBotCookie);
      let rawText = await upstreamRes.text();

      // Check if anti-bot challenge was returned
      if (rawText.includes('aes.js') || rawText.includes('slowAES') || rawText.includes('toNumbers(')) {
        const solved = solveAntiBot(rawText);
        if (solved) {
          cachedAntiBotCookie = solved;
          upstreamRes = await makeRequest(solved);
          rawText = await upstreamRes.text();
        }
      }

      let parsed: any = null;
      try {
        let cleanText = rawText.replace(/^\uFEFF/, '').trim();
        const first = cleanText.indexOf('{');
        const last = cleanText.lastIndexOf('}');
        if (first >= 0 && last > first) {
          cleanText = cleanText.substring(first, last + 1);
        }
        parsed = JSON.parse(cleanText);
      } catch {
        parsed = null;
      }

      if (!parsed) {
        return res.status(502).json({
          valid: false,
          status: 'server_error',
          message: 'Unexpected response from licensing server. Please try again.'
        });
      }

      return res.status(upstreamRes.status).json(parsed);
    } catch (err: any) {
      return res.status(502).json({
        valid: false,
        status: 'network_error',
        message: 'Unable to connect to security licensing server. Please check connection.'
      });
    }
  });

  // Server-Side Token Broker Endpoint (optional Node/Cloud Run alternative to PHP launcher-auth.php)
  app.post('/api/launcher-auth', async (req: Request, res: Response) => {
    const secretAppKey = (process.env.ALPHA10_SECRET_APP_KEY || '').trim();
    if (!secretAppKey) {
      return res.status(503).json({
        success: false,
        error: 'Server configuration incomplete: ALPHA10_SECRET_APP_KEY is not set on server.'
      });
    }

    const pkg = String(req.body?.launcher_package || req.body?.package_name || '').trim();
    const deviceId = String(req.body?.device_id || '').trim();
    if (pkg !== 'com.alpha10.aimshot.launcher' && pkg !== 'com.alpha10.aimshot') {
      return res.status(403).json({
        success: false,
        error: 'Unauthorized client application package.'
      });
    }
    if (!deviceId || deviceId.length < 6) {
      return res.status(400).json({
        success: false,
        error: 'Missing or invalid device identifier.'
      });
    }

    try {
      const upstreamRes = await fetch('https://alpha10.freedev.app/api/auth.php', {
        method: 'POST',
        headers: {
          'X-App-Key': secretAppKey,
          'Accept': 'application/json',
          'User-Agent': 'Alpha10-Launcher-Broker/1.0'
        },
        redirect: 'manual'
      });

      const rawText = await upstreamRes.text();
      let parsed: Record<string, unknown> | null = null;
      try {
        parsed = JSON.parse(rawText.replace(/^\uFEFF/, '').trim());
      } catch {
        parsed = null;
      }

      if (!upstreamRes.ok || !parsed) {
        const upstreamErr =
          (parsed && typeof parsed.error === 'string' && parsed.error) ||
          (parsed && typeof parsed.message === 'string' && parsed.message) ||
          `Upstream Alpha10 auth.php rejected request (HTTP ${upstreamRes.status})`;
        return res.status(upstreamRes.status >= 400 ? upstreamRes.status : 502).json({
          success: false,
          error: upstreamErr
        });
      }

      const dataObj = (parsed.data && typeof parsed.data === 'object' ? parsed.data : {}) as Record<string, unknown>;
      const token = [
        parsed.token,
        parsed.temporary_token,
        parsed.temp_token,
        parsed.access_token,
        parsed.download_token,
        parsed.auth_token,
        dataObj.token,
        dataObj.temporary_token,
        dataObj.temp_token,
        dataObj.access_token
      ].find(v => typeof v === 'string' && v.trim().length > 0) as string | undefined;

      if (!token) {
        return res.status(502).json({
          success: false,
          error: (typeof parsed.error === 'string' && parsed.error) || 'Upstream auth.php did not return a temporary token.'
        });
      }

      const expectedSha256 = (process.env.ALPHA10_MAIN_APK_SHA256 || '').trim();
      const trustedCertSha256 =
        (process.env.ALPHA10_MAIN_CERT_SHA256 || '').trim() ||
        '1C:8D:56:83:62:EE:D9:04:02:36:43:0D:AC:8D:1F:BE:30:06:0E:9F:FD:2D:F4:01:AB:69:04:E7:94:86:A5:A2';

      return res.json({
        success: true,
        token: token.trim(),
        temporary_token: token.trim(),
        expires_in: Number(parsed.expires_in) > 0 ? Number(parsed.expires_in) : 60,
        expected_sha256: /^[0-9a-f]{64}$/i.test(expectedSha256) ? expectedSha256.toLowerCase() : null,
        trusted_cert_sha256: trustedCertSha256,
        target_package: 'com.alpha10.aimshot',
        download_endpoint: 'https://alpha10.freedev.app/api/aimshot-download.php'
      });
    } catch (err) {
      return res.status(502).json({
        success: false,
        error: 'Unable to reach upstream Alpha10 authentication server.'
      });
    }
  });

  // Public/App Notification list endpoint
  app.get('/api/notifications', (req: Request, res: Response) => {
    const category = req.query.category as string | undefined;
    let list = notificationsDatabase.filter(n => n.isActive);
    if (category && category !== 'all') {
      list = list.filter(n => n.category.toLowerCase() === category.toLowerCase());
    }
    res.json(list);
  });

  // Mark single notification as read
  app.post('/api/notifications/read', (req: Request, res: Response) => {
    const { id } = req.body;
    if (!id) {
      return res.status(400).json({ error: 'Missing notification id' });
    }
    const item = notificationsDatabase.find(n => n.id === id);
    if (item) {
      item.isRead = true;
    }
    const unreadCount = notificationsDatabase.filter(n => n.isActive && !n.isRead).length;
    res.json({ success: true, id, unreadCount });
  });

  // Mark all notifications as read
  app.post('/api/notifications/read-all', (req: Request, res: Response) => {
    notificationsDatabase.forEach(n => {
      n.isRead = true;
    });
    res.json({ success: true, unreadCount: 0 });
  });

  // Website Admin Endpoint: Publish new notification to mobile app
  app.post('/api/admin/notifications', (req: Request, res: Response) => {
    const { title, message, category, priority, minVersion, actionUrl } = req.body;
    if (!title || !message) {
      return res.status(400).json({ error: 'Title and message are required' });
    }

    const newNotification: NotificationRecord = {
      id: `notif-${Date.now()}`,
      title: title.trim(),
      message: message.trim(),
      category: category || 'announcement',
      timestamp: 'Just now',
      isRead: false,
      priority: priority || 'normal',
      minVersion: minVersion || '1.0.0',
      actionUrl: actionUrl || '',
      isActive: true
    };

    // Prepend to list so newest is first
    notificationsDatabase.unshift(newNotification);

    res.status(201).json({
      success: true,
      message: 'Notification published successfully to AimShot Engine mobile app',
      notification: newNotification
    });
  });

  // Website Admin Endpoint: Toggle / Update notification
  app.put('/api/admin/notifications/:id', (req: Request, res: Response) => {
    const { id } = req.params;
    const { title, message, category, priority, isActive, isRead } = req.body;

    const notif = notificationsDatabase.find(n => n.id === id);
    if (!notif) {
      return res.status(404).json({ error: 'Notification not found' });
    }

    if (title !== undefined) notif.title = title;
    if (message !== undefined) notif.message = message;
    if (category !== undefined) notif.category = category;
    if (priority !== undefined) notif.priority = priority;
    if (isActive !== undefined) notif.isActive = isActive;
    if (isRead !== undefined) notif.isRead = isRead;

    res.json({ success: true, notification: notif });
  });

  // Website Admin Endpoint: Delete notification
  app.delete('/api/admin/notifications/:id', (req: Request, res: Response) => {
    const { id } = req.params;
    const initialLen = notificationsDatabase.length;
    notificationsDatabase = notificationsDatabase.filter(n => n.id !== id);

    if (notificationsDatabase.length === initialLen) {
      return res.status(404).json({ error: 'Notification not found' });
    }

    res.json({ success: true, message: 'Notification deleted' });
  });

  // Vite development middleware or static production serving
  const isProd = process.env.NODE_ENV === 'production';
  if (!isProd) {
    const vite = await createViteServer({
      server: { middlewareMode: true },
      appType: 'spa'
    });
    app.use(vite.middlewares);
  } else {
    app.use(express.static(path.resolve(__dirname, 'dist')));
    app.get('*', (req: Request, res: Response) => {
      res.sendFile(path.resolve(__dirname, 'dist/index.html'));
    });
  }

  app.listen(PORT, '0.0.0.0', () => {
    console.log(`[AimShot Engine] Full-stack Server listening on http://0.0.0.0:${PORT}`);
  });
}

startServer().catch(err => {
  console.error('[AimShot Engine] Failed to start server:', err);
  process.exit(1);
});
