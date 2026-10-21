import { Game, SellerContact, DeviceSpecs } from '../types';

export const SUPPORTED_GAMES: Game[] = [
  {
    id: 'carrom',
    name: 'Carrom Disc Pool',
    features: 'Auto Aim • Guideline • Smooth Control',
    category: 'Tabletop / Pool',
    verified: true,
    isActive: true,
    locked: false,
    iconBg: 'from-sky-500 to-blue-700'
  },
  {
    id: '8ball',
    name: '8 Ball Pool',
    features: 'Long Line • Accurate Angle • Safe',
    category: 'Billiards / Pool',
    verified: true,
    isActive: false,
    locked: true,
    iconBg: 'from-slate-700 to-slate-900'
  },
  {
    id: 'pubg',
    name: 'PUBG Mobile',
    features: 'Aim Assist • Crosshair • FPS Boost',
    category: 'Battle Royale',
    verified: false,
    isActive: false,
    locked: true,
    iconBg: 'from-amber-600 to-yellow-800'
  },
  {
    id: 'freefire',
    name: 'Free Fire',
    features: 'Smooth Aim • No Recoil • Custom HUD',
    category: 'Battle Royale',
    verified: false,
    isActive: false,
    locked: true,
    iconBg: 'from-orange-600 to-red-800'
  },
  {
    id: 'bgmi',
    name: 'BGMI',
    features: 'Performance • Aim Tools • Anti-Lag',
    category: 'Battle Royale',
    verified: false,
    isActive: false,
    locked: true,
    iconBg: 'from-yellow-500 to-amber-700'
  },
  {
    id: 'codm',
    name: 'Call of Duty',
    features: 'Precision • Sensitivity • No Recoil',
    category: 'FPS / Tactical',
    verified: false,
    isActive: false,
    locked: true,
    iconBg: 'from-cyan-700 to-blue-900'
  },
  {
    id: 'valorant',
    name: 'Valorant',
    features: 'Crosshair • FPS • Game Sense',
    category: 'Tactical Shooter',
    verified: false,
    isActive: false,
    locked: true,
    iconBg: 'from-rose-600 to-red-900'
  },
  {
    id: 'gtav',
    name: 'GTA V',
    features: 'Aim Bot • Visuals • Performance',
    category: 'Action Adventure',
    verified: false,
    isActive: false,
    locked: true,
    iconBg: 'from-emerald-700 to-teal-900'
  },
  {
    id: 'efootball',
    name: 'eFootball',
    features: 'Better Control • Aim Assist • Smooth',
    category: 'Sports',
    verified: false,
    isActive: false,
    locked: true,
    iconBg: 'from-blue-600 to-indigo-900'
  },
  {
    id: 'minecraft',
    name: 'Minecraft',
    features: 'FPS Boost • Performance • Stability',
    category: 'Sandbox',
    verified: false,
    isActive: false,
    locked: true,
    iconBg: 'from-lime-600 to-green-800'
  }
];

export const OFFICIAL_SELLER: SellerContact = {
  name: 'AimShot Shop',
  online: true,
  verified: true,
  whatsapp: '+880 1700 111222',
  telegram: '@aimshot_support',
  email: 'aimshot@support.com',
  trustRating: '100% Trusted & Verified Seller'
};

export const DEVICE_SPECIFICATIONS: DeviceSpecs = {
  deviceId: 'AIM-8839-4491-BC99',
  androidVersion: '14.0 (API 34)',
  model: 'Pixel 8 Pro (Tactical Edition)',
  brand: 'Google',
  totalRam: '12 GB LPDDR5X',
  screenResolution: '2992 × 1344 @ 120Hz',
  cpu: 'Google Tensor G3 (Titan M2 Security)'
};

export const DEFAULT_DEVICE_SPECS = DEVICE_SPECIFICATIONS;
