import React, { useState } from 'react';
import {
  Smartphone,
  Code,
  Image as ImageIcon,
  Sparkles,
  KeyRound,
  CheckCircle2,
  Download,
  Maximize2
} from 'lucide-react';
import { PhoneFrame, DevicePreset } from './components/PhoneFrame';
import { SplashScreen } from './components/screens/SplashScreen';
import { HomeScreen } from './components/screens/HomeScreen';
import { VerificationModal } from './components/screens/VerificationModal';
import { PremiumScreen } from './components/screens/PremiumScreen';
import { SupportedGamesScreen } from './components/screens/SupportedGamesScreen';
import { SellerContactScreen } from './components/screens/SellerContactScreen';
import { DeviceInfoScreen } from './components/screens/DeviceInfoScreen';
import { SettingsScreen } from './components/screens/SettingsScreen';
import { CodeExplorer } from './components/CodeExplorer';
import { ScreenId, BottomTab, MembershipState, Game } from './types';

export default function App() {
  const [currentScreen, setCurrentScreen] = useState<ScreenId>('home');
  const [currentTab, setCurrentTab] = useState<BottomTab>('home');
  const [isLoginModalOpen, setIsLoginModalOpen] = useState(false);
  const [viewMode, setViewMode] = useState<'phone' | 'split' | 'code' | 'mockup'>('phone');
  const [devicePreset, setDevicePreset] = useState<DevicePreset>('standard');
  const [toastMessage, setToastMessage] = useState<string | null>(null);

  // Membership state starting as authentic Free / Logged Out
  const [membership, setMembership] = useState<MembershipState>({
    isLoggedIn: false,
    status: 'free',
    planType: 'Free / Not Activated',
    startDate: '--',
    expiryDate: '--',
    daysRemaining: 0,
    featuresUnlocked: false,
    boundDeviceId: 'AS-7F3E-9C4D-2A8B-6E1F'
  });

  const showToast = (msg: string) => {
    setToastMessage(msg);
    setTimeout(() => setToastMessage(null), 3000);
  };

  const handleTabChange = (tab: BottomTab) => {
    setCurrentTab(tab);
    if (tab === 'home') {
      setCurrentScreen('home');
    } else if (tab === 'premium') {
      setCurrentScreen('premium');
    } else if (tab === 'settings') {
      setCurrentScreen('settings');
    }
  };

  const handleLoginSuccess = (key: string) => {
    setMembership({
      isLoggedIn: true,
      status: 'active',
      planType: 'Monthly Premium',
      startDate: '07 Apr 2025',
      expiryDate: '25 Apr 2025',
      daysRemaining: 18,
      featuresUnlocked: true,
      boundDeviceId: 'AS-7F3E-9C4D-2A8B-6E1F'
    });
    showToast('Key Verified! Premium active: 18 Days remaining.');
  };

  const handleLogout = () => {
    setMembership({
      isLoggedIn: false,
      status: 'free',
      planType: 'Free / Not Activated',
      startDate: '--',
      expiryDate: '--',
      daysRemaining: 0,
      featuresUnlocked: false,
      boundDeviceId: 'AS-7F3E-9C4D-2A8B-6E1F'
    });
    setCurrentScreen('home');
    setCurrentTab('home');
    showToast('Session cleared. Logged out.');
  };

  const handleSelectGame = (game: Game) => {
    showToast(`${game.name} profile active: ${game.features}`);
  };

  const handleDownloadApk = async () => {
    showToast('Preparing AimShot Engine APK download...');
    try {
      const response = await fetch('/AimShot-Engine-v1.0.0.apk');
      if (!response.ok) throw new Error('Failed to fetch APK binary');
      const blob = await response.blob();
      const apkBlob = new Blob([blob], { type: 'application/vnd.android.package-archive' });
      const downloadUrl = window.URL.createObjectURL(apkBlob);
      const tempLink = document.createElement('a');
      tempLink.href = downloadUrl;
      tempLink.download = 'AimShot-Engine-v1.0.0.apk';
      document.body.appendChild(tempLink);
      tempLink.click();
      document.body.removeChild(tempLink);
      setTimeout(() => window.URL.revokeObjectURL(downloadUrl), 8000);
      showToast('APK Downloaded! Open with Android Package Installer.');
    } catch (e) {
      window.location.href = '/AimShot-Engine-v1.0.0.apk';
    }
  };

  return (
    <div className="min-h-screen bg-[#070B12] text-[#F4F7FC] flex flex-col font-sans selection:bg-[#39B9FF]/30">
      {/* Top Application Control & Navigation Bar */}
      <header className="h-16 px-3 sm:px-6 md:px-8 bg-[#0B0F17] border-b border-[#1C2B42] flex items-center justify-between shrink-0 z-50">
        <div className="flex items-center gap-2.5">
          <div className="flex items-baseline gap-1.5 font-bold text-base sm:text-lg tracking-wider">
            <span className="text-white">AimShot</span>
            <span className="text-[#39B9FF] drop-shadow-[0_0_8px_rgba(57,185,255,0.6)]">ENGINE</span>
          </div>

          <span className="hidden lg:inline-block px-2.5 py-0.5 rounded-full bg-[#111B2A] border border-[#39B9FF]/30 text-[11px] font-mono text-[#39B9FF]">
            com.alpha10.aimshot
          </span>
        </div>

        {/* View Switcher Tabs */}
        <div className="flex items-center gap-1 bg-[#111B2A] p-1 rounded-xl border border-[#1C2B42]">
          <button
            onClick={() => setViewMode('phone')}
            className={`px-2.5 sm:px-3 py-1.5 rounded-lg text-xs font-bold flex items-center gap-1.5 transition-all cursor-pointer ${
              viewMode === 'phone'
                ? 'bg-[#39B9FF] text-[#0B0F17] shadow-[0_0_10px_rgba(57,185,255,0.3)]'
                : 'text-[#8D9BB0] hover:text-white'
            }`}
          >
            <Smartphone className="w-3.5 h-3.5" />
            <span className="hidden sm:inline">Mobile</span>
          </button>

          <button
            onClick={() => setViewMode('split')}
            className={`px-2.5 sm:px-3 py-1.5 rounded-lg text-xs font-bold flex items-center gap-1.5 transition-all cursor-pointer ${
              viewMode === 'split'
                ? 'bg-[#39B9FF] text-[#0B0F17] shadow-[0_0_10px_rgba(57,185,255,0.3)]'
                : 'text-[#8D9BB0] hover:text-white'
            }`}
          >
            <Sparkles className="w-3.5 h-3.5" />
            <span className="hidden sm:inline">Dual Studio</span>
          </button>

          <button
            onClick={() => setViewMode('code')}
            className={`px-2.5 sm:px-3 py-1.5 rounded-lg text-xs font-bold flex items-center gap-1.5 transition-all cursor-pointer ${
              viewMode === 'code'
                ? 'bg-[#39B9FF] text-[#0B0F17] shadow-[0_0_10px_rgba(57,185,255,0.3)]'
                : 'text-[#8D9BB0] hover:text-white'
            }`}
          >
            <Code className="w-3.5 h-3.5" />
            <span className="hidden sm:inline">Android Code</span>
          </button>

          <button
            onClick={() => setViewMode('mockup')}
            className={`px-2.5 sm:px-3 py-1.5 rounded-lg text-xs font-bold flex items-center gap-1.5 transition-all cursor-pointer ${
              viewMode === 'mockup'
                ? 'bg-[#39B9FF] text-[#0B0F17] shadow-[0_0_10px_rgba(57,185,255,0.3)]'
                : 'text-[#8D9BB0] hover:text-white'
            }`}
          >
            <ImageIcon className="w-3.5 h-3.5" />
            <span className="hidden md:inline">Mockup</span>
          </button>
        </div>

        {/* Action Group: Download APK & Test Key */}
        <div className="flex items-center gap-2">
          {/* Prominent Direct APK Download Button */}
          <button
            onClick={handleDownloadApk}
            className="px-3 py-1.5 rounded-xl bg-gradient-to-r from-[#49DCA0] to-[#20C97F] hover:opacity-90 text-[#0B0F17] text-xs font-extrabold flex items-center gap-1.5 transition-all shadow-[0_0_12px_rgba(73,220,160,0.35)] cursor-pointer active:scale-95"
            title="Download built APK package"
          >
            <Download className="w-3.5 h-3.5" />
            <span>Download APK</span>
          </button>

          {/* Quick Test Key */}
          {membership.isLoggedIn ? (
            <button
              onClick={handleLogout}
              className="px-2.5 py-1.5 rounded-xl bg-red-950/40 border border-red-500/40 text-red-400 hover:bg-red-900/30 text-xs font-bold transition-all cursor-pointer"
            >
              Reset
            </button>
          ) : (
            <button
              onClick={() => handleLoginSuccess('AIM-PRO-2025-X9')}
              className="hidden sm:flex px-2.5 py-1.5 rounded-xl bg-[#39B9FF]/15 border border-[#39B9FF]/40 text-[#39B9FF] hover:bg-[#39B9FF]/25 text-xs font-bold items-center gap-1.5 transition-all cursor-pointer"
            >
              <KeyRound className="w-3.5 h-3.5" />
              <span>Test 18-Day Key</span>
            </button>
          )}
        </div>
      </header>

      {/* Screen Navigator & Responsive Device Sizer Toolbar */}
      <div className="bg-[#090D14] border-b border-[#162234] px-3 sm:px-6 py-2 flex flex-wrap items-center justify-between text-xs overflow-x-auto select-none gap-2 shrink-0">
        {/* Screen Jump Links */}
        <div className="flex items-center gap-1.5 shrink-0 overflow-x-auto">
          <span className="text-[#55657E] font-bold text-[10px] uppercase mr-1">Screens:</span>
          {(
            [
              { id: 'splash', label: '1. Splash' },
              { id: 'home', label: '2. Home' },
              { id: 'login_dialog', label: '3. Key Login' },
              { id: 'premium', label: '4. Premium' },
              { id: 'supported_games', label: '5. Games' },
              { id: 'seller_contact', label: '6. Seller' },
              { id: 'device_info', label: '7. Device' },
              { id: 'settings', label: '8. Settings' }
            ] as const
          ).map((item) => (
            <button
              key={item.id}
              onClick={() => {
                if (item.id === 'login_dialog') {
                  setCurrentScreen('home');
                  setCurrentTab('home');
                  setIsLoginModalOpen(true);
                } else if (item.id === 'premium') {
                  if (!membership.isLoggedIn) {
                    handleLoginSuccess('AIM-PRO-2025-X9');
                  }
                  setCurrentScreen('premium');
                  setCurrentTab('premium');
                } else {
                  setCurrentScreen(item.id as ScreenId);
                  if (item.id === 'home') setCurrentTab('home');
                  if (item.id === 'settings') setCurrentTab('settings');
                }
              }}
              className={`px-2 py-0.5 rounded-lg text-[10px] font-medium transition-colors shrink-0 cursor-pointer ${
                currentScreen === item.id || (item.id === 'login_dialog' && isLoginModalOpen)
                  ? 'bg-[#14283D] text-[#39B9FF] border border-[#39B9FF]/40'
                  : 'text-[#8D9BB0] hover:bg-[#111B2A] hover:text-white'
              }`}
            >
              {item.label}
            </button>
          ))}
        </div>

        {/* Mobile Device Preset Sizer */}
        <div className="flex items-center gap-2 shrink-0">
          <span className="text-[#55657E] font-bold text-[10px] uppercase">Device Size:</span>
          <div className="flex items-center gap-1 bg-[#111B2A] p-0.5 rounded-lg border border-[#1C2B42]">
            <button
              onClick={() => {
                setDevicePreset('responsive');
                showToast('Auto Responsive Mode (Adapts to any screen)');
              }}
              className={`px-2 py-0.5 rounded text-[10px] font-bold transition-all ${
                devicePreset === 'responsive'
                  ? 'bg-[#39B9FF] text-[#0B0F17]'
                  : 'text-[#8D9BB0] hover:text-white'
              }`}
              title="Fluid responsive for all screens"
            >
              Auto Responsive
            </button>
            <button
              onClick={() => {
                setDevicePreset('small');
                showToast('Compact Mobile: 360px × 740px (Galaxy A12)');
              }}
              className={`px-2 py-0.5 rounded text-[10px] font-bold transition-all ${
                devicePreset === 'small'
                  ? 'bg-[#39B9FF] text-[#0B0F17]'
                  : 'text-[#8D9BB0] hover:text-white'
              }`}
            >
              360px
            </button>
            <button
              onClick={() => {
                setDevicePreset('standard');
                showToast('Standard Mobile: 390px × 800px');
              }}
              className={`px-2 py-0.5 rounded text-[10px] font-bold transition-all ${
                devicePreset === 'standard'
                  ? 'bg-[#39B9FF] text-[#0B0F17]'
                  : 'text-[#8D9BB0] hover:text-white'
              }`}
            >
              390px
            </button>
            <button
              onClick={() => {
                setDevicePreset('large');
                showToast('Large Mobile: 430px × 880px (Pro Max)');
              }}
              className={`px-2 py-0.5 rounded text-[10px] font-bold transition-all ${
                devicePreset === 'large'
                  ? 'bg-[#39B9FF] text-[#0B0F17]'
                  : 'text-[#8D9BB0] hover:text-white'
              }`}
            >
              430px
            </button>
          </div>
        </div>
      </div>

      {/* Main Content Viewport */}
      <main className="flex-1 overflow-auto p-2 sm:p-4 md:p-6 flex items-center justify-center relative">
        {/* VIEW 1: Phone Emulator */}
        {viewMode === 'phone' && (
          <div className="w-full h-full flex items-center justify-center my-auto py-1">
            <PhoneFrame
              currentTab={currentTab}
              currentScreen={currentScreen}
              devicePreset={devicePreset}
              onTabChange={handleTabChange}
            >
              {currentScreen === 'splash' && (
                <SplashScreen onFinish={() => setCurrentScreen('home')} />
              )}

              {currentScreen === 'home' && (
                <HomeScreen
                  membership={membership}
                  onOpenLoginModal={() => setIsLoginModalOpen(true)}
                  onViewAllGames={() => setCurrentScreen('supported_games')}
                  onOpenNotifications={() => setCurrentScreen('seller_contact')}
                  onNavigateToPremium={() => {
                    setCurrentTab('premium');
                    setCurrentScreen('premium');
                  }}
                  onSelectGame={handleSelectGame}
                />
              )}

              {currentScreen === 'premium' && (
                <PremiumScreen
                  membership={membership}
                  onOpenSettings={() => {
                    setCurrentTab('settings');
                    setCurrentScreen('settings');
                  }}
                  onUpgradeRenew={() => setCurrentScreen('seller_contact')}
                />
              )}

              {currentScreen === 'supported_games' && (
                <SupportedGamesScreen
                  onBack={() => setCurrentScreen('home')}
                  onSelectGame={handleSelectGame}
                />
              )}

              {currentScreen === 'seller_contact' && (
                <SellerContactScreen
                  onBack={() => setCurrentScreen(currentTab === 'settings' ? 'settings' : 'home')}
                  onShowToast={showToast}
                />
              )}

              {currentScreen === 'device_info' && (
                <DeviceInfoScreen
                  onBack={() => setCurrentScreen('settings')}
                  onShowToast={showToast}
                />
              )}

              {currentScreen === 'settings' && (
                <SettingsScreen
                  onBack={() => {
                    setCurrentTab('home');
                    setCurrentScreen('home');
                  }}
                  onLogout={handleLogout}
                  onOpenDeviceInfo={() => setCurrentScreen('device_info')}
                  onOpenSellerContact={() => setCurrentScreen('seller_contact')}
                  onDownloadApk={handleDownloadApk}
                  onShowToast={showToast}
                />
              )}

              {/* Verification Dialog Modal overlay */}
              <VerificationModal
                isOpen={isLoginModalOpen}
                onClose={() => setIsLoginModalOpen(false)}
                onSuccess={handleLoginSuccess}
              />
            </PhoneFrame>
          </div>
        )}

        {/* VIEW 2: Dual Studio */}
        {viewMode === 'split' && (
          <div className="w-full h-full max-w-7xl flex flex-col lg:flex-row gap-6 items-center justify-center">
            <div className="shrink-0 scale-95 origin-top">
              <PhoneFrame
                currentTab={currentTab}
                currentScreen={currentScreen}
                devicePreset={devicePreset}
                onTabChange={handleTabChange}
              >
                {currentScreen === 'splash' && (
                  <SplashScreen onFinish={() => setCurrentScreen('home')} />
                )}

                {currentScreen === 'home' && (
                  <HomeScreen
                    membership={membership}
                    onOpenLoginModal={() => setIsLoginModalOpen(true)}
                    onViewAllGames={() => setCurrentScreen('supported_games')}
                    onOpenNotifications={() => setCurrentScreen('seller_contact')}
                    onNavigateToPremium={() => {
                      setCurrentTab('premium');
                      setCurrentScreen('premium');
                    }}
                    onSelectGame={handleSelectGame}
                  />
                )}

                {currentScreen === 'premium' && (
                  <PremiumScreen
                    membership={membership}
                    onOpenSettings={() => {
                      setCurrentTab('settings');
                      setCurrentScreen('settings');
                    }}
                    onUpgradeRenew={() => setCurrentScreen('seller_contact')}
                  />
                )}

                {currentScreen === 'supported_games' && (
                  <SupportedGamesScreen
                    onBack={() => setCurrentScreen('home')}
                    onSelectGame={handleSelectGame}
                  />
                )}

                {currentScreen === 'seller_contact' && (
                  <SellerContactScreen
                    onBack={() => setCurrentScreen(currentTab === 'settings' ? 'settings' : 'home')}
                    onShowToast={showToast}
                  />
                )}

                {currentScreen === 'device_info' && (
                  <DeviceInfoScreen
                    onBack={() => setCurrentScreen('settings')}
                    onShowToast={showToast}
                  />
                )}

                {currentScreen === 'settings' && (
                  <SettingsScreen
                    onBack={() => {
                      setCurrentTab('home');
                      setCurrentScreen('home');
                    }}
                    onLogout={handleLogout}
                    onOpenDeviceInfo={() => setCurrentScreen('device_info')}
                    onOpenSellerContact={() => setCurrentScreen('seller_contact')}
                    onDownloadApk={handleDownloadApk}
                    onShowToast={showToast}
                  />
                )}

                <VerificationModal
                  isOpen={isLoginModalOpen}
                  onClose={() => setIsLoginModalOpen(false)}
                  onSuccess={handleLoginSuccess}
                />
              </PhoneFrame>
            </div>

            <div className="flex-1 w-full h-[800px]">
              <CodeExplorer />
            </div>
          </div>
        )}

        {/* VIEW 3: Dedicated Android Code Studio & Zip Downloader */}
        {viewMode === 'code' && (
          <div className="w-full h-full max-w-6xl">
            <CodeExplorer />
          </div>
        )}

        {/* VIEW 4: Side-by-Side Reference Mockup */}
        {viewMode === 'mockup' && (
          <div className="w-full max-w-6xl flex flex-col items-center">
            <div className="w-full bg-[#111B2A] rounded-2xl p-6 border border-[#1C2B42] mb-6">
              <h2 className="text-lg font-bold text-white mb-2">Original Reference Mockup (8 Screens)</h2>
              <p className="text-xs text-[#8D9BB0] mb-4">
                Comparison against the uploaded graphic specifications: Splash, Home, Key Login, Premium Active 18 Days, Supported Games, Seller Contact, Device Information, and Settings.
              </p>
              <div className="rounded-xl overflow-hidden border border-[#1C2B42] bg-black">
                <img
                  src="/assets/aistudio/file_000000003c7482119665d69e139b72e9_85ca5b44.png"
                  alt="AimShot Engine Mockup"
                  className="w-full h-auto object-contain"
                />
              </div>
            </div>
          </div>
        )}

        {/* Toast Notification */}
        {toastMessage && (
          <div className="fixed bottom-6 right-6 z-50 bg-[#111B2A] border border-[#39B9FF] text-white px-4 py-2.5 rounded-xl shadow-[0_0_20px_rgba(57,185,255,0.3)] flex items-center gap-2.5 animate-bounce text-xs font-semibold">
            <CheckCircle2 className="w-4 h-4 text-[#49DCA0]" />
            <span>{toastMessage}</span>
          </div>
        )}
      </main>
    </div>
  );
}
