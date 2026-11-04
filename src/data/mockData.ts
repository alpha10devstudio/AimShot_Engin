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
    iconBg: 'from-[#0F2B48] to-[#0A1A2E]',
    logoUrl: '/assets/images/games/carrom.png',
    packageName: 'com.miniclip.carrom'
  },
  {
    id: '8ball',
    name: '8 Ball Pool',
    features: 'Long Line • Accurate Angle • Safe',
    category: 'Billiards / Pool',
    verified: true,
    isActive: false,
    locked: true,
    iconBg: 'from-[#141E28] to-[#0D151F]',
    logoUrl: '/assets/images/games/eightball.png',
    packageName: 'com.miniclip.eightballpool'
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
