export type ScreenId = 
  | 'splash'
  | 'home'
  | 'premium'
  | 'supported_games'
  | 'seller_contact'
  | 'device_info'
  | 'settings';

export type BottomTab = 'home' | 'premium' | 'settings';

export interface MembershipState {
  isLoggedIn: boolean;
  status: 'active' | 'free' | 'expired' | 'pending';
  planType: string;
  startDate: string;
  expiryDate: string;
  daysRemaining: number;
  featuresUnlocked: boolean;
  boundDeviceId: string;
}

export interface Game {
  id: string;
  name: string;
  features: string;
  category: string;
  verified: boolean;
  iconBg: string;
  badgeText?: string;
}

export interface SellerContact {
  name: string;
  online: boolean;
  verified: boolean;
  whatsapp: string;
  telegram: string;
  email: string;
  trustRating: string;
}

export interface DeviceSpecs {
  deviceId: string;
  androidVersion: string;
  model: string;
  brand: string;
  totalRam: string;
  screenResolution: string;
  cpu: string;
}
