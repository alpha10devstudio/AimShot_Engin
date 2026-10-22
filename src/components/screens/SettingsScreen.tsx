import React, { useState } from 'react';
import {
  ArrowLeft,
  Moon,
  Sun,
  Globe,
  Bell,
  Vibrate,
  Shield,
  HelpCircle,
  Info,
  LogOut,
  ChevronRight,
  Cpu,
  Check
} from 'lucide-react';
import { AppTheme } from '../../types';
import { Language, Translations } from '../../data/translations';

interface SettingsScreenProps {
  theme: AppTheme;
  language: Language;
  t: Translations;
  onToggleTheme: () => void;
  onChangeLanguage: (lang: Language) => void;
  onBack: () => void;
  onLogout: () => void;
  onOpenDeviceInfo: () => void;
  onOpenSellerContact: () => void;
  onShowToast: (message: string) => void;
}

export const SettingsScreen: React.FC<SettingsScreenProps> = ({
  theme,
  language,
  t,
  onToggleTheme,
  onChangeLanguage,
  onBack,
  onLogout,
  onOpenDeviceInfo,
  onOpenSellerContact,
  onShowToast
}) => {
  const isDark = theme === 'dark';
  const [notifications, setNotifications] = useState(true);
  const [haptic, setHaptic] = useState(true);
  const [isLangDialogOpen, setIsLangDialogOpen] = useState(false);
  const [showLogoutConfirm, setShowLogoutConfirm] = useState(false);
  const [activeModal, setActiveModal] = useState<'privacy' | 'about' | null>(null);

  const availableLanguages: { code: Language; name: string; nativeName: string }[] = [
    { code: 'en', name: 'English', nativeName: 'English (US)' },
    { code: 'bn', name: 'Bengali', nativeName: 'বাংলা' },
    { code: 'hi', name: 'Hindi', nativeName: 'हिन्दी' }
  ];

  const handleSelectLanguage = (langCode: Language) => {
    onChangeLanguage(langCode);
    setIsLangDialogOpen(false);
    const langNames: Record<Language, string> = {
      en: 'Language changed to English',
      bn: 'ভাষা পরিবর্তন করা হয়েছে: বাংলা',
      hi: 'भाषा बदलकर हिन्दी कर दी गई है'
    };
    onShowToast(langNames[langCode]);
  };

  return (
    <div
      className={`w-full h-full flex flex-col overflow-y-auto px-4 py-3 select-none relative transition-colors duration-200 ${
        isDark ? 'bg-[#0B0F17] text-[#F4F7FC]' : 'bg-[#F1F5F9] text-[#0F172A]'
      }`}
    >
      {/* Top Action Bar */}
      <div className="flex items-center gap-2 mb-4 pt-1">
        <button
          onClick={onBack}
          className={`w-9 h-9 rounded-xl flex items-center justify-center transition-colors cursor-pointer ${
            isDark
              ? 'bg-[#111B2A] border border-[#1C2B42] text-white hover:text-[#39B9FF]'
              : 'bg-white border border-[#CBD5E1] text-[#0F172A] hover:text-[#0284C7] shadow-xs'
          }`}
        >
          <ArrowLeft className="w-5 h-5" />
        </button>
        <h2 className={`text-base font-bold tracking-wide ${isDark ? 'text-white' : 'text-[#0F172A]'}`}>
          {t.settingsTitle}
        </h2>
      </div>

      {/* Settings Rows */}
      <div className="space-y-2 mb-6">
        {/* App Theme Toggle (Requirement 5) */}
        <div
          className={`p-3.5 rounded-2xl border flex items-center justify-between transition-all ${
            isDark ? 'bg-[#111B2A] border-[#1C2B42]' : 'bg-white border-[#CBD5E1] shadow-xs'
          }`}
        >
          <div className="flex items-center gap-3">
            <div
              className={`w-9 h-9 rounded-xl flex items-center justify-center ${
                isDark ? 'bg-[#39B9FF]/15 text-[#39B9FF]' : 'bg-[#E0F2FE] text-[#0284C7]'
              }`}
            >
              {isDark ? <Moon className="w-4 h-4" /> : <Sun className="w-4 h-4 text-amber-500" />}
            </div>
            <div className="flex flex-col">
              <span className={`text-sm font-bold ${isDark ? 'text-white' : 'text-[#0F172A]'}`}>
                {t.appTheme}
              </span>
              <span className={`text-[11px] ${isDark ? 'text-[#8D9BB0]' : 'text-[#64748B]'}`}>
                {isDark ? t.darkMode : t.lightMode}
              </span>
            </div>
          </div>

          <button
            onClick={() => {
              onToggleTheme();
              onShowToast(isDark ? 'Switched to Light Theme' : 'Switched to Tactical Dark Mode');
            }}
            className={`w-12 h-6 rounded-full transition-colors relative cursor-pointer ${
              isDark ? 'bg-[#39B9FF]' : 'bg-[#94A3B8]'
            }`}
          >
            <div
              className={`w-4 h-4 rounded-full bg-white shadow-md absolute top-1 transition-all ${
                isDark ? 'right-1' : 'left-1'
              }`}
            />
          </button>
        </div>

        {/* Language Selection Row (Requirement 6 & 7) */}
        <div
          onClick={() => setIsLangDialogOpen(true)}
          className={`p-3.5 rounded-2xl border transition-all flex items-center justify-between cursor-pointer ${
            isDark
              ? 'bg-[#111B2A] border-[#1C2B42] hover:border-[#39B9FF]/40'
              : 'bg-white border-[#CBD5E1] hover:border-[#0284C7]/40 shadow-xs'
          }`}
        >
          <div className="flex items-center gap-3">
            <div
              className={`w-9 h-9 rounded-xl flex items-center justify-center ${
                isDark ? 'bg-[#39B9FF]/15 text-[#39B9FF]' : 'bg-[#E0F2FE] text-[#0284C7]'
              }`}
            >
              <Globe className="w-4 h-4" />
            </div>
            <div className="flex flex-col">
              <span className={`text-sm font-bold ${isDark ? 'text-white' : 'text-[#0F172A]'}`}>
                {t.language}
              </span>
              <span className={`text-[11px] font-medium ${isDark ? 'text-[#39B9FF]' : 'text-[#0284C7]'}`}>
                {t.selectedLanguageName}
              </span>
            </div>
          </div>
          <ChevronRight className={`w-4 h-4 ${isDark ? 'text-[#8D9BB0]' : 'text-[#94A3B8]'}`} />
        </div>

        {/* Notifications Toggle */}
        <div
          onClick={() => {
            setNotifications(!notifications);
            onShowToast(`Notifications ${!notifications ? 'Enabled' : 'Disabled'}`);
          }}
          className={`p-3.5 rounded-2xl border transition-all flex items-center justify-between cursor-pointer ${
            isDark
              ? 'bg-[#111B2A] border-[#1C2B42] hover:border-[#39B9FF]/40'
              : 'bg-white border-[#CBD5E1] hover:border-[#0284C7]/40 shadow-xs'
          }`}
        >
          <div className="flex items-center gap-3">
            <div
              className={`w-9 h-9 rounded-xl flex items-center justify-center ${
                isDark ? 'bg-[#39B9FF]/15 text-[#39B9FF]' : 'bg-[#E0F2FE] text-[#0284C7]'
              }`}
            >
              <Bell className="w-4 h-4" />
            </div>
            <div className="flex flex-col">
              <span className={`text-sm font-bold ${isDark ? 'text-white' : 'text-[#0F172A]'}`}>
                {t.notifications}
              </span>
              <span className={`text-[11px] ${isDark ? 'text-[#8D9BB0]' : 'text-[#64748B]'}`}>
                {notifications ? t.notificationsOn : t.notificationsOff}
              </span>
            </div>
          </div>
          <div
            className={`w-10 h-5 rounded-full transition-colors relative ${
              notifications ? 'bg-[#39B9FF]' : isDark ? 'bg-[#1C2B42]' : 'bg-[#CBD5E1]'
            }`}
          >
            <div
              className={`w-3.5 h-3.5 rounded-full bg-white absolute top-0.5 transition-all ${
                notifications ? 'right-0.5' : 'left-0.5'
              }`}
            />
          </div>
        </div>

        {/* Haptic Feedback Toggle */}
        <div
          onClick={() => {
            setHaptic(!haptic);
            onShowToast(`Haptic Feedback ${!haptic ? 'Enabled' : 'Disabled'}`);
          }}
          className={`p-3.5 rounded-2xl border transition-all flex items-center justify-between cursor-pointer ${
            isDark
              ? 'bg-[#111B2A] border-[#1C2B42] hover:border-[#39B9FF]/40'
              : 'bg-white border-[#CBD5E1] hover:border-[#0284C7]/40 shadow-xs'
          }`}
        >
          <div className="flex items-center gap-3">
            <div
              className={`w-9 h-9 rounded-xl flex items-center justify-center ${
                isDark ? 'bg-[#39B9FF]/15 text-[#39B9FF]' : 'bg-[#E0F2FE] text-[#0284C7]'
              }`}
            >
              <Vibrate className="w-4 h-4" />
            </div>
            <div className="flex flex-col">
              <span className={`text-sm font-bold ${isDark ? 'text-white' : 'text-[#0F172A]'}`}>
                {t.hapticFeedback}
              </span>
              <span className={`text-[11px] ${isDark ? 'text-[#8D9BB0]' : 'text-[#64748B]'}`}>
                {haptic ? t.notificationsOn : t.notificationsOff}
              </span>
            </div>
          </div>
          <div
            className={`w-10 h-5 rounded-full transition-colors relative ${
              haptic ? 'bg-[#39B9FF]' : isDark ? 'bg-[#1C2B42]' : 'bg-[#CBD5E1]'
            }`}
          >
            <div
              className={`w-3.5 h-3.5 rounded-full bg-white absolute top-0.5 transition-all ${
                haptic ? 'right-0.5' : 'left-0.5'
              }`}
            />
          </div>
        </div>

        {/* Device Information */}
        <div
          onClick={onOpenDeviceInfo}
          className={`p-3.5 rounded-2xl border transition-all flex items-center justify-between cursor-pointer ${
            isDark
              ? 'bg-[#111B2A] border-[#1C2B42] hover:border-[#39B9FF]/40'
              : 'bg-white border-[#CBD5E1] hover:border-[#0284C7]/40 shadow-xs'
          }`}
        >
          <div className="flex items-center gap-3">
            <div
              className={`w-9 h-9 rounded-xl flex items-center justify-center ${
                isDark ? 'bg-[#39B9FF]/15 text-[#39B9FF]' : 'bg-[#E0F2FE] text-[#0284C7]'
              }`}
            >
              <Cpu className="w-4 h-4" />
            </div>
            <span className={`text-sm font-bold ${isDark ? 'text-white' : 'text-[#0F172A]'}`}>
              {t.deviceInformation}
            </span>
          </div>
          <ChevronRight className={`w-4 h-4 ${isDark ? 'text-[#8D9BB0]' : 'text-[#94A3B8]'}`} />
        </div>

        {/* Privacy & Security */}
        <div
          onClick={() => setActiveModal('privacy')}
          className={`p-3.5 rounded-2xl border transition-all flex items-center justify-between cursor-pointer ${
            isDark
              ? 'bg-[#111B2A] border-[#1C2B42] hover:border-[#39B9FF]/40'
              : 'bg-white border-[#CBD5E1] hover:border-[#0284C7]/40 shadow-xs'
          }`}
        >
          <div className="flex items-center gap-3">
            <div
              className={`w-9 h-9 rounded-xl flex items-center justify-center ${
                isDark ? 'bg-[#39B9FF]/15 text-[#39B9FF]' : 'bg-[#E0F2FE] text-[#0284C7]'
              }`}
            >
              <Shield className="w-4 h-4" />
            </div>
            <span className={`text-sm font-bold ${isDark ? 'text-white' : 'text-[#0F172A]'}`}>
              {t.privacySecurity}
            </span>
          </div>
          <ChevronRight className={`w-4 h-4 ${isDark ? 'text-[#8D9BB0]' : 'text-[#94A3B8]'}`} />
        </div>

        {/* Help & Support */}
        <div
          onClick={onOpenSellerContact}
          className={`p-3.5 rounded-2xl border transition-all flex items-center justify-between cursor-pointer ${
            isDark
              ? 'bg-[#111B2A] border-[#1C2B42] hover:border-[#39B9FF]/40'
              : 'bg-white border-[#CBD5E1] hover:border-[#0284C7]/40 shadow-xs'
          }`}
        >
          <div className="flex items-center gap-3">
            <div
              className={`w-9 h-9 rounded-xl flex items-center justify-center ${
                isDark ? 'bg-[#39B9FF]/15 text-[#39B9FF]' : 'bg-[#E0F2FE] text-[#0284C7]'
              }`}
            >
              <HelpCircle className="w-4 h-4" />
            </div>
            <span className={`text-sm font-bold ${isDark ? 'text-white' : 'text-[#0F172A]'}`}>
              {t.helpSupport}
            </span>
          </div>
          <ChevronRight className={`w-4 h-4 ${isDark ? 'text-[#8D9BB0]' : 'text-[#94A3B8]'}`} />
        </div>

        {/* About App */}
        <div
          onClick={() => setActiveModal('about')}
          className={`p-3.5 rounded-2xl border transition-all flex items-center justify-between cursor-pointer ${
            isDark
              ? 'bg-[#111B2A] border-[#1C2B42] hover:border-[#39B9FF]/40'
              : 'bg-white border-[#CBD5E1] hover:border-[#0284C7]/40 shadow-xs'
          }`}
        >
          <div className="flex items-center gap-3">
            <div
              className={`w-9 h-9 rounded-xl flex items-center justify-center ${
                isDark ? 'bg-[#39B9FF]/15 text-[#39B9FF]' : 'bg-[#E0F2FE] text-[#0284C7]'
              }`}
            >
              <Info className="w-4 h-4" />
            </div>
            <span className={`text-sm font-bold ${isDark ? 'text-white' : 'text-[#0F172A]'}`}>
              {t.aboutApp}
            </span>
          </div>
          <div className="flex items-center gap-1.5">
            <span className={`text-xs font-mono ${isDark ? 'text-[#8D9BB0]' : 'text-[#64748B]'}`}>
              v1.0.0
            </span>
            <ChevronRight className={`w-4 h-4 ${isDark ? 'text-[#8D9BB0]' : 'text-[#94A3B8]'}`} />
          </div>
        </div>
      </div>

      {/* Red Outlined Log Out Button */}
      <button
        onClick={() => setShowLogoutConfirm(true)}
        className="w-full py-3 rounded-2xl bg-transparent border-2 border-[#FF4655] text-[#FF4655] hover:bg-[#FF4655]/10 active:scale-[0.98] font-bold text-sm tracking-wide transition-all shadow-[0_0_12px_rgba(255,70,85,0.15)] flex items-center justify-center gap-2.5 cursor-pointer mt-auto"
      >
        <LogOut className="w-4 h-4" />
        <span>{t.logOut}</span>
      </button>

      {/* ========================================================
          REQUIREMENT 7: LANGUAGE SELECTION MODAL WITH FULL DIMMED
          BACKDROP & SOLID WELL-STYLED CARD (EXACTLY 3 LANGUAGES)
          ======================================================== */}
      {isLangDialogOpen && (
        <div className="absolute inset-0 z-50 flex items-center justify-center p-4 bg-black/85 backdrop-blur-md animate-fade-in">
          <div
            className={`w-full max-w-xs rounded-2xl p-5 border shadow-2xl transition-all ${
              isDark
                ? 'bg-[#111B2A] border-[#39B9FF]/50 text-white'
                : 'bg-white border-[#0284C7]/50 text-[#0F172A]'
            }`}
          >
            <div className="flex items-center gap-2.5 mb-2">
              <div
                className={`w-8 h-8 rounded-lg flex items-center justify-center ${
                  isDark ? 'bg-[#39B9FF]/15 text-[#39B9FF]' : 'bg-[#E0F2FE] text-[#0284C7]'
                }`}
              >
                <Globe className="w-4 h-4" />
              </div>
              <h3 className="text-base font-bold">{t.selectLanguageTitle}</h3>
            </div>

            <p className={`text-xs mb-4 ${isDark ? 'text-[#8D9BB0]' : 'text-[#64748B]'}`}>
              {t.selectLanguageDesc}
            </p>

            {/* Exactly 3 languages: English, Bengali, Hindi (Requirement 6) */}
            <div className="space-y-2 mb-5">
              {availableLanguages.map((langItem) => {
                const isSelected = language === langItem.code;
                return (
                  <button
                    key={langItem.code}
                    onClick={() => handleSelectLanguage(langItem.code)}
                    className={`w-full p-3 rounded-xl border flex items-center justify-between transition-all cursor-pointer text-left ${
                      isSelected
                        ? isDark
                          ? 'bg-[#14283D] border-[#39B9FF] text-white shadow-[0_0_10px_rgba(57,185,255,0.25)]'
                          : 'bg-[#E0F2FE] border-[#0284C7] text-[#0369A1] font-bold'
                        : isDark
                        ? 'bg-[#0B0F17] border-[#1C2B42] text-[#8D9BB0] hover:text-white hover:border-[#39B9FF]/40'
                        : 'bg-[#F8FAFC] border-[#E2E8F0] text-[#475569] hover:text-[#0F172A] hover:border-[#CBD5E1]'
                    }`}
                  >
                    <div className="flex flex-col">
                      <span className="text-sm font-semibold">{langItem.nativeName}</span>
                      <span className="text-[10px] opacity-75">{langItem.name}</span>
                    </div>

                    {isSelected && (
                      <div className="w-5 h-5 rounded-full bg-[#39B9FF] flex items-center justify-center text-[#0B0F17]">
                        <Check className="w-3.5 h-3.5 stroke-[3]" />
                      </div>
                    )}
                  </button>
                );
              })}
            </div>

            <button
              onClick={() => setIsLangDialogOpen(false)}
              className={`w-full py-2.5 rounded-xl text-xs font-bold transition-all cursor-pointer ${
                isDark
                  ? 'bg-[#1C2B42] hover:bg-[#233854] text-white'
                  : 'bg-[#E2E8F0] hover:bg-[#CBD5E1] text-[#0F172A]'
              }`}
            >
              {t.close}
            </button>
          </div>
        </div>
      )}

      {/* Logout Confirmation Dialog */}
      {showLogoutConfirm && (
        <div className="absolute inset-0 z-50 flex items-center justify-center p-4 bg-black/85 backdrop-blur-md">
          <div
            className={`w-full max-w-xs rounded-2xl p-5 border shadow-2xl ${
              isDark ? 'bg-[#111B2A] border-[#FF4655]/60' : 'bg-white border-[#FF4655]/50'
            }`}
          >
            <h3 className={`text-base font-bold mb-2 ${isDark ? 'text-white' : 'text-[#0F172A]'}`}>
              {t.confirmLogout}
            </h3>
            <p className={`text-xs mb-5 leading-relaxed ${isDark ? 'text-[#8D9BB0]' : 'text-[#64748B]'}`}>
              {t.logoutDesc}
            </p>
            <div className="flex gap-2.5">
              <button
                onClick={() => setShowLogoutConfirm(false)}
                className={`flex-1 py-2.5 rounded-xl text-xs font-bold cursor-pointer ${
                  isDark
                    ? 'bg-[#1C2B42] text-[#8D9BB0] hover:text-white'
                    : 'bg-[#E2E8F0] text-[#64748B] hover:text-[#0F172A]'
                }`}
              >
                {t.cancel}
              </button>
              <button
                onClick={() => {
                  setShowLogoutConfirm(false);
                  onLogout();
                }}
                className="flex-1 py-2.5 rounded-xl bg-[#FF4655] hover:bg-red-600 text-xs font-bold text-white shadow-[0_0_12px_rgba(255,70,85,0.4)] cursor-pointer"
              >
                {t.logOut}
              </button>
            </div>
          </div>
        </div>
      )}

      {/* Privacy Dialog */}
      {activeModal === 'privacy' && (
        <div className="absolute inset-0 z-50 flex items-center justify-center p-4 bg-black/85 backdrop-blur-md">
          <div
            className={`w-full max-w-xs rounded-2xl p-5 border ${
              isDark ? 'bg-[#111B2A] border-[#39B9FF]/50 text-white' : 'bg-white border-[#0284C7]/50 text-[#0F172A]'
            }`}
          >
            <h3 className="text-base font-bold mb-2">{t.privacySecurity}</h3>
            <div className={`text-xs space-y-2 mb-5 leading-relaxed ${isDark ? 'text-[#8D9BB0]' : 'text-[#64748B]'}`}>
              <p>• AimShot Engine uses local device encryption and tokenized activation keys.</p>
              <p>• Zero telemetry, no location tracking, and no invasive permissions requested.</p>
              <p>• Touch emulation and touch guidelines run entirely within isolated system sandbox.</p>
            </div>
            <button
              onClick={() => setActiveModal(null)}
              className="w-full py-2.5 rounded-xl bg-[#39B9FF] text-xs font-bold text-[#0B0F17] cursor-pointer"
            >
              {t.close}
            </button>
          </div>
        </div>
      )}

      {/* About Dialog */}
      {activeModal === 'about' && (
        <div className="absolute inset-0 z-50 flex items-center justify-center p-4 bg-black/85 backdrop-blur-md">
          <div
            className={`w-full max-w-xs rounded-2xl p-5 border text-center ${
              isDark ? 'bg-[#111B2A] border-[#39B9FF]/50 text-white' : 'bg-white border-[#0284C7]/50 text-[#0F172A]'
            }`}
          >
            <h3 className="text-base font-bold">{t.appTitle} {t.appEngine}</h3>
            <span className="text-xs text-[#39B9FF] font-mono block mt-0.5 mb-2">v1.0.0</span>
            <p className={`text-xs mb-2 ${isDark ? 'text-[#8D9BB0]' : 'text-[#64748B]'}`}>{t.appSlogan}</p>
            <p className={`text-[11px] mb-5 font-mono ${isDark ? 'text-[#55657E]' : 'text-[#94A3B8]'}`}>
              Package: com.alpha10.aimshot<br />Native Android Java + Tactical UI Engine
            </p>
            <button
              onClick={() => setActiveModal(null)}
              className="w-full py-2.5 rounded-xl bg-[#39B9FF] text-xs font-bold text-[#0B0F17] cursor-pointer"
            >
              {t.close}
            </button>
          </div>
        </div>
      )}
    </div>
  );
};
