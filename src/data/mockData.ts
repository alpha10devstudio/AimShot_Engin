import { Game, SellerContact, DeviceSpecs } from '../types';

export const SUPPORTED_GAMES: Game[] = [
  {
    id: 'pubg',
    name: 'PUBG Mobile',
    features: 'Aim Assist • Crosshair • FPS Boost',
    category: 'Battle Royale',
    verified: true,
    iconBg: 'from-amber-600 to-yellow-800'
  },
  {
    id: 'freefire',
    name: 'Free Fire',
    features: 'Smooth Aim • No Recoil • Custom HUD',
    category: 'Battle Royale',
    verified: true,
    iconBg: 'from-orange-600 to-red-800'
  },
  {
    id: 'bgmi',
    name: 'BGMI',
    features: 'Performance • Aim Tools • Anti-Lag',
    category: 'Battle Royale',
    verified: true,
    iconBg: 'from-yellow-500 to-amber-700'
  },
  {
    id: 'codm',
    name: 'Call of Duty',
    features: 'Precision • Sensitivity • No Recoil',
    category: 'FPS / Tactical',
    verified: true,
    iconBg: 'from-cyan-700 to-blue-900'
  },
  {
    id: 'valorant',
    name: 'Valorant',
    features: 'Crosshair • FPS • Game Sense',
    category: 'Tactical Shooter',
    verified: true,
    iconBg: 'from-rose-600 to-red-900'
  },
  {
    id: 'gtav',
    name: 'GTA V',
    features: 'Aim Bot • Visuals • Performance',
    category: 'Action Adventure',
    verified: true,
    iconBg: 'from-emerald-700 to-teal-900'
  },
  {
    id: 'efootball',
    name: 'eFootball',
    features: 'Better Control • Aim Assist • Smooth',
    category: 'Sports',
    verified: true,
    iconBg: 'from-blue-600 to-indigo-900'
  },
  {
    id: 'minecraft',
    name: 'Minecraft',
    features: 'FPS Boost • Performance • Stability',
    category: 'Sandbox',
    verified: true,
    iconBg: 'from-lime-700 to-emerald-900'
  }
];

export const OFFICIAL_SELLER: SellerContact = {
  name: 'AimShot Shop',
  online: true,
  verified: true,
  whatsapp: '+880 1700 111222',
  telegram: '@aimshot_support',
  email: 'aimshot@support.com',
  trustRating: '100% Trusted'
};

export const DEFAULT_DEVICE_SPECS: DeviceSpecs = {
  deviceId: 'AS-7F3E-9C4D-2A8B-6E1F',
  androidVersion: '13 (Tiramisu)',
  model: 'Samsung Galaxy A12',
  brand: 'samsung',
  totalRam: '3.0 GB',
  screenResolution: '720 x 1600',
  cpu: 'Exynos 850'
};
