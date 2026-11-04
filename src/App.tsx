import React, { useState, useEffect, useCallback } from 'react';
import {
  Smartphone,
  Code,
  Image as ImageIcon,
  Sparkles,
  KeyRound,
  CheckCircle2,
  Download,
  AlertCircle,
  Play,
  X,
  Megaphone,
  Bell
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
import { NotificationsScreen } from './components/screens/NotificationsScreen';
import { AdminNotificationModal } from './components/AdminNotificationModal';
import { CodeExplorer } from './components/CodeExplorer';
import { ScreenId, BottomTab, MembershipState, Game, AppTheme, AppNotification } from './types';
import { SUPPORTED_GAMES } from './data/mockData';
import { Language, TRANSLATIONS } from './data/translations';

export default function App() {
  const [currentScreen, setCurrentScreen] = useState<ScreenId>('home');
  const [currentTab, setCurrentTab] = useState<BottomTab>('home');
  const [isLoginModalOpen, setIsLoginModalOpen] = useState(false);
  const [viewMode, setViewMode] = useState<'phone' | 'split' | 'code' | 'mockup'>('phone');
  const [devicePreset, setDevicePreset] = useState<DevicePreset>('standard');
  const [toastMessage, setToastMessage] = useState<string | null>(null);

  // App Theme state: 'dark' (tactical cyber) or 'light' (clean slate)
  const [theme, setTheme] = useState<AppTheme>(() => {
    const saved = localStorage.getItem('aimshot_theme');
    return (saved === 'light' || saved === 'dark') ? saved : 'dark';
  });

  // App Language state: strictly 'en', 'bn', or 'hi'
  const [language, setLanguage] = useState<Language>(() => {
    const saved = localStorage.getItem('aimshot_lang');
    return (saved === 'bn' || saved === 'hi' || saved === 'en') ? saved : 'en';
  });

  // Currently selected game (default Carrom Disc Pool)
  const [selectedGame, setSelectedGame] = useState<Game>(SUPPORTED_GAMES[0]);

  // Active game launch session overlay
  const [launchingGameSession, setLaunchingGameSession] = useState<{
    game: Game;
    status: 'initializing' | 'injecting' | 'running';
  } | null>(null);

  // Track whether Main AimShot Engine APK (com.alpha10.aimshot) is installed
  const [isMainApkInstalled, setIsMainApkInstalled] = useState<boolean>(() => {
    if (typeof window !== 'undefined') {
      return localStorage.getItem('aimshot_main_apk_installed') === 'true';
    }
    return false;
  });

  // Track if Update & Install Modal is open from Launcher Button
  const [isLauncherUpdateOpen, setIsLauncherUpdateOpen] = useState(false);
  const [launcherUpdateStage, setLauncherUpdateStage] = useState<
    'idle' | 'auth' | 'downloading' | 'verifying' | 'installing' | 'installed'
  >('idle');
  const [launcherUpdateProgress, setLauncherUpdateProgress] = useState(0);

  // Active Main AimShot Engine session overlay (Package: com.alpha10.aimshot - NEVER Carrom Pool!)
  const [launchingEngineSession, setLaunchingEngineSession] = useState<{
    packageName: string;
    versionName: string;
    status: 'initializing' | 'injecting' | 'running';
  } | null>(null);

  // Active membership state starting as Free / Logged Out
  const [membership, setMembership] = useState<MembershipState>(() => {
    const saved = localStorage.getItem('aimshot_membership');
    if (saved) {
      try {
        return JSON.parse(saved);
      } catch {
        // fallback
      }
    }
    return {
      isLoggedIn: false,
      status: 'free',
      planType: 'Free / Not Activated',
      startDate: '--',
      expiryDate: '--',
      daysRemaining: 0,
      featuresUnlocked: false,
      boundDeviceId: 'AIM-8839-4491-BC99'
    };
  });

  // Real-time App & Server Notifications State
  const [notifications, setNotifications] = useState<AppNotification[]>(() => {
    const saved = localStorage.getItem('aimshot_notifications');
    if (saved) {
      try {
        return JSON.parse(saved);
      } catch {
        // fallback
      }
    }
    return [
      {
        id: 'notif-101',
        title: 'AimShot Engine v1.0.0 Global Release',
        message: 'Welcome to AimShot Engine! Enhanced auto-aim guidelines, millimeter precision physics, and 120 FPS frame pacing are active.',
        category: 'announcement',
        timestamp: 'Today, 10:24 AM',
        isRead: false,
        minVersion: '1.0.0',
        priority: 'high',
        isActive: true
      },
      {
        id: 'notif-102',
        title: 'Carrom Disc Pool Guideline Calibration',
        message: 'New millimeter precision laser physics engine calibrated for smooth bank shots and double rebound angles.',
        category: 'update',
        timestamp: 'Yesterday',
        isRead: false,
        minVersion: '1.0.0',
        priority: 'normal',
        isActive: true
      },
      {
        id: 'notif-103',
        title: 'Anti-Ban Shield v2.4 Active',
        message: 'Kernel touch hook isolation verified against latest mobile anti-cheat updates. 100% undetectable safe overlay kernel.',
        category: 'feature',
        timestamp: '2 days ago',
        isRead: false,
        minVersion: '1.0.0',
        priority: 'high',
        isActive: true
      },
      {
        id: 'notif-104',
        title: 'Cloud Server Optimization Notice',
        message: 'Routine cloud verification server maintenance scheduled for 03:00 UTC. In-app offline license verification remains 100% functional.',
        category: 'maintenance',
        timestamp: '3 days ago',
        isRead: false,
        minVersion: '1.0.0',
        priority: 'normal',
        isActive: true
      }
    ];
  });

  const [isSyncingNotifs, setIsSyncingNotifs] = useState(false);
  const [isAdminModalOpen, setIsAdminModalOpen] = useState(false);

  const fetchNotifications = useCallback(async () => {
    setIsSyncingNotifs(true);
    try {
      const res = await fetch('/api/notifications');
      if (res.ok) {
        const data = await res.json();
        if (Array.isArray(data) && data.length > 0) {
          setNotifications(data);
          localStorage.setItem('aimshot_notifications', JSON.stringify(data));
        }
      }
    } catch {
      // offline fallback
    } finally {
      setIsSyncingNotifs(false);
    }
  }, []);

  useEffect(() => {
    fetchNotifications();
  }, [fetchNotifications]);

  useEffect(() => {
    localStorage.setItem('aimshot_notifications', JSON.stringify(notifications));
  }, [notifications]);

  const handleMarkAsRead = async (id: string) => {
    setNotifications((prev) =>
      prev.map((n) => (n.id === id ? { ...n, isRead: true } : n))
    );
    try {
      await fetch('/api/notifications/read', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ id })
      });
    } catch {
      // offline persistence handles it
    }
  };

  const handleMarkAllAsRead = async () => {
    setNotifications((prev) => prev.map((n) => ({ ...n, isRead: true })));
    try {
      await fetch('/api/notifications/read-all', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' }
      });
    } catch {
      // offline persistence handles it
    }
    showToast('All notifications marked as read');
  };

  const handlePublishNotification = async (newNotif: {
    title: string;
    message: string;
    category: 'announcement' | 'update' | 'feature' | 'maintenance';
    priority: 'normal' | 'high' | 'urgent';
    minVersion: string;
  }) => {
    try {
      const res = await fetch('/api/admin/notifications', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(newNotif)
      });
      if (res.ok) {
        await fetchNotifications();
        showToast('Announcement broadcasted to AimShot Engine!');
      } else {
        throw new Error('Failed to publish');
      }
    } catch {
      const localNotif: AppNotification = {
        id: `notif-${Date.now()}`,
        title: newNotif.title,
        message: newNotif.message,
        category: newNotif.category,
        timestamp: 'Just now',
        isRead: false,
        priority: newNotif.priority,
        minVersion: newNotif.minVersion,
        isActive: true
      };
      setNotifications((prev) => [localNotif, ...prev]);
      showToast('Announcement saved locally');
    }
  };

  const handleDeleteNotification = async (id: string) => {
    try {
      await fetch(`/api/admin/notifications/${id}`, {
        method: 'DELETE'
      });
    } catch {
      // safe fallback
    }
    setNotifications((prev) => prev.filter((n) => n.id !== id));
  };

  const unreadNotificationsCount = notifications.filter((n) => !n.isRead).length;

  // Save state changes
  useEffect(() => {
    localStorage.setItem('aimshot_theme', theme);
  }, [theme]);

  useEffect(() => {
    localStorage.setItem('aimshot_lang', language);
  }, [language]);

  useEffect(() => {
    localStorage.setItem('aimshot_membership', JSON.stringify(membership));
  }, [membership]);

  const t = TRANSLATIONS[language];

  const showToast = (msg: string) => {
    setToastMessage(msg);
    setTimeout(() => setToastMessage(null), 3200);
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

  const handleToggleTheme = () => {
    setTheme((prev) => (prev === 'dark' ? 'light' : 'dark'));
  };

  const handleChangeLanguage = (newLang: Language) => {
    setLanguage(newLang);
  };

  const handleLoginSuccess = (key: string) => {
    const newMembership: MembershipState = {
      isLoggedIn: true,
      status: 'active',
      planType: 'Monthly Premium',
      startDate: '07 Apr 2025',
      expiryDate: '25 Apr 2025',
      daysRemaining: 18,
      featuresUnlocked: true,
      boundDeviceId: 'AIM-8839-4491-BC99'
    };
    setMembership(newMembership);
    showToast(t.keyVerified);
  };

  const handleLogout = () => {
    const resetMembership: MembershipState = {
      isLoggedIn: false,
      status: 'free',
      planType: 'Free / Not Activated',
      startDate: '--',
      expiryDate: '--',
      daysRemaining: 0,
      featuresUnlocked: false,
      boundDeviceId: 'AIM-8839-4491-BC99'
    };
    setMembership(resetMembership);
    setCurrentScreen('home');
    setCurrentTab('home');
    showToast(t.sessionCleared);
  };

  const handleSelectGame = (game: Game) => {
    setSelectedGame(game);
    showToast(`${game.name}: ${game.features}`);
  };

  const handleLaunchGame = (game: Game) => {
    // If user is logged in, initiate launch
    if (!membership.isLoggedIn) {
      setIsLoginModalOpen(true);
      return;
    }

    // A. Mandatory Internet Check
    if (typeof navigator !== 'undefined' && !navigator.onLine) {
      showToast('No Internet Connection. Please connect to the internet and try again.');
      return;
    }

    setLaunchingGameSession({
      game,
      status: 'initializing'
    });

    // Step 1: Initialize touch guidelines
    setTimeout(() => {
      setLaunchingGameSession({
        game,
        status: 'injecting'
      });
    }, 900);

    // Step 2: Running state
    setTimeout(() => {
      setLaunchingGameSession({
        game,
        status: 'running'
      });
      showToast(`${t.gameLaunchedSuccess} ${game.name}`);
    }, 2000);
  };

  const handleLaunchEngine = () => {
    if (!membership.isLoggedIn) {
      setIsLoginModalOpen(true);
      return;
    }

    // 1. Mandatory Internet Check
    if (typeof navigator !== 'undefined' && !navigator.onLine) {
      showToast('No Internet Connection. Please connect to the internet and try again.');
      return;
    }

    // Situation A: Main APK is already installed
    if (isMainApkInstalled) {
      // Launch Main APK: com.alpha10.aimshot (Under NO circumstances launch Carrom Pool!)
      setLaunchingEngineSession({
        packageName: 'com.alpha10.aimshot',
        versionName: '1.0.1',
        status: 'initializing'
      });

      setTimeout(() => {
        setLaunchingEngineSession(prev => prev ? { ...prev, status: 'injecting' } : null);
      }, 700);

      setTimeout(() => {
        setLaunchingEngineSession(prev => prev ? { ...prev, status: 'running' } : null);
        showToast(t.aimshotEngineLaunched);
      }, 1500);
      return;
    }

    // Situation B: Main APK is not installed yet -> Open Alpha10 download & verification pipeline
    showToast(t.mainApkNotInstalledToast);
    setIsLauncherUpdateOpen(true);
    setLauncherUpdateStage('idle');
    setLauncherUpdateProgress(0);
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
    } catch {
      window.location.href = '/AimShot-Engine-v1.0.0.apk';
    }
  };

  return (
    <div
      className={`min-h-screen flex flex-col font-sans transition-colors duration-200 ${
        theme === 'dark' ? 'bg-[#070B12] text-[#F4F7FC]' : 'bg-[#E2E8F0] text-[#0F172A]'
      }`}
    >
      {/* Top Application Control & Navigation Bar */}
      <header
        className={`h-16 px-3 sm:px-6 md:px-8 border-b flex items-center justify-between shrink-0 z-50 transition-colors ${
          theme === 'dark'
            ? 'bg-[#0B0F17] border-[#1C2B42]'
            : 'bg-white border-[#CBD5E1] shadow-xs'
        }`}
      >
        <div className="flex items-center gap-2.5">
          <div className="flex items-baseline gap-1.5 font-bold text-base sm:text-lg tracking-wider">
            <span className={theme === 'dark' ? 'text-white' : 'text-[#0F172A]'}>AimShot</span>
            <span className="text-[#39B9FF] drop-shadow-[0_0_8px_rgba(57,185,255,0.6)]">ENGINE</span>
          </div>

          <span
            className={`hidden lg:inline-block px-2.5 py-0.5 rounded-full border text-[11px] font-mono ${
              theme === 'dark'
                ? 'bg-[#111B2A] border-[#39B9FF]/30 text-[#39B9FF]'
                : 'bg-[#E0F2FE] border-[#39B9FF]/40 text-[#0284C7]'
            }`}
          >
            com.alpha10.aimshot
          </span>
        </div>

        {/* View Switcher Tabs (Clean & Focused) */}
        <div
          className={`flex items-center gap-1 p-1 rounded-xl border ${
            theme === 'dark'
              ? 'bg-[#111B2A] border-[#1C2B42]'
              : 'bg-[#F1F5F9] border-[#CBD5E1]'
          }`}
        >
          <button
            onClick={() => setViewMode('phone')}
            className={`px-3 py-1.5 rounded-lg text-xs font-bold flex items-center gap-1.5 transition-all cursor-pointer ${
              viewMode === 'phone'
                ? 'bg-[#39B9FF] text-[#0B0F17] shadow-[0_0_10px_rgba(57,185,255,0.3)]'
                : theme === 'dark'
                ? 'text-[#8D9BB0] hover:text-white'
                : 'text-[#64748B] hover:text-[#0F172A]'
            }`}
          >
            <Smartphone className="w-3.5 h-3.5" />
            <span>Mobile</span>
          </button>

          <button
            onClick={() => setViewMode('code')}
            className={`px-3 py-1.5 rounded-lg text-xs font-bold flex items-center gap-1.5 transition-all cursor-pointer ${
              viewMode === 'code'
                ? 'bg-[#39B9FF] text-[#0B0F17] shadow-[0_0_10px_rgba(57,185,255,0.3)]'
                : theme === 'dark'
                ? 'text-[#8D9BB0] hover:text-white'
                : 'text-[#64748B] hover:text-[#0F172A]'
            }`}
          >
            <Code className="w-3.5 h-3.5" />
            <span className="hidden sm:inline">Android Code</span>
          </button>
        </div>
      </header>

      {/* Main Content Viewport */}
      <main className="flex-1 overflow-auto p-2 sm:p-4 md:p-6 flex items-center justify-center relative">
        {/* VIEW 1: Phone Emulator */}
        {viewMode === 'phone' && (
          <div className="w-full h-full flex items-center justify-center my-auto py-1">
            <PhoneFrame
              currentTab={currentTab}
              currentScreen={currentScreen}
              devicePreset={devicePreset}
              theme={theme}
              t={t}
              onTabChange={handleTabChange}
            >
              {currentScreen === 'splash' && (
                <SplashScreen onFinish={() => setCurrentScreen('home')} />
              )}

              {currentScreen === 'home' && (
                <HomeScreen
                  membership={membership}
                  theme={theme}
                  t={t}
                  selectedGame={selectedGame}
                  unreadNotificationsCount={unreadNotificationsCount}
                  onSelectGame={handleSelectGame}
                  onOpenLoginModal={() => setIsLoginModalOpen(true)}
                  onViewAllGames={() => setCurrentScreen('supported_games')}
                  onOpenNotifications={() => setCurrentScreen('notifications')}
                  onNavigateToPremium={() => {
                    setCurrentTab('premium');
                    setCurrentScreen('premium');
                  }}
                  onLaunchGame={handleLaunchGame}
                  isMainApkInstalled={isMainApkInstalled}
                  onLaunchEngine={handleLaunchEngine}
                />
              )}

              {currentScreen === 'premium' && (
                <PremiumScreen
                  membership={membership}
                  theme={theme}
                  t={t}
                  unreadNotificationsCount={unreadNotificationsCount}
                  onOpenNotifications={() => setCurrentScreen('notifications')}
                  onUpgradeRenew={() => setCurrentScreen('seller_contact')}
                  onShowToast={showToast}
                />
              )}

              {currentScreen === 'notifications' && (
                <NotificationsScreen
                  theme={theme}
                  t={t}
                  notifications={notifications}
                  onBack={() => setCurrentScreen(currentTab === 'settings' ? 'settings' : 'home')}
                  onMarkAsRead={handleMarkAsRead}
                  onMarkAllAsRead={handleMarkAllAsRead}
                  onRefresh={fetchNotifications}
                  isSyncing={isSyncingNotifs}
                />
              )}

              {currentScreen === 'supported_games' && (
                <SupportedGamesScreen
                  theme={theme}
                  t={t}
                  selectedGame={selectedGame}
                  onBack={() => setCurrentScreen('home')}
                  onSelectGame={(game) => {
                    handleSelectGame(game);
                    setCurrentScreen('home');
                  }}
                />
              )}

              {currentScreen === 'seller_contact' && (
                <SellerContactScreen
                  theme={theme}
                  t={t}
                  onBack={() => setCurrentScreen(currentTab === 'settings' ? 'settings' : 'home')}
                  onShowToast={showToast}
                />
              )}

              {currentScreen === 'device_info' && (
                <DeviceInfoScreen
                  theme={theme}
                  t={t}
                  onBack={() => setCurrentScreen('settings')}
                  onShowToast={showToast}
                />
              )}

              {currentScreen === 'settings' && (
                <SettingsScreen
                  theme={theme}
                  language={language}
                  t={t}
                  onToggleTheme={handleToggleTheme}
                  onChangeLanguage={handleChangeLanguage}
                  onBack={() => {
                    setCurrentTab('home');
                    setCurrentScreen('home');
                  }}
                  onLogout={handleLogout}
                  onOpenDeviceInfo={() => setCurrentScreen('device_info')}
                  onOpenSellerContact={() => setCurrentScreen('seller_contact')}
                  onShowToast={showToast}
                />
              )}

              {/* Verification Dialog Modal overlay */}
              <VerificationModal
                isOpen={isLoginModalOpen}
                onClose={() => setIsLoginModalOpen(false)}
                onSuccess={handleLoginSuccess}
              />

              {/* Launcher Update & Installation Modal (Stage 1 to 9) */}
              {isLauncherUpdateOpen && (
                <div className="absolute inset-0 z-50 flex items-center justify-center p-4 bg-black/90 backdrop-blur-md animate-fade-in select-none">
                  <div
                    className={`w-full max-w-xs rounded-2xl p-5 border text-center shadow-2xl relative ${
                      theme === 'dark' ? 'bg-[#111B2A] border-[#39B9FF]/50 text-white' : 'bg-white border-[#0284C7]/50 text-[#0F172A]'
                    }`}
                  >
                    <button
                      onClick={() => {
                        setIsLauncherUpdateOpen(false);
                        setLauncherUpdateStage('idle');
                        setLauncherUpdateProgress(0);
                      }}
                      className="absolute top-3 right-3 p-1 rounded-lg text-slate-400 hover:text-white cursor-pointer"
                    >
                      <X className="w-4 h-4" />
                    </button>

                    <div className="w-12 h-12 rounded-2xl bg-[#39B9FF]/15 text-[#39B9FF] flex items-center justify-center mx-auto mb-3 border border-[#39B9FF]/30">
                      <Download className="w-6 h-6" />
                    </div>

                    <h3 className="text-base font-bold tracking-wide">
                      {language === 'bn' ? 'মেইন এইমশট ইঞ্জিন ইনস্টলার' : language === 'hi' ? 'मुख्य एमशॉट इंजन इंस्टालर' : 'Main AimShot Engine Installer'}
                    </h3>
                    <span className="text-[10px] text-[#39B9FF] font-mono tracking-wider block mt-0.5 mb-2">
                      PACKAGE: com.alpha10.aimshot
                    </span>

                    {/* Pipeline Status Box */}
                    <div className="w-full bg-[#080C12] rounded-xl p-3 border border-white/10 text-left text-xs space-y-1 mb-4 font-mono">
                      <div className="flex items-center justify-between text-[10px]">
                        <span className="text-slate-400">1. Internet &amp; Server:</span>
                        <span className={launcherUpdateProgress >= 15 ? 'text-[#49DCA0] font-bold' : 'text-slate-500'}>
                          {launcherUpdateProgress >= 15 ? 'ONLINE' : 'WAITING'}
                        </span>
                      </div>
                      <div className="flex items-center justify-between text-[10px]">
                        <span className="text-slate-400">2. Alpha10 Token (60s):</span>
                        <span className={launcherUpdateProgress >= 35 ? 'text-[#49DCA0] font-bold' : 'text-slate-500'}>
                          {launcherUpdateProgress >= 35 ? 'OBTAINED' : 'PENDING'}
                        </span>
                      </div>
                      <div className="flex items-center justify-between text-[10px]">
                        <span className="text-slate-400">3. Binary APK Download:</span>
                        <span className={launcherUpdateProgress >= 65 ? 'text-[#49DCA0] font-bold' : 'text-slate-500'}>
                          {launcherUpdateProgress >= 65 ? 'STREAMED' : 'PENDING'}
                        </span>
                      </div>
                      <div className="flex items-center justify-between text-[10px]">
                        <span className="text-slate-400">4. SHA-256 Digest:</span>
                        <span className={launcherUpdateProgress >= 85 ? 'text-[#49DCA0] font-bold' : 'text-slate-500'}>
                          {launcherUpdateProgress >= 85 ? 'VERIFIED' : 'PENDING'}
                        </span>
                      </div>
                      <div className="flex items-center justify-between text-[10px]">
                        <span className="text-slate-400">5. Permanent Certificate:</span>
                        <span className={launcherUpdateProgress >= 95 ? 'text-[#49DCA0] font-bold' : 'text-slate-500'}>
                          {launcherUpdateProgress >= 95 ? '1C:8D:56:83...' : 'PENDING'}
                        </span>
                      </div>
                      <div className="flex items-center justify-between text-[10px]">
                        <span className="text-slate-400">6. OS PackageInstaller:</span>
                        <span className={launcherUpdateStage === 'installed' ? 'text-[#49DCA0] font-bold' : 'text-slate-500'}>
                          {launcherUpdateStage === 'installed' ? 'SUCCESS (Code 0)' : 'PENDING'}
                        </span>
                      </div>
                    </div>

                    {/* Progress Bar */}
                    {launcherUpdateStage !== 'idle' && launcherUpdateStage !== 'installed' && (
                      <div className="w-full bg-[#1C2B42] h-2 rounded-full overflow-hidden mb-4">
                        <div
                          className="bg-[#39B9FF] h-full transition-all duration-300"
                          style={{ width: `${launcherUpdateProgress}%` }}
                        />
                      </div>
                    )}

                    {/* Actions */}
                    {launcherUpdateStage === 'idle' && (
                      <button
                        onClick={() => {
                          setLauncherUpdateStage('auth');
                          setLauncherUpdateProgress(20);
                          setTimeout(() => {
                            setLauncherUpdateStage('downloading');
                            setLauncherUpdateProgress(50);
                            setTimeout(() => {
                              setLauncherUpdateStage('verifying');
                              setLauncherUpdateProgress(80);
                              setTimeout(() => {
                                setLauncherUpdateStage('installing');
                                setLauncherUpdateProgress(95);
                                setTimeout(() => {
                                  setLauncherUpdateStage('installed');
                                  setLauncherUpdateProgress(100);
                                  setIsMainApkInstalled(true);
                                  if (typeof window !== 'undefined') {
                                    localStorage.setItem('aimshot_main_apk_installed', 'true');
                                  }
                                }, 900);
                              }, 800);
                            }, 900);
                          }, 600);
                        }}
                        className="w-full py-2.5 rounded-xl bg-gradient-to-r from-[#1A85CC] via-[#39B9FF] to-[#1A85CC] text-[#0B0F17] text-xs font-black uppercase tracking-wider hover:opacity-95 cursor-pointer shadow-[0_0_15px_rgba(57,185,255,0.4)]"
                      >
                        {language === 'bn' ? 'ডাউনলোড ও ইনস্টল শুরু করুন' : language === 'hi' ? 'डाउनलोड एवं इंस्टॉल शुरू करें' : 'Download & Install APK'}
                      </button>
                    )}

                    {launcherUpdateStage === 'installed' && (
                      <div className="space-y-2">
                        <div className="flex items-center justify-center gap-1.5 text-xs text-[#49DCA0] font-bold">
                          <CheckCircle2 className="w-4 h-4" />
                          <span>{language === 'bn' ? 'ইনস্টলেশন সম্পন্ন হয়েছে!' : 'Installation Confirmed!'}</span>
                        </div>
                        <button
                          onClick={() => {
                            setIsLauncherUpdateOpen(false);
                            setLauncherUpdateStage('idle');
                            setLauncherUpdateProgress(0);
                            setLaunchingEngineSession({
                              packageName: 'com.alpha10.aimshot',
                              versionName: '1.0.1',
                              status: 'running'
                            });
                            showToast(t.aimshotEngineLaunched);
                          }}
                          className="w-full py-2.5 rounded-xl bg-gradient-to-r from-[#1A85CC] via-[#39B9FF] to-[#1A85CC] text-[#0B0F17] text-xs font-black uppercase tracking-wider hover:opacity-95 cursor-pointer shadow-[0_0_20px_rgba(57,185,255,0.45)]"
                        >
                          {language === 'bn' ? 'মেইন এইমশট ইঞ্জিন চালু করুন' : 'OPEN AIMSHOT ENGINE'}
                        </button>
                      </div>
                    )}
                  </div>
                </div>
              )}

              {/* Main AimShot Engine HUD Simulation Overlay (Package: com.alpha10.aimshot - NEVER Carrom Pool!) */}
              {launchingEngineSession && (
                <div className="absolute inset-0 z-50 flex items-center justify-center p-4 bg-black/95 backdrop-blur-md animate-fade-in select-none">
                  <div
                    className={`w-full max-w-xs rounded-2xl p-5 border text-center shadow-2xl relative ${
                      theme === 'dark' ? 'bg-[#0E1524] border-[#39B9FF]/60 text-white' : 'bg-white border-[#0284C7]/60 text-[#0F172A]'
                    }`}
                  >
                    <button
                      onClick={() => setLaunchingEngineSession(null)}
                      className="absolute top-3 right-3 p-1 rounded-lg text-slate-400 hover:text-white cursor-pointer"
                    >
                      <X className="w-4 h-4" />
                    </button>

                    <div className="w-16 h-16 rounded-2xl mx-auto mb-3 p-2 bg-[#142338] border border-[#39B9FF]/40 flex items-center justify-center shadow-[0_0_25px_rgba(57,185,255,0.3)]">
                      <Sparkles className="w-8 h-8 text-[#39B9FF]" />
                    </div>

                    <h3 className="text-base font-bold tracking-wide">AimShot Engine</h3>
                    <p className="text-xs text-[#39B9FF] font-mono mt-0.5 mb-3 font-semibold">
                      Package: com.alpha10.aimshot
                    </p>

                    <div className="w-full bg-[#080C12] rounded-xl p-3 border border-white/10 text-left text-xs space-y-1.5 mb-4 font-mono">
                      <div className="flex items-center justify-between text-[11px]">
                        <span className="text-slate-400">Application Identity:</span>
                        <span className="text-[#39B9FF] font-bold">Main Engine APK</span>
                      </div>
                      <div className="flex items-center justify-between text-[11px]">
                        <span className="text-slate-400">Package Verification:</span>
                        <span className="text-[#49DCA0] font-bold">MATCHED (com.alpha10.aimshot)</span>
                      </div>
                      <div className="flex items-center justify-between text-[11px]">
                        <span className="text-slate-400">Guideline Core:</span>
                        <span className="text-[#49DCA0] font-bold">120 FPS Realtime</span>
                      </div>
                      <div className="flex items-center justify-between text-[11px]">
                        <span className="text-slate-400">Hardware Touch Hook:</span>
                        <span className="text-[#49DCA0] font-bold">ACTIVE &amp; INJECTED</span>
                      </div>
                    </div>

                    {launchingEngineSession.status !== 'running' ? (
                      <div className="flex items-center justify-center gap-2 py-2 text-xs text-[#39B9FF] font-bold">
                        <span className="w-2 h-2 rounded-full bg-[#39B9FF] animate-ping" />
                        <span>{t.launchingGame}</span>
                      </div>
                    ) : (
                      <div className="space-y-2">
                        <div className="flex items-center justify-center gap-1.5 text-xs text-[#49DCA0] font-bold">
                          <CheckCircle2 className="w-4 h-4" />
                          <span>AimShot Engine Running Standalone</span>
                        </div>
                        <button
                          onClick={() => setLaunchingEngineSession(null)}
                          className="w-full py-2.5 rounded-xl bg-[#39B9FF] text-[#0B0F17] text-xs font-black uppercase tracking-wider hover:bg-[#2da5e6] cursor-pointer shadow-[0_0_15px_rgba(57,185,255,0.4)]"
                        >
                          Minimize Engine HUD
                        </button>
                      </div>
                    )}
                  </div>
                </div>
              )}
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
                theme={theme}
                t={t}
                onTabChange={handleTabChange}
              >
                {currentScreen === 'splash' && (
                  <SplashScreen onFinish={() => setCurrentScreen('home')} />
                )}

                {currentScreen === 'home' && (
                  <HomeScreen
                    membership={membership}
                    theme={theme}
                    t={t}
                    selectedGame={selectedGame}
                    unreadNotificationsCount={unreadNotificationsCount}
                    onSelectGame={handleSelectGame}
                    onOpenLoginModal={() => setIsLoginModalOpen(true)}
                    onViewAllGames={() => setCurrentScreen('supported_games')}
                    onOpenNotifications={() => setCurrentScreen('notifications')}
                    onNavigateToPremium={() => {
                      setCurrentTab('premium');
                      setCurrentScreen('premium');
                    }}
                    onLaunchGame={handleLaunchGame}
                    isMainApkInstalled={isMainApkInstalled}
                    onLaunchEngine={handleLaunchEngine}
                  />
                )}

                {currentScreen === 'premium' && (
                  <PremiumScreen
                    membership={membership}
                    theme={theme}
                    t={t}
                    unreadNotificationsCount={unreadNotificationsCount}
                    onOpenNotifications={() => setCurrentScreen('notifications')}
                    onUpgradeRenew={() => setCurrentScreen('seller_contact')}
                    onShowToast={showToast}
                  />
                )}

                {currentScreen === 'notifications' && (
                  <NotificationsScreen
                    theme={theme}
                    t={t}
                    notifications={notifications}
                    onBack={() => setCurrentScreen(currentTab === 'settings' ? 'settings' : 'home')}
                    onMarkAsRead={handleMarkAsRead}
                    onMarkAllAsRead={handleMarkAllAsRead}
                    onRefresh={fetchNotifications}
                    isSyncing={isSyncingNotifs}
                  />
                )}

                {currentScreen === 'supported_games' && (
                  <SupportedGamesScreen
                    theme={theme}
                    t={t}
                    selectedGame={selectedGame}
                    onBack={() => setCurrentScreen('home')}
                    onSelectGame={(game) => {
                      handleSelectGame(game);
                      setCurrentScreen('home');
                    }}
                  />
                )}

                {currentScreen === 'seller_contact' && (
                  <SellerContactScreen
                    theme={theme}
                    t={t}
                    onBack={() => setCurrentScreen(currentTab === 'settings' ? 'settings' : 'home')}
                    onShowToast={showToast}
                  />
                )}

                {currentScreen === 'device_info' && (
                  <DeviceInfoScreen
                    theme={theme}
                    t={t}
                    onBack={() => setCurrentScreen('settings')}
                    onShowToast={showToast}
                  />
                )}

                {currentScreen === 'settings' && (
                  <SettingsScreen
                    theme={theme}
                    language={language}
                    t={t}
                    onToggleTheme={handleToggleTheme}
                    onChangeLanguage={handleChangeLanguage}
                    onBack={() => {
                      setCurrentTab('home');
                      setCurrentScreen('home');
                    }}
                    onLogout={handleLogout}
                    onOpenDeviceInfo={() => setCurrentScreen('device_info')}
                    onOpenSellerContact={() => setCurrentScreen('seller_contact')}
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
                Comparison against graphic specifications: Splash, Home, Key Login, Premium Active 18 Days, Supported Games, Seller Contact, Device Information, and Settings.
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

        {/* Website Admin Broadcast Management Modal */}
        <AdminNotificationModal
          isOpen={isAdminModalOpen}
          onClose={() => setIsAdminModalOpen(false)}
          notifications={notifications}
          onPublishNotification={handlePublishNotification}
          onDeleteNotification={handleDeleteNotification}
          onRefresh={fetchNotifications}
        />
      </main>
    </div>
  );
}
