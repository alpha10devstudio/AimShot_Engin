import React, { useState } from 'react';
import {
  ArrowLeft,
  Moon,
  Globe,
  Bell,
  Vibrate,
  Shield,
  HelpCircle,
  Info,
  LogOut,
  ChevronRight,
  Cpu
} from 'lucide-react';

interface SettingsScreenProps {
  onBack: () => void;
  onLogout: () => void;
  onOpenDeviceInfo: () => void;
  onOpenSellerContact: () => void;
  onDownloadApk?: () => void;
  onShowToast: (message: string) => void;
}

export const SettingsScreen: React.FC<SettingsScreenProps> = ({
  onBack,
  onLogout,
  onOpenDeviceInfo,
  onOpenSellerContact,
  onDownloadApk,
  onShowToast
}) => {
  const [darkMode, setDarkMode] = useState(true);
  const [notifications, setNotifications] = useState(true);
  const [haptic, setHaptic] = useState(true);
  const [showLogoutConfirm, setShowLogoutConfirm] = useState(false);
  const [activeModal, setActiveModal] = useState<'privacy' | 'about' | null>(null);

  return (
    <div className="w-full h-full flex flex-col bg-[#0B0F17] overflow-y-auto px-4 py-3 select-none relative">
      {/* Top Action Bar matching Screen 8 */}
      <div className="flex items-center gap-2 mb-4 pt-1">
        <button
          onClick={onBack}
          className="w-9 h-9 rounded-xl bg-[#111B2A] border border-[#1C2B42] flex items-center justify-center text-white hover:text-[#39B9FF] transition-colors"
        >
          <ArrowLeft className="w-5 h-5" />
        </button>
        <h2 className="text-base font-bold text-white tracking-wide">Settings</h2>
      </div>

      {/* Settings Rows matching Screen 8 */}
      <div className="space-y-2 mb-6">
        {/* App Theme */}
        <div className="p-3.5 rounded-2xl bg-[#111B2A] border border-[#1C2B42] flex items-center justify-between">
          <div className="flex items-center gap-3">
            <div className="w-9 h-9 rounded-xl bg-[#39B9FF]/15 flex items-center justify-center text-[#39B9FF]">
              <Moon className="w-4 h-4" />
            </div>
            <div className="flex flex-col">
              <span className="text-sm font-bold text-white">App Theme</span>
              <span className="text-[11px] text-[#8D9BB0]">Dark Mode</span>
            </div>
          </div>
          <button
            onClick={() => {
              setDarkMode(!darkMode);
              onShowToast(darkMode ? 'Light theme not recommended for tactical mode' : 'Dark Mode activated');
            }}
            className={`w-11 h-6 rounded-full transition-colors relative cursor-pointer ${
              darkMode ? 'bg-[#39B9FF]' : 'bg-[#1C2B42]'
            }`}
          >
            <div
              className={`w-4 h-4 rounded-full bg-[#0B0F17] absolute top-1 transition-transform ${
                darkMode ? 'right-1' : 'left-1'
              }`}
            />
          </button>
        </div>

        {/* Language */}
        <div
          onClick={() => onShowToast('Language is set to English (US)')}
          className="p-3.5 rounded-2xl bg-[#111B2A] border border-[#1C2B42] hover:border-[#39B9FF]/40 transition-all flex items-center justify-between cursor-pointer"
        >
          <div className="flex items-center gap-3">
            <div className="w-9 h-9 rounded-xl bg-[#39B9FF]/15 flex items-center justify-center text-[#39B9FF]">
              <Globe className="w-4 h-4" />
            </div>
            <div className="flex flex-col">
              <span className="text-sm font-bold text-white">Language</span>
              <span className="text-[11px] text-[#8D9BB0]">English</span>
            </div>
          </div>
          <ChevronRight className="w-4 h-4 text-[#8D9BB0]" />
        </div>

        {/* Notifications */}
        <div
          onClick={() => {
            setNotifications(!notifications);
            onShowToast(`Notifications ${!notifications ? 'enabled' : 'disabled'}`);
          }}
          className="p-3.5 rounded-2xl bg-[#111B2A] border border-[#1C2B42] hover:border-[#39B9FF]/40 transition-all flex items-center justify-between cursor-pointer"
        >
          <div className="flex items-center gap-3">
            <div className="w-9 h-9 rounded-xl bg-[#39B9FF]/15 flex items-center justify-center text-[#39B9FF]">
              <Bell className="w-4 h-4" />
            </div>
            <div className="flex flex-col">
              <span className="text-sm font-bold text-white">Notifications</span>
              <span className="text-[11px] text-[#8D9BB0]">{notifications ? 'On' : 'Off'}</span>
            </div>
          </div>
          <ChevronRight className="w-4 h-4 text-[#8D9BB0]" />
        </div>

        {/* Haptic Feedback */}
        <div
          onClick={() => {
            setHaptic(!haptic);
            onShowToast(`Haptic feedback ${!haptic ? 'enabled' : 'disabled'}`);
          }}
          className="p-3.5 rounded-2xl bg-[#111B2A] border border-[#1C2B42] hover:border-[#39B9FF]/40 transition-all flex items-center justify-between cursor-pointer"
        >
          <div className="flex items-center gap-3">
            <div className="w-9 h-9 rounded-xl bg-[#39B9FF]/15 flex items-center justify-center text-[#39B9FF]">
              <Vibrate className="w-4 h-4" />
            </div>
            <div className="flex flex-col">
              <span className="text-sm font-bold text-white">Haptic Feedback</span>
              <span className="text-[11px] text-[#8D9BB0]">{haptic ? 'On' : 'Off'}</span>
            </div>
          </div>
          <ChevronRight className="w-4 h-4 text-[#8D9BB0]" />
        </div>

        {/* Download APK File */}
        <button
          onClick={() => {
            if (onDownloadApk) {
              onDownloadApk();
            }
          }}
          className="w-full text-left p-3.5 rounded-2xl bg-gradient-to-r from-[#14283D] to-[#111B2A] border border-[#39B9FF]/50 hover:border-[#39B9FF] transition-all flex items-center justify-between cursor-pointer group shadow-[0_0_12px_rgba(57,185,255,0.15)]"
        >
          <div className="flex items-center gap-3">
            <div className="w-9 h-9 rounded-xl bg-[#39B9FF]/20 flex items-center justify-center text-[#39B9FF]">
              <Cpu className="w-4 h-4 text-[#39B9FF]" />
            </div>
            <div className="flex flex-col">
              <span className="text-sm font-bold text-white group-hover:text-[#39B9FF] transition-colors">
                Install APK Package
              </span>
              <span className="text-[11px] text-[#49DCA0] font-mono">
                AimShot-Engine-v1.0.0.apk (Ready)
              </span>
            </div>
          </div>
          <span className="text-[11px] px-2.5 py-1 rounded-lg bg-[#39B9FF] text-[#0B0F17] font-bold">
            Download
          </span>
        </button>

        {/* Device Information */}
        <div
          onClick={onOpenDeviceInfo}
          className="p-3.5 rounded-2xl bg-[#111B2A] border border-[#1C2B42] hover:border-[#39B9FF]/40 transition-all flex items-center justify-between cursor-pointer"
        >
          <div className="flex items-center gap-3">
            <div className="w-9 h-9 rounded-xl bg-[#39B9FF]/15 flex items-center justify-center text-[#39B9FF]">
              <Cpu className="w-4 h-4" />
            </div>
            <span className="text-sm font-bold text-white">Device Information</span>
          </div>
          <ChevronRight className="w-4 h-4 text-[#8D9BB0]" />
        </div>

        {/* Privacy & Security */}
        <div
          onClick={() => setActiveModal('privacy')}
          className="p-3.5 rounded-2xl bg-[#111B2A] border border-[#1C2B42] hover:border-[#39B9FF]/40 transition-all flex items-center justify-between cursor-pointer"
        >
          <div className="flex items-center gap-3">
            <div className="w-9 h-9 rounded-xl bg-[#39B9FF]/15 flex items-center justify-center text-[#39B9FF]">
              <Shield className="w-4 h-4" />
            </div>
            <span className="text-sm font-bold text-white">Privacy & Security</span>
          </div>
          <ChevronRight className="w-4 h-4 text-[#8D9BB0]" />
        </div>

        {/* Help & Support */}
        <div
          onClick={onOpenSellerContact}
          className="p-3.5 rounded-2xl bg-[#111B2A] border border-[#1C2B42] hover:border-[#39B9FF]/40 transition-all flex items-center justify-between cursor-pointer"
        >
          <div className="flex items-center gap-3">
            <div className="w-9 h-9 rounded-xl bg-[#39B9FF]/15 flex items-center justify-center text-[#39B9FF]">
              <HelpCircle className="w-4 h-4" />
            </div>
            <span className="text-sm font-bold text-white">Help & Support</span>
          </div>
          <ChevronRight className="w-4 h-4 text-[#8D9BB0]" />
        </div>

        {/* About App */}
        <div
          onClick={() => setActiveModal('about')}
          className="p-3.5 rounded-2xl bg-[#111B2A] border border-[#1C2B42] hover:border-[#39B9FF]/40 transition-all flex items-center justify-between cursor-pointer"
        >
          <div className="flex items-center gap-3">
            <div className="w-9 h-9 rounded-xl bg-[#39B9FF]/15 flex items-center justify-center text-[#39B9FF]">
              <Info className="w-4 h-4" />
            </div>
            <span className="text-sm font-bold text-white">About App</span>
          </div>
          <div className="flex items-center gap-1.5">
            <span className="text-xs text-[#8D9BB0] font-mono">v1.0.0</span>
            <ChevronRight className="w-4 h-4 text-[#8D9BB0]" />
          </div>
        </div>
      </div>

      {/* Red Outlined Log Out Button matching Screen 8 */}
      <button
        onClick={() => setShowLogoutConfirm(true)}
        className="w-full py-3 rounded-2xl bg-transparent border-2 border-[#FF4655] text-[#FF4655] hover:bg-[#FF4655]/10 active:scale-[0.98] font-bold text-sm tracking-wide transition-all shadow-[0_0_12px_rgba(255,70,85,0.15)] flex items-center justify-center gap-2.5 cursor-pointer mt-auto"
      >
        <LogOut className="w-4 h-4" />
        <span>Log Out</span>
      </button>

      {/* Logout Confirmation Dialog */}
      {showLogoutConfirm && (
        <div className="absolute inset-0 z-50 flex items-center justify-center p-4 bg-black/80 backdrop-blur-sm">
          <div className="w-full max-w-xs bg-[#111B2A] rounded-2xl p-5 border border-[#FF4655]/50 shadow-[0_0_25px_rgba(255,70,85,0.25)]">
            <h3 className="text-base font-bold text-white mb-2">Confirm Logout</h3>
            <p className="text-xs text-[#8D9BB0] mb-5 leading-relaxed">
              Are you sure you want to log out? Your active session will be cleared and premium license detached.
            </p>
            <div className="flex gap-2.5">
              <button
                onClick={() => setShowLogoutConfirm(false)}
                className="flex-1 py-2 rounded-xl bg-[#1C2B42] text-xs font-bold text-[#8D9BB0] hover:text-white"
              >
                Cancel
              </button>
              <button
                onClick={() => {
                  setShowLogoutConfirm(false);
                  onLogout();
                }}
                className="flex-1 py-2 rounded-xl bg-[#FF4655] text-xs font-bold text-white hover:bg-red-600 shadow-[0_0_10px_#FF4655]"
              >
                Log Out
              </button>
            </div>
          </div>
        </div>
      )}

      {/* Privacy Dialog */}
      {activeModal === 'privacy' && (
        <div className="absolute inset-0 z-50 flex items-center justify-center p-4 bg-black/80 backdrop-blur-sm">
          <div className="w-full max-w-xs bg-[#111B2A] rounded-2xl p-5 border border-[#39B9FF]/50">
            <h3 className="text-base font-bold text-white mb-2">Privacy & Security</h3>
            <div className="text-xs text-[#8D9BB0] space-y-2 mb-5 leading-relaxed">
              <p>• AimShot Engine uses app-scoped identifier hashing.</p>
              <p>• No hardware serial numbers, IMEI, or telephony identifiers are collected.</p>
              <p>• All license verifications use encrypted HTTPS endpoints.</p>
            </div>
            <button
              onClick={() => setActiveModal(null)}
              className="w-full py-2 rounded-xl bg-[#39B9FF] text-xs font-bold text-[#0B0F17]"
            >
              Understood
            </button>
          </div>
        </div>
      )}

      {/* About Dialog */}
      {activeModal === 'about' && (
        <div className="absolute inset-0 z-50 flex items-center justify-center p-4 bg-black/80 backdrop-blur-sm">
          <div className="w-full max-w-xs bg-[#111B2A] rounded-2xl p-5 border border-[#39B9FF]/50 text-center">
            <h3 className="text-base font-bold text-white">AimShot Engine</h3>
            <span className="text-xs text-[#39B9FF] font-mono block mt-0.5 mb-2">v1.0.0</span>
            <p className="text-xs text-[#8D9BB0] mb-2">Sharper Aim • Better Game • Higher You</p>
            <p className="text-[11px] text-[#55657E] mb-5 font-mono">
              Package: com.alpha10.aimshot<br />Native Android Java + XML
            </p>
            <button
              onClick={() => setActiveModal(null)}
              className="w-full py-2 rounded-xl bg-[#39B9FF] text-xs font-bold text-[#0B0F17]"
            >
              Close
            </button>
          </div>
        </div>
      )}
    </div>
  );
};
