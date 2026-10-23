import React from 'react';
import { Crown, Bell, CheckCircle2, Key, ShoppingBag, ExternalLink } from 'lucide-react';
import { TacticalReticleLogo } from '../TacticalReticleLogo';
import { MembershipState, AppTheme } from '../../types';
import { Translations } from '../../data/translations';

interface PremiumScreenProps {
  membership: MembershipState;
  theme: AppTheme;
  t: Translations;
  unreadNotificationsCount?: number;
  onOpenNotifications: () => void;
  onUpgradeRenew: () => void;
  onShowToast: (msg: string) => void;
}

export const PremiumScreen: React.FC<PremiumScreenProps> = ({
  membership,
  theme,
  t,
  unreadNotificationsCount = 0,
  onOpenNotifications,
  onUpgradeRenew,
  onShowToast
}) => {
  const isDark = theme === 'dark';
  const isActive = membership.status === 'active';

  const handleOpenFreeKey = () => {
    onShowToast('Opening AimShot Free Key verification portal...');
    try {
      window.open('https://aimshotengine.com/free-key', '_blank');
    } catch {
      // safe fallback
    }
  };

  return (
    <div
      className={`w-full h-full flex flex-col overflow-y-auto px-4 py-3 select-none transition-colors duration-200 ${
        isDark ? 'bg-[#0B0F17] text-[#F4F7FC]' : 'bg-[#F1F5F9] text-[#0F172A]'
      }`}
    >
      {/* Top Header Bar */}
      <div className="flex items-center justify-between mb-4 pt-1">
        <div className="flex items-center gap-2.5">
          <TacticalReticleLogo size={36} />
          <div className="flex flex-col">
            <div className="flex items-baseline gap-1 leading-none">
              <span className={`text-base font-bold ${isDark ? 'text-white' : 'text-[#0F172A]'}`}>
                {t.appTitle}
              </span>
              <span className="text-base font-bold text-[#39B9FF]">{t.appEngine}</span>
            </div>
            <span
              className={`text-[10px] tracking-tight mt-0.5 font-medium ${
                isDark ? 'text-[#8D9BB0]' : 'text-[#64748B]'
              }`}
            >
              {t.toolkitBadge}
            </span>
          </div>
        </div>

        {/* Top Notification Icon (Replaced Settings icon per Requirement 9) */}
        <button
          onClick={onOpenNotifications}
          className={`w-9 h-9 rounded-xl flex items-center justify-center transition-colors relative cursor-pointer ${
            isDark
              ? 'bg-[#111B2A] border border-[#1C2B42] text-[#8D9BB0] hover:text-[#39B9FF]'
              : 'bg-white border border-[#CBD5E1] text-[#64748B] hover:text-[#0284C7] shadow-xs'
          }`}
          title="Notifications & Announcements"
        >
          <Bell className="w-4 h-4" />
          {unreadNotificationsCount > 0 ? (
            <span className="absolute -top-1 -right-1 min-w-[17px] h-[17px] px-1 rounded-full bg-[#39B9FF] text-[#0B0F17] text-[10px] font-black flex items-center justify-center shadow-[0_0_8px_rgba(57,185,255,0.7)] border border-[#0B0F17]">
              {unreadNotificationsCount > 9 ? '9+' : unreadNotificationsCount}
            </span>
          ) : (
            <span className="absolute top-2 right-2 w-1.5 h-1.5 rounded-full bg-[#39B9FF]/40" />
          )}
        </button>
      </div>

      {/* Premium Status Hero Card */}
      <div
        className={`w-full rounded-2xl p-4 mb-3.5 border transition-all ${
          isDark
            ? 'bg-[#111B2A] border-[#1C2B42]'
            : 'bg-white border-[#CBD5E1] shadow-sm'
        }`}
      >
        <div className="flex items-center justify-between mb-3.5">
          <div className="flex items-center gap-3">
            {/* Gold Crown Container */}
            <div
              className={`w-11 h-11 rounded-xl flex items-center justify-center border ${
                isDark
                  ? 'bg-[#F59E0B]/15 border-[#F59E0B]/30 text-[#F59E0B]'
                  : 'bg-[#FEF3C7] border-[#F59E0B]/40 text-[#D97706]'
              }`}
            >
              <Crown className="w-6 h-6 fill-[#F59E0B]/30 text-[#F59E0B]" />
            </div>
            <span className={`text-base font-bold ${isDark ? 'text-white' : 'text-[#0F172A]'}`}>
              {t.premiumStatus}
            </span>
          </div>

          {/* Status Badge */}
          <span
            className={`px-3 py-1 rounded-full text-[11px] font-bold ${
              isActive
                ? 'bg-[#49DCA0] text-[#0B0F17]'
                : membership.status === 'expired'
                ? 'bg-[#FF4655] text-white'
                : isDark
                ? 'bg-[#1C2B42] text-[#8D9BB0]'
                : 'bg-[#E2E8F0] text-[#64748B]'
            }`}
          >
            {isActive ? t.statusActive : membership.status === 'expired' ? t.statusExpired : t.statusFree}
          </span>
        </div>

        {/* Expires In & Countdown */}
        <span className={`text-xs font-medium block ${isDark ? 'text-[#8D9BB0]' : 'text-[#64748B]'}`}>
          {t.expiresIn}
        </span>
        <div className="flex items-baseline gap-2 mt-1 mb-3">
          <span className="text-2xl font-black text-[#39B9FF] tracking-wide">
            {isActive ? `${membership.daysRemaining} ${t.days}` : `0 ${t.days}`}
          </span>
          <span className={`text-xs ${isDark ? 'text-[#8D9BB0]' : 'text-[#64748B]'}`}>
            ( {isActive ? membership.expiryDate : t.notActivated} )
          </span>
        </div>

        {/* Progress Bar */}
        <div
          className={`w-full h-2 rounded-full overflow-hidden border mb-3 ${
            isDark ? 'bg-[#0B0F17] border-[#1C2B42]' : 'bg-[#E2E8F0] border-[#CBD5E1]'
          }`}
        >
          <div
            className="h-full bg-gradient-to-r from-[#1A85CC] to-[#39B9FF] rounded-full transition-all duration-500 shadow-[0_0_8px_#39B9FF]"
            style={{ width: isActive ? `${Math.min(100, membership.daysRemaining * 3.5)}%` : '0%' }}
          />
        </div>

        {/* Features Unlocked Line */}
        <div className="flex items-center gap-1.5 pt-0.5">
          <CheckCircle2
            className={`w-4 h-4 ${isActive ? 'text-[#49DCA0]' : isDark ? 'text-[#55657E]' : 'text-[#94A3B8]'}`}
          />
          <span
            className={`text-xs font-semibold ${
              isActive ? 'text-[#49DCA0]' : isDark ? 'text-[#8D9BB0]' : 'text-[#64748B]'
            }`}
          >
            {isActive ? t.featuresUnlocked : t.featuresLocked}
          </span>
        </div>
      </div>

      {/* Get Activation Key Section */}
      <div
        className={`w-full rounded-2xl p-4 mb-3.5 border transition-all ${
          isDark
            ? 'bg-[#111B2A] border-[#1C2B42]'
            : 'bg-white border-[#CBD5E1] shadow-sm'
        }`}
      >
        <div className="flex items-center gap-2 mb-3">
          <Key className="w-4 h-4 text-[#39B9FF]" />
          <h3 className={`text-sm font-bold tracking-wide ${isDark ? 'text-white' : 'text-[#0F172A]'}`}>
            {t.getActivationKey}
          </h3>
        </div>

        <div className="grid grid-cols-2 gap-2.5">
          {/* Free Key Button */}
          <button
            onClick={handleOpenFreeKey}
            className={`flex items-center justify-center gap-2 py-3 px-2 rounded-xl border transition-all group active:scale-[0.98] cursor-pointer ${
              isDark
                ? 'bg-[#0B0F17] border-[#39B9FF]/40 hover:border-[#39B9FF] text-white hover:text-[#39B9FF]'
                : 'bg-[#F0F9FF] border-[#39B9FF]/50 hover:border-[#0284C7] text-[#0369A1] hover:text-[#0284C7]'
            }`}
          >
            <ExternalLink className="w-4 h-4 text-[#39B9FF] group-hover:scale-110 transition-transform" />
            <span className="text-xs font-bold">{t.getFreeKey}</span>
          </button>

          {/* Paid Key Button */}
          <button
            onClick={onUpgradeRenew}
            className={`flex items-center justify-center gap-2 py-3 px-2 rounded-xl border transition-all group active:scale-[0.98] cursor-pointer ${
              isDark
                ? 'bg-[#0B0F17] border-[#F59E0B]/40 hover:border-[#F59E0B] text-white hover:text-[#F59E0B]'
                : 'bg-[#FFFBEB] border-[#F59E0B]/50 hover:border-[#D97706] text-[#B45309] hover:text-[#D97706]'
            }`}
          >
            <ShoppingBag className="w-4 h-4 text-[#F59E0B] group-hover:scale-110 transition-transform" />
            <span className="text-xs font-bold">{t.getPaidKey}</span>
          </button>
        </div>
      </div>

      {/* Membership Details Card */}
      <div
        className={`w-full rounded-2xl p-4 mb-3.5 border transition-all ${
          isDark
            ? 'bg-[#111B2A] border-[#1C2B42]'
            : 'bg-white border-[#CBD5E1] shadow-sm'
        }`}
      >
        <h3 className={`text-sm font-bold mb-3 tracking-wide ${isDark ? 'text-white' : 'text-[#0F172A]'}`}>
          {t.membershipDetails}
        </h3>

        <div
          className={`divide-y text-xs ${
            isDark ? 'divide-[#1C2B42]/60' : 'divide-[#E2E8F0]'
          }`}
        >
          <div className="flex justify-between py-2.5">
            <span className={isDark ? 'text-[#8D9BB0]' : 'text-[#64748B]'}>{t.planType}</span>
            <span className={`font-bold ${isDark ? 'text-white' : 'text-[#0F172A]'}`}>
              {membership.planType}
            </span>
          </div>

          <div className="flex justify-between py-2.5">
            <span className={isDark ? 'text-[#8D9BB0]' : 'text-[#64748B]'}>{t.startDate}</span>
            <span className={`font-mono ${isDark ? 'text-white' : 'text-[#0F172A]'}`}>
              {membership.startDate}
            </span>
          </div>

          <div className="flex justify-between py-2.5">
            <span className={isDark ? 'text-[#8D9BB0]' : 'text-[#64748B]'}>{t.expiryDate}</span>
            <span className={`font-mono ${isDark ? 'text-white' : 'text-[#0F172A]'}`}>
              {membership.expiryDate}
            </span>
          </div>

          <div className="flex justify-between py-2.5">
            <span className={isDark ? 'text-[#8D9BB0]' : 'text-[#64748B]'}>{t.daysRemaining}</span>
            <span className="text-[#39B9FF] font-bold font-mono">
              {membership.daysRemaining} {t.days}
            </span>
          </div>
        </div>
      </div>

      {/* Upgrade / Renew Button */}
      <button
        onClick={onUpgradeRenew}
        className="w-full py-3 rounded-xl bg-transparent border-2 border-[#F59E0B] text-[#F59E0B] hover:bg-[#F59E0B]/10 active:scale-[0.98] font-bold text-sm tracking-wide transition-all shadow-[0_0_12px_rgba(245,158,11,0.15)] flex items-center justify-center gap-2 mb-2 cursor-pointer"
      >
        <span>{t.upgradeRenew}</span>
      </button>
    </div>
  );
};
