import express, { Request, Response } from 'express';
import { createServer as createViteServer } from 'vite';
import path from 'path';
import fs from 'fs';
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
