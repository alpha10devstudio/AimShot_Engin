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
  },
  {
    id: 'pubg',
    name: 'PUBG Mobile',
    features: 'Aim Assist • Crosshair • FPS Boost',
    category: 'Battle Royale',
    verified: false,
    isActive: false,
    locked: true,
    iconBg: 'from-[#3A220B] to-[#1F1206]',
    logoUrl: '/assets/images/games/pubg.png',
    packageName: 'com.tencent.ig'
  },
  {
    id: 'freefire',
    name: 'Free Fire',
    features: 'Smooth Aim • No Recoil • Custom HUD',
    category: 'Battle Royale',
    verified: false,
    isActive: false,
    locked: true,
    iconBg: 'from-[#38160E] to-[#1E0C07]',
    logoUrl: '/assets/images/games/freefire.png',
    packageName: 'com.dts.freefireth'
  },
  {
    id: 'bgmi',
    name: 'BGMI',
    features: 'Performance • Aim Tools • Anti-Lag',
    category: 'Battle Royale',
    verified: false,
    isActive: false,
    locked: true,
    iconBg: 'from-[#2E240D] to-[#181307]',
    logoUrl: '/assets/images/games/bgmi.png',
    packageName: 'com.pubg.imobile'
  },
  {
    id: 'codm',
    name: 'Call of Duty',
    features: 'Precision • Sensitivity • No Recoil',
    category: 'FPS / Tactical',
    verified: false,
    isActive: false,
    locked: true,
    iconBg: 'from-[#102737] to-[#091721]',
    logoUrl: '/assets/images/games/codm.png',
    packageName: 'com.activision.callofduty.shooter'
  },
  {
    id: 'valorant',
    name: 'Valorant',
    features: 'Crosshair • FPS • Game Sense',
    category: 'Tactical Shooter',
    verified: false,
    isActive: false,
    locked: true,
    iconBg: 'from-[#3B1520] to-[#200B11]',
    logoUrl: '/assets/images/games/valorant.png',
    packageName: 'com.riotgames.valorant'
  },
  {
    id: 'gtav',
    name: 'GTA V',
    features: 'Aim Bot • Visuals • Performance',
    category: 'Action Adventure',
    verified: false,
    isActive: false,
    locked: true,
    iconBg: 'from-[#132E22] to-[#0A1A13]',
    logoUrl: '/assets/images/games/gtav.png',
    packageName: 'com.rockstargames.gtav'
  },
  {
    id: 'efootball',
    name: 'eFootball',
    features: 'Better Control • Aim Assist • Smooth',
    category: 'Sports',
    verified: false,
    isActive: false,
    locked: true,
    iconBg: 'from-[#122244] to-[#091326]',
    logoUrl: '/assets/images/games/efootball.png',
    packageName: 'jp.konami.pesam'
  },
  {
    id: 'minecraft',
    name: 'Minecraft',
    features: 'FPS Boost • Performance • Stability',
    category: 'Sandbox',
    verified: false,
    isActive: false,
    locked: true,
    iconBg: 'from-[#1F2E14] to-[#10180B]',
    logoUrl: '/assets/images/games/minecraft.png',
    packageName: 'com.mojang.minecraftpe'
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
