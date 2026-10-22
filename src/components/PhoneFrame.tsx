import React from 'react';
import { Wifi, Battery, Home, Crown, Settings } from 'lucide-react';
import { BottomTab, ScreenId, AppTheme } from '../types';
import { Translations } from '../data/translations';

export type DevicePreset = 'responsive' | 'small' | 'standard' | 'large';

interface PhoneFrameProps {
  currentTab: BottomTab;
  currentScreen: ScreenId;
  devicePreset?: DevicePreset;
  theme?: AppTheme;
  t?: Translations;
  onTabChange: (tab: BottomTab) => void;
  children: React.ReactNode;
}

export const PhoneFrame: React.FC<PhoneFrameProps> = ({
  currentTab,
  currentScreen,
  devicePreset = 'standard',
  theme = 'dark',
  t,
  onTabChange,
  children
}) => {
  const isSplash = currentScreen === 'splash';
  const isDark = theme === 'dark';

  // Responsive device styling presets
  const getFrameDimensions = () => {
    switch (devicePreset) {
      case 'responsive':
        return 'w-full h-full max-w-md min-h-[600px] md:rounded-[40px] md:border-4 md:border-[#1C2B42]';
      case 'small':
        return 'w-[360px] h-[740px] rounded-[44px] border-4 border-[#1C2B42]';
      case 'large':
        return 'w-[430px] h-[880px] rounded-[52px] border-4 border-[#1C2B42]';
      case 'standard':
      default:
        return 'w-full max-w-[390px] h-[800px] rounded-[48px] border-4 border-[#1C2B42]';
    }
  };

  const isFullResponsive = devicePreset === 'responsive';

  const isHomeActive = currentTab === 'home';
  const isPremiumActive = currentTab === 'premium';
  const isSettingsActive = currentTab === 'settings';

  const homeLabel = t ? t.navHome : 'Home';
  const premiumLabel = t ? t.navPremium : 'Premium';
  const settingsLabel = t ? t.navSettings : 'Settings';

  return (
    <div
      className={`relative mx-auto bg-[#000000] p-0 md:p-2.5 shadow-[0_0_50px_rgba(0,0,0,0.8),0_0_25px_rgba(57,185,255,0.18)] flex flex-col overflow-hidden ring-1 ring-[#39B9FF]/30 transition-all duration-300 ${getFrameDimensions()}`}
    >
      {/* Device Inner Screen Container */}
      <div
        className={`relative w-full h-full flex flex-col overflow-hidden border transition-colors duration-200 ${
          isDark ? 'bg-[#0B0F17] border-[#111B2A]' : 'bg-[#F1F5F9] border-[#CBD5E1]'
        } ${isFullResponsive ? 'rounded-none md:rounded-[34px]' : 'rounded-[38px]'}`}
      >
        {/* Android Top Status Bar (Hidden on splash) */}
        {!isSplash && (
          <div
            className={`w-full h-8 px-4 sm:px-6 flex items-center justify-between text-[11px] font-semibold select-none shrink-0 z-40 border-b pt-[env(safe-area-inset-top)] ${
              isDark
                ? 'bg-[#0B0F17] text-[#F4F7FC] border-[#111B2A]/50'
                : 'bg-white text-[#0F172A] border-[#E2E8F0]'
            }`}
          >
            {/* Clock */}
            <span className="font-mono text-xs">10:24</span>

            {/* Front Camera Dot Notch */}
            <div className="w-3.5 h-3.5 rounded-full bg-black/90 border border-[#1C2B42] flex items-center justify-center">
              <div className="w-1.5 h-1.5 rounded-full bg-[#0E1624]" />
            </div>

            {/* Status Icons: Signal, WiFi, Battery 78% */}
            <div className="flex items-center gap-1.5">
              <span className="text-[10px] text-[#49DCA0] font-bold">5G</span>
              <Wifi className="w-3.5 h-3.5" />
              <div className="flex items-center gap-1 font-mono text-[10px]">
                <Battery className="w-4 h-4" />
                <span>78%</span>
              </div>
            </div>
          </div>
        )}

        {/* Dynamic Screen Viewport with smooth scrollbar */}
        <div
          className={`relative flex-1 w-full overflow-hidden transition-colors duration-200 ${
            isDark ? 'bg-[#0B0F17]' : 'bg-[#F1F5F9]'
          }`}
        >
          {children}
        </div>

        {/* Persistent Native Bottom Navigation Bar */}
        {!isSplash && (
          <div
            className={`w-full h-16 border-t flex items-center justify-around px-2 sm:px-4 shrink-0 select-none z-40 pb-[env(safe-area-inset-bottom)] transition-colors duration-200 ${
              isDark
                ? 'bg-[#090D14] border-[#162234]'
                : 'bg-white border-[#CBD5E1] shadow-lg'
            }`}
          >
            {/* Nav Item 1: Home (Requirement 1: Active indicator dot appears properly) */}
            <button
              onClick={() => onTabChange('home')}
              className={`flex-1 flex flex-col items-center justify-center py-1 rounded-xl transition-all cursor-pointer ${
                isHomeActive
                  ? isDark
                    ? 'text-[#39B9FF]'
                    : 'text-[#0284C7]'
                  : isDark
                  ? 'text-[#8D9BB0] hover:text-[#CCD6E0]'
                  : 'text-[#64748B] hover:text-[#0F172A]'
              }`}
            >
              <div className="relative">
                <Home className="w-5 h-5" />
                {isHomeActive && (
                  <div className="absolute inset-0 bg-[#39B9FF]/25 blur-sm rounded-full" />
                )}
              </div>
              <span className="text-[10px] font-bold mt-0.5 tracking-wider">
                {homeLabel}
              </span>
              {/* Active Indicator Dot */}
              <div
                className={`w-1.5 h-1.5 rounded-full mt-0.5 transition-all duration-200 ${
                  isHomeActive
                    ? 'bg-[#39B9FF] shadow-[0_0_8px_#39B9FF] opacity-100 scale-100'
                    : 'opacity-0 scale-0'
                }`}
              />
            </button>

            {/* Nav Item 2: Premium (Requirement 1: Active indicator dot appears properly) */}
            <button
              onClick={() => onTabChange('premium')}
              className={`flex-1 flex flex-col items-center justify-center py-1 rounded-xl transition-all cursor-pointer ${
                isPremiumActive
                  ? isDark
                    ? 'text-[#39B9FF]'
                    : 'text-[#0284C7]'
                  : isDark
                  ? 'text-[#8D9BB0] hover:text-[#CCD6E0]'
                  : 'text-[#64748B] hover:text-[#0F172A]'
              }`}
            >
              <div className="relative">
                <Crown
                  className={`w-5 h-5 ${
                    isPremiumActive ? 'fill-[#39B9FF]/20 text-[#39B9FF]' : ''
                  }`}
                />
                {isPremiumActive && (
                  <div className="absolute inset-0 bg-[#39B9FF]/25 blur-sm rounded-full" />
                )}
              </div>
              <span className="text-[10px] font-bold mt-0.5 tracking-wider">
                {premiumLabel}
              </span>
              {/* Active Indicator Dot */}
              <div
                className={`w-1.5 h-1.5 rounded-full mt-0.5 transition-all duration-200 ${
                  isPremiumActive
                    ? 'bg-[#39B9FF] shadow-[0_0_8px_#39B9FF] opacity-100 scale-100'
                    : 'opacity-0 scale-0'
                }`}
              />
            </button>

            {/* Nav Item 3: Settings (Requirement 1: Active indicator dot appears properly) */}
            <button
              onClick={() => onTabChange('settings')}
              className={`flex-1 flex flex-col items-center justify-center py-1 rounded-xl transition-all cursor-pointer ${
                isSettingsActive
                  ? isDark
                    ? 'text-[#39B9FF]'
                    : 'text-[#0284C7]'
                  : isDark
                  ? 'text-[#8D9BB0] hover:text-[#CCD6E0]'
                  : 'text-[#64748B] hover:text-[#0F172A]'
              }`}
            >
              <div className="relative">
                <Settings className="w-5 h-5" />
                {isSettingsActive && (
                  <div className="absolute inset-0 bg-[#39B9FF]/25 blur-sm rounded-full" />
                )}
              </div>
              <span className="text-[10px] font-bold mt-0.5 tracking-wider">
                {settingsLabel}
              </span>
              {/* Active Indicator Dot */}
              <div
                className={`w-1.5 h-1.5 rounded-full mt-0.5 transition-all duration-200 ${
                  isSettingsActive
                    ? 'bg-[#39B9FF] shadow-[0_0_8px_#39B9FF] opacity-100 scale-100'
                    : 'opacity-0 scale-0'
                }`}
              />
            </button>
          </div>
        )}

        {/* Android System Home Navigation Pill Bar */}
        <div
          className={`w-full h-3 flex items-center justify-center shrink-0 ${
            isDark ? 'bg-[#090D14]' : 'bg-white'
          }`}
        >
          <div
            className={`w-28 sm:w-32 h-1 rounded-full ${
              isDark ? 'bg-[#8D9BB0]/40' : 'bg-[#94A3B8]/40'
            }`}
          />
        </div>
      </div>
    </div>
  );
};
