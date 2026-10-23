export type ScreenId = 
  | 'splash'
  | 'home'
  | 'premium'
  | 'supported_games'
  | 'seller_contact'
  | 'device_info'
  | 'settings'
  | 'notifications';

export type BottomTab = 'home' | 'premium' | 'settings';

export type AppTheme = 'dark' | 'light';

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
  logoUrl: string;
  packageName?: string;
  badgeText?: string;
  isActive?: boolean;
  locked?: boolean;
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

export interface AppNotification {
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
