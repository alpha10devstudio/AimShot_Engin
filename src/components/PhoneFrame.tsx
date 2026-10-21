import React from 'react';
import { Wifi, Battery, Home, Crown, Settings } from 'lucide-react';
import { BottomTab, ScreenId } from '../types';

export type DevicePreset = 'responsive' | 'small' | 'standard' | 'large';

interface PhoneFrameProps {
  currentTab: BottomTab;
  currentScreen: ScreenId;
  devicePreset?: DevicePreset;
  onTabChange: (tab: BottomTab) => void;
  children: React.ReactNode;
}

export const PhoneFrame: React.FC<PhoneFrameProps> = ({
  currentTab,
  currentScreen,
  devicePreset = 'standard',
  onTabChange,
  children
}) => {
  const isSplash = currentScreen === 'splash';

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

  return (
    <div
      className={`relative mx-auto bg-[#000000] p-0 md:p-2.5 shadow-[0_0_50px_rgba(0,0,0,0.8),0_0_25px_rgba(57,185,255,0.18)] flex flex-col overflow-hidden ring-1 ring-[#39B9FF]/30 transition-all duration-300 ${getFrameDimensions()}`}
    >
      {/* Device Inner Screen Container */}
      <div
        className={`relative w-full h-full bg-[#0B0F17] flex flex-col overflow-hidden border border-[#111B2A] ${
          isFullResponsive ? 'rounded-none md:rounded-[34px]' : 'rounded-[38px]'
        }`}
      >
        {/* Android Top Status Bar (Hidden on splash) */}
        {!isSplash && (
          <div className="w-full h-8 px-4 sm:px-6 bg-[#0B0F17] flex items-center justify-between text-[11px] font-semibold text-[#F4F7FC] select-none shrink-0 z-40 border-b border-[#111B2A]/50 pt-[env(safe-area-inset-top)]">
            {/* Clock */}
            <span className="font-mono text-xs">10:24</span>

            {/* Front Camera Dot Notch */}
            <div className="w-3.5 h-3.5 rounded-full bg-black/80 border border-[#1C2B42] flex items-center justify-center">
              <div className="w-1.5 h-1.5 rounded-full bg-[#0E1624]" />
            </div>

            {/* Status Icons: Signal, WiFi, Battery 78% */}
            <div className="flex items-center gap-1.5">
              <span className="text-[10px] text-[#49DCA0] font-bold">5G</span>
              <Wifi className="w-3.5 h-3.5 text-[#F4F7FC]" />
              <div className="flex items-center gap-1 font-mono text-[10px]">
                <Battery className="w-4 h-4 text-[#F4F7FC]" />
                <span>78%</span>
              </div>
            </div>
          </div>
        )}

        {/* Dynamic Screen Viewport with smooth scrollbar */}
        <div className="relative flex-1 w-full overflow-hidden bg-[#0B0F17]">
          {children}
        </div>

        {/* Persistent Native Bottom Navigation Bar */}
        {!isSplash && (
          <div className="w-full h-16 bg-[#090D14] border-t border-[#162234] flex items-center justify-around px-2 sm:px-4 shrink-0 select-none z-40 pb-[env(safe-area-inset-bottom)]">
            {/* Nav Item 1: Home */}
            <button
              onClick={() => onTabChange('home')}
              className={`flex-1 flex flex-col items-center justify-center py-1 rounded-xl transition-all cursor-pointer ${
                currentTab === 'home' && currentScreen === 'home'
                  ? 'text-[#39B9FF]'
                  : 'text-[#8D9BB0] hover:text-[#CCD6E0]'
              }`}
            >
              <div className="relative">
                <Home className="w-5 h-5" />
                {currentTab === 'home' && currentScreen === 'home' && (
                  <div className="absolute inset-0 bg-[#39B9FF]/25 blur-sm rounded-full" />
                )}
              </div>
              <span className="text-[10px] font-bold mt-0.5 tracking-wider">Home</span>
              {currentTab === 'home' && currentScreen === 'home' && (
                <div className="w-1.5 h-1.5 rounded-full bg-[#39B9FF] shadow-[0_0_8px_#39B9FF] mt-0.5" />
              )}
            </button>

            {/* Nav Item 2: Premium */}
            <button
              onClick={() => onTabChange('premium')}
              className={`flex-1 flex flex-col items-center justify-center py-1 rounded-xl transition-all cursor-pointer ${
                currentTab === 'premium' && currentScreen === 'premium'
                  ? 'text-[#39B9FF]'
                  : 'text-[#8D9BB0] hover:text-[#CCD6E0]'
              }`}
            >
              <div className="relative">
                <Crown
                  className={`w-5 h-5 ${
                    currentTab === 'premium' && currentScreen === 'premium'
                      ? 'fill-[#39B9FF]/20 text-[#39B9FF]'
                      : 'text-[#8D9BB0]'
                  }`}
                />
                {currentTab === 'premium' && currentScreen === 'premium' && (
                  <div className="absolute inset-0 bg-[#39B9FF]/25 blur-sm rounded-full" />
                )}
              </div>
              <span className="text-[10px] font-bold mt-0.5 tracking-wider">Premium</span>
              {currentTab === 'premium' && currentScreen === 'premium' && (
                <div className="w-1.5 h-1.5 rounded-full bg-[#39B9FF] shadow-[0_0_8px_#39B9FF] mt-0.5" />
              )}
            </button>

            {/* Nav Item 3: Settings */}
            <button
              onClick={() => onTabChange('settings')}
              className={`flex-1 flex flex-col items-center justify-center py-1 rounded-xl transition-all cursor-pointer ${
                currentTab === 'settings' && currentScreen === 'settings'
                  ? 'text-[#39B9FF]'
                  : 'text-[#8D9BB0] hover:text-[#CCD6E0]'
              }`}
            >
              <div className="relative">
                <Settings className="w-5 h-5" />
                {currentTab === 'settings' && currentScreen === 'settings' && (
                  <div className="absolute inset-0 bg-[#39B9FF]/25 blur-sm rounded-full" />
                )}
              </div>
              <span className="text-[10px] font-bold mt-0.5 tracking-wider">Settings</span>
              {currentTab === 'settings' && currentScreen === 'settings' && (
                <div className="w-1.5 h-1.5 rounded-full bg-[#39B9FF] shadow-[0_0_8px_#39B9FF] mt-0.5" />
              )}
            </button>
          </div>
        )}

        {/* Android System Home Navigation Pill Bar */}
        <div className="w-full h-3 bg-[#090D14] flex items-center justify-center shrink-0">
          <div className="w-28 sm:w-32 h-1 bg-[#8D9BB0]/40 rounded-full" />
        </div>
      </div>
    </div>
  );
};
