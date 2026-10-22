import React from 'react';
import { ArrowLeft, MessageSquare, Send, Mail, ShieldCheck, Zap, ChevronRight } from 'lucide-react';
import { TacticalReticleLogo } from '../TacticalReticleLogo';
import { OFFICIAL_SELLER } from '../../data/mockData';
import { AppTheme } from '../../types';
import { Translations } from '../../data/translations';

interface SellerContactScreenProps {
  theme: AppTheme;
  t: Translations;
  onBack: () => void;
  onShowToast: (message: string) => void;
}

export const SellerContactScreen: React.FC<SellerContactScreenProps> = ({
  theme,
  t,
  onBack,
  onShowToast
}) => {
  const isDark = theme === 'dark';

  const handleOpenLink = (url: string, label: string) => {
    window.open(url, '_blank');
    onShowToast(`Opening ${label}...`);
  };

  return (
    <div
      className={`w-full h-full flex flex-col overflow-y-auto px-4 py-3 select-none transition-colors duration-200 ${
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
          {t.sellerTitle}
        </h2>
      </div>

      {/* Official Seller Card */}
      <div
        className={`w-full rounded-2xl p-5 mb-4 border flex flex-col items-center text-center relative overflow-hidden transition-all ${
          isDark
            ? 'bg-[#111B2A] border-[#39B9FF]/40 shadow-[0_0_20px_rgba(57,185,255,0.12)]'
            : 'bg-white border-[#CBD5E1] shadow-sm'
        }`}
      >
        <div
          className={`w-16 h-16 rounded-2xl border flex items-center justify-center mb-3 shadow-md ${
            isDark ? 'bg-[#0B0F17] border-[#39B9FF]/50' : 'bg-[#E0F2FE] border-[#39B9FF]/40'
          }`}
        >
          <TacticalReticleLogo size={42} glow={false} />
        </div>

        <span className={`text-xs font-medium ${isDark ? 'text-[#8D9BB0]' : 'text-[#64748B]'}`}>
          {t.officialDistributor}
        </span>
        <h3 className={`text-lg font-bold mt-0.5 mb-2 ${isDark ? 'text-white' : 'text-[#0F172A]'}`}>
          {OFFICIAL_SELLER.name}
        </h3>

        {/* Online Pill Badge */}
        <div className="flex items-center gap-1.5 px-3 py-1 rounded-full bg-[#49DCA0]/15 border border-[#49DCA0]/30 text-[#49DCA0] text-xs font-bold mb-1">
          <span className="w-2 h-2 rounded-full bg-[#49DCA0] animate-pulse" />
          <span>Online 24/7</span>
        </div>

        <span className={`text-[11px] ${isDark ? 'text-[#8D9BB0]' : 'text-[#64748B]'}`}>
          {t.trustedSeller}
        </span>
      </div>

      {/* Contact Channels List */}
      <div className="space-y-2.5 mb-5">
        {/* WhatsApp */}
        <button
          onClick={() => handleOpenLink('https://wa.me/8801700111222', 'WhatsApp')}
          className={`w-full p-3.5 rounded-2xl border transition-all flex items-center justify-between group cursor-pointer ${
            isDark
              ? 'bg-[#111B2A] border-[#1C2B42] hover:border-[#49DCA0]/50'
              : 'bg-white border-[#CBD5E1] hover:border-[#49DCA0]/60 shadow-xs'
          }`}
        >
          <div className="flex items-center gap-3">
            <div className="w-10 h-10 rounded-xl bg-[#49DCA0]/15 flex items-center justify-center text-[#49DCA0]">
              <MessageSquare className="w-5 h-5" />
            </div>
            <div className="flex flex-col text-left">
              <span className={`text-xs ${isDark ? 'text-[#8D9BB0]' : 'text-[#64748B]'}`}>
                {t.whatsappSupport}
              </span>
              <span
                className={`text-sm font-bold group-hover:text-[#49DCA0] transition-colors font-mono ${
                  isDark ? 'text-white' : 'text-[#0F172A]'
                }`}
              >
                {OFFICIAL_SELLER.whatsapp}
              </span>
            </div>
          </div>
          <ChevronRight className={`w-5 h-5 group-hover:translate-x-0.5 transition-transform ${isDark ? 'text-[#8D9BB0]' : 'text-[#94A3B8]'}`} />
        </button>

        {/* Telegram */}
        <button
          onClick={() => handleOpenLink('https://t.me/aimshot_support', 'Telegram')}
          className={`w-full p-3.5 rounded-2xl border transition-all flex items-center justify-between group cursor-pointer ${
            isDark
              ? 'bg-[#111B2A] border-[#1C2B42] hover:border-[#39B9FF]/50'
              : 'bg-white border-[#CBD5E1] hover:border-[#0284C7]/60 shadow-xs'
          }`}
        >
          <div className="flex items-center gap-3">
            <div className="w-10 h-10 rounded-xl bg-[#39B9FF]/15 flex items-center justify-center text-[#39B9FF]">
              <Send className="w-5 h-5" />
            </div>
            <div className="flex flex-col text-left">
              <span className={`text-xs ${isDark ? 'text-[#8D9BB0]' : 'text-[#64748B]'}`}>
                {t.telegramChannel}
              </span>
              <span
                className={`text-sm font-bold group-hover:text-[#39B9FF] transition-colors font-mono ${
                  isDark ? 'text-white' : 'text-[#0F172A]'
                }`}
              >
                {OFFICIAL_SELLER.telegram}
              </span>
            </div>
          </div>
          <ChevronRight className={`w-5 h-5 group-hover:translate-x-0.5 transition-transform ${isDark ? 'text-[#8D9BB0]' : 'text-[#94A3B8]'}`} />
        </button>

        {/* Email */}
        <button
          onClick={() => handleOpenLink('mailto:aimshot@support.com', 'Email Client')}
          className={`w-full p-3.5 rounded-2xl border transition-all flex items-center justify-between group cursor-pointer ${
            isDark
              ? 'bg-[#111B2A] border-[#1C2B42] hover:border-[#39B9FF]/50'
              : 'bg-white border-[#CBD5E1] hover:border-[#0284C7]/60 shadow-xs'
          }`}
        >
          <div className="flex items-center gap-3">
            <div className="w-10 h-10 rounded-xl bg-[#39B9FF]/15 flex items-center justify-center text-[#39B9FF]">
              <Mail className="w-5 h-5" />
            </div>
            <div className="flex flex-col text-left">
              <span className={`text-xs ${isDark ? 'text-[#8D9BB0]' : 'text-[#64748B]'}`}>
                {t.emailSupport}
              </span>
              <span
                className={`text-sm font-bold group-hover:text-[#39B9FF] transition-colors font-mono ${
                  isDark ? 'text-white' : 'text-[#0F172A]'
                }`}
              >
                {OFFICIAL_SELLER.email}
              </span>
            </div>
          </div>
          <ChevronRight className={`w-5 h-5 group-hover:translate-x-0.5 transition-transform ${isDark ? 'text-[#8D9BB0]' : 'text-[#94A3B8]'}`} />
        </button>
      </div>

      {/* Trust Badges */}
      <div className="space-y-2 mb-5 px-1">
        <div className={`flex items-center gap-2.5 text-xs ${isDark ? 'text-[#8D9BB0]' : 'text-[#64748B]'}`}>
          <Zap className="w-4 h-4 text-[#39B9FF]" />
          <span>Fast 24/7 Activation & Instant Delivery</span>
        </div>
        <div className={`flex items-center gap-2.5 text-xs ${isDark ? 'text-[#8D9BB0]' : 'text-[#64748B]'}`}>
          <ShieldCheck className="w-4 h-4 text-[#49DCA0]" />
          <span>100% Anti-Ban Guarantee & Security Shield</span>
        </div>
      </div>

      {/* Contact Now Action Button */}
      <button
        onClick={() => handleOpenLink('https://t.me/aimshot_support', 'AimShot Support')}
        className="w-full py-3 rounded-xl bg-[#39B9FF] hover:bg-[#2da5e6] text-[#0B0F17] font-bold text-sm tracking-wide transition-all shadow-[0_0_15px_rgba(57,185,255,0.4)] active:scale-[0.98] cursor-pointer"
      >
        Contact Seller Now
      </button>
    </div>
  );
};
