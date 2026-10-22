import tailwindcss from '@tailwindcss/vite';
import react from '@vitejs/plugin-react';
import path from 'path';
import fs from 'fs';
import { defineConfig, Plugin } from 'vite';

// Custom plugin to reliably serve Android APK binary with correct MIME type and attachment headers
function serveApkPlugin(): Plugin {
  return {
    name: 'serve-apk-binary',
    configureServer(server) {
      server.middlewares.use((req, res, next) => {
        if (req.url && (req.url.endsWith('.apk') || req.url.includes('.apk?'))) {
          const rootApk = path.resolve(__dirname, 'AimShot-Engine-v1.0.0.apk');
          const publicApk = path.resolve(__dirname, 'public/AimShot-Engine-v1.0.0.apk');
          const filePath = fs.existsSync(rootApk) ? rootApk : fs.existsSync(publicApk) ? publicApk : null;

          if (filePath) {
            const stat = fs.statSync(filePath);
            res.setHeader('Content-Type', 'application/vnd.android.package-archive');
            res.setHeader('Content-Disposition', 'attachment; filename="AimShot-Engine-v1.0.0.apk"');
            res.setHeader('Content-Length', stat.size);
            res.setHeader('Cache-Control', 'no-cache');
            const readStream = fs.createReadStream(filePath);
            readStream.pipe(res);
            return;
          }
        }
        next();
      });
    },
  };
}

export default defineConfig(() => {
  return {
    plugins: [react(), tailwindcss(), serveApkPlugin()],
    resolve: {
      alias: {
        '@': path.resolve(__dirname, '.'),
      },
    },
    server: {
      host: '0.0.0.0',
      port: 3000,
      allowedHosts: true as const,
      // HMR is disabled in AI Studio via DISABLE_HMR env var.
      // Do not modify—file watching is disabled to prevent flickering during agent edits.
      hmr: process.env.DISABLE_HMR !== 'true',
      // Disable file watching when DISABLE_HMR is true to save CPU during agent edits.
      watch: process.env.DISABLE_HMR === 'true' ? null : {},
    },
  };
});
