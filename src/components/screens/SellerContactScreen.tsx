import React from 'react';
import { ArrowLeft, MessageSquare, Send, Mail, ShieldCheck, Zap, ChevronRight } from 'lucide-react';
import { TacticalReticleLogo } from '../TacticalReticleLogo';
import { OFFICIAL_SELLER } from '../../data/mockData';

interface SellerContactScreenProps {
  onBack: () => void;
  onShowToast: (message: string) => void;
}

export const SellerContactScreen: React.FC<SellerContactScreenProps> = ({
  onBack,
  onShowToast
}) => {
  const handleOpenLink = (url: string, label: string) => {
    window.open(url, '_blank');
    onShowToast(`Opening ${label}...`);
  };

  return (
    <div className="w-full h-full flex flex-col bg-[#0B0F17] overflow-y-auto px-4 py-3 select-none">
      {/* Top Action Bar matching Screen 6 */}
      <div className="flex items-center gap-2 mb-4 pt-1">
        <button
          onClick={onBack}
          className="w-9 h-9 rounded-xl bg-[#111B2A] border border-[#1C2B42] flex items-center justify-center text-white hover:text-[#39B9FF] transition-colors"
        >
          <ArrowLeft className="w-5 h-5" />
        </button>
        <h2 className="text-base font-bold text-white tracking-wide">Seller Contact</h2>
      </div>

      {/* Official Seller Card matching Screen 6 */}
      <div className="w-full bg-[#111B2A] rounded-2xl p-5 mb-4 border border-[#39B9FF]/40 shadow-[0_0_20px_rgba(57,185,255,0.12)] flex flex-col items-center text-center relative overflow-hidden">
        {/* Glow ambient background */}
        <div className="w-16 h-16 rounded-2xl bg-[#0B0F17] border border-[#39B9FF]/50 flex items-center justify-center mb-3 shadow-[0_0_15px_rgba(57,185,255,0.3)]">
          <TacticalReticleLogo size={42} glow={false} />
        </div>

        <span className="text-xs text-[#8D9BB0] font-medium">Official Seller</span>
        <h3 className="text-lg font-bold text-white mt-0.5 mb-2">{OFFICIAL_SELLER.name}</h3>

        {/* Online Pill Badge */}
        <div className="flex items-center gap-1.5 px-3 py-1 rounded-full bg-[#49DCA0]/15 border border-[#49DCA0]/30 text-[#49DCA0] text-xs font-bold mb-1">
          <span className="w-2 h-2 rounded-full bg-[#49DCA0] animate-pulse" />
          <span>Online</span>
        </div>

        <span className="text-[11px] text-[#8D9BB0]">Trusted & Verified Partner</span>
      </div>

      {/* Contact Channels List matching Screen 6 */}
      <div className="space-y-2.5 mb-5">
        {/* WhatsApp */}
        <button
          onClick={() => handleOpenLink('https://wa.me/8801700111222', 'WhatsApp')}
          className="w-full p-3.5 rounded-2xl bg-[#111B2A] border border-[#1C2B42] hover:border-[#49DCA0]/50 transition-all flex items-center justify-between group cursor-pointer"
        >
          <div className="flex items-center gap-3">
            <div className="w-10 h-10 rounded-xl bg-[#49DCA0]/15 flex items-center justify-center text-[#49DCA0]">
              <MessageSquare className="w-5 h-5" />
            </div>
            <div className="flex flex-col text-left">
              <span className="text-xs text-[#8D9BB0]">WhatsApp</span>
              <span className="text-sm font-bold text-white group-hover:text-[#49DCA0] transition-colors font-mono">
                {OFFICIAL_SELLER.whatsapp}
              </span>
            </div>
          </div>
          <ChevronRight className="w-5 h-5 text-[#8D9BB0] group-hover:translate-x-0.5 transition-transform" />
        </button>

        {/* Telegram */}
        <button
          onClick={() => handleOpenLink('https://t.me/aimshot_support', 'Telegram')}
          className="w-full p-3.5 rounded-2xl bg-[#111B2A] border border-[#1C2B42] hover:border-[#39B9FF]/50 transition-all flex items-center justify-between group cursor-pointer"
        >
          <div className="flex items-center gap-3">
            <div className="w-10 h-10 rounded-xl bg-[#39B9FF]/15 flex items-center justify-center text-[#39B9FF]">
              <Send className="w-5 h-5" />
            </div>
            <div className="flex flex-col text-left">
              <span className="text-xs text-[#8D9BB0]">Telegram</span>
              <span className="text-sm font-bold text-white group-hover:text-[#39B9FF] transition-colors font-mono">
                {OFFICIAL_SELLER.telegram}
              </span>
            </div>
          </div>
          <ChevronRight className="w-5 h-5 text-[#8D9BB0] group-hover:translate-x-0.5 transition-transform" />
        </button>

        {/* Email */}
        <button
          onClick={() => handleOpenLink('mailto:aimshot@support.com', 'Email Client')}
          className="w-full p-3.5 rounded-2xl bg-[#111B2A] border border-[#1C2B42] hover:border-[#39B9FF]/50 transition-all flex items-center justify-between group cursor-pointer"
        >
          <div className="flex items-center gap-3">
            <div className="w-10 h-10 rounded-xl bg-[#39B9FF]/15 flex items-center justify-center text-[#39B9FF]">
              <Mail className="w-5 h-5" />
            </div>
            <div className="flex flex-col text-left">
              <span className="text-xs text-[#8D9BB0]">Email</span>
              <span className="text-sm font-bold text-white group-hover:text-[#39B9FF] transition-colors font-mono">
                {OFFICIAL_SELLER.email}
              </span>
            </div>
          </div>
          <ChevronRight className="w-5 h-5 text-[#8D9BB0] group-hover:translate-x-0.5 transition-transform" />
        </button>
      </div>

      {/* Trust Badges matching Screen 6 */}
      <div className="space-y-2 mb-5 px-1">
        <div className="flex items-center gap-2.5 text-xs text-[#8D9BB0]">
          <Zap className="w-4 h-4 text-[#39B9FF]" />
          <span>Fast Response & 24/7 Activation</span>
        </div>
        <div className="flex items-center gap-2.5 text-xs text-[#8D9BB0]">
          <ShieldCheck className="w-4 h-4 text-[#49DCA0]" />
          <span>Secure Payment & Verified Key Delivery</span>
        </div>
        <div className="flex items-center gap-2.5 text-xs text-[#8D9BB0]">
          <ShieldCheck className="w-4 h-4 text-[#39B9FF]" />
          <span>100% Trusted Official License Provider</span>
        </div>
      </div>

      {/* Contact Now Action Button matching Screen 6 */}
      <button
        onClick={() => handleOpenLink('https://t.me/aimshot_support', 'AimShot Support')}
        className="w-full py-3 rounded-xl bg-[#39B9FF] hover:bg-[#2da5e6] text-[#0B0F17] font-bold text-sm tracking-wide transition-all shadow-[0_0_15px_rgba(57,185,255,0.4)] active:scale-[0.98] cursor-pointer"
      >
        Contact Now
      </button>
    </div>
  );
};
