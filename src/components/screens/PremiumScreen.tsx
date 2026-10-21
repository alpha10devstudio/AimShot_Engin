import React from 'react';
import { Crown, Settings, CheckCircle2 } from 'lucide-react';
import { TacticalReticleLogo } from '../TacticalReticleLogo';
import { MembershipState } from '../../types';

interface PremiumScreenProps {
  membership: MembershipState;
  onOpenSettings: () => void;
  onUpgradeRenew: () => void;
}

export const PremiumScreen: React.FC<PremiumScreenProps> = ({
  membership,
  onOpenSettings,
  onUpgradeRenew
}) => {
  const isActive = membership.status === 'active';

  return (
    <div className="w-full h-full flex flex-col bg-[#0B0F17] overflow-y-auto px-4 py-3 select-none">
      {/* Top Header Bar */}
      <div className="flex items-center justify-between mb-4 pt-1">
        <div className="flex items-center gap-2.5">
          <TacticalReticleLogo size={36} />
          <div className="flex flex-col">
            <div className="flex items-baseline gap-1 leading-none">
              <span className="text-base font-bold text-white">AimShot</span>
              <span className="text-base font-bold text-[#39B9FF]">Engine</span>
            </div>
            <span className="text-[10px] text-[#8D9BB0] tracking-tight mt-0.5 font-medium">
              Pro Gaming Toolkit
            </span>
          </div>
        </div>

        <button
          onClick={onOpenSettings}
          className="w-9 h-9 rounded-xl bg-[#111B2A] border border-[#1C2B42] flex items-center justify-center text-[#8D9BB0] hover:text-[#39B9FF] transition-colors"
          title="Settings"
        >
          <Settings className="w-4 h-4" />
        </button>
      </div>

      {/* Premium Status Hero Card matching Screen 4 */}
      <div className="w-full bg-[#111B2A] rounded-2xl p-4 mb-4 border border-[#1C2B42]">
        <div className="flex items-center justify-between mb-4">
          <div className="flex items-center gap-3">
            {/* Gold Crown in Rounded Dark Gold Container */}
            <div className="w-11 h-11 rounded-xl bg-[#F59E0B]/15 border border-[#F59E0B]/30 flex items-center justify-center text-[#F59E0B]">
              <Crown className="w-6 h-6 fill-[#F59E0B]/30 text-[#F59E0B]" />
            </div>
            <span className="text-base font-bold text-white">Premium Status</span>
          </div>

          {/* Status Badge */}
          <span
            className={`px-3 py-1 rounded-full text-[11px] font-bold ${
              isActive
                ? 'bg-[#49DCA0] text-[#0B0F17]'
                : membership.status === 'expired'
                ? 'bg-[#FF4655] text-white'
                : 'bg-[#1C2B42] text-[#8D9BB0]'
            }`}
          >
            {isActive ? 'Active' : membership.status === 'expired' ? 'Expired' : 'Free'}
          </span>
        </div>

        {/* Expires In & Countdown */}
        <span className="text-xs text-[#8D9BB0] font-medium block">Expires In</span>
        <div className="flex items-baseline gap-2 mt-1 mb-3">
          <span className="text-2xl font-black text-[#39B9FF] tracking-wide">
            {isActive ? `${membership.daysRemaining} Days` : '0 Days'}
          </span>
          <span className="text-xs text-[#8D9BB0]">
            ( {isActive ? membership.expiryDate : 'Not Activated'} )
          </span>
        </div>

        {/* Progress Bar */}
        <div className="w-full h-2 bg-[#0B0F17] rounded-full overflow-hidden border border-[#1C2B42] mb-3">
          <div
            className="h-full bg-gradient-to-r from-[#1A85CC] to-[#39B9FF] rounded-full transition-all duration-500 shadow-[0_0_8px_#39B9FF]"
            style={{ width: isActive ? `${Math.min(100, membership.daysRemaining * 3.5)}%` : '0%' }}
          />
        </div>

        {/* Features Unlocked Line */}
        <div className="flex items-center gap-1.5 pt-1">
          <CheckCircle2
            className={`w-4 h-4 ${isActive ? 'text-[#49DCA0]' : 'text-[#55657E]'}`}
          />
          <span
            className={`text-xs font-semibold ${
              isActive ? 'text-[#49DCA0]' : 'text-[#8D9BB0]'
            }`}
          >
            {isActive ? 'Premium Features Unlocked' : 'Premium Features Locked'}
          </span>
        </div>
      </div>

      {/* Membership Details Card matching Screen 4 */}
      <div className="w-full bg-[#111B2A] rounded-2xl p-4 mb-5 border border-[#1C2B42]">
        <h3 className="text-sm font-bold text-white mb-3 tracking-wide">Membership Details</h3>

        <div className="divide-y divide-[#1C2B42]/50 text-xs">
          <div className="flex justify-between py-2.5">
            <span className="text-[#8D9BB0]">Plan Type</span>
            <span className="text-white font-bold">{membership.planType}</span>
          </div>

          <div className="flex justify-between py-2.5">
            <span className="text-[#8D9BB0]">Start Date</span>
            <span className="text-white font-mono">{membership.startDate}</span>
          </div>

          <div className="flex justify-between py-2.5">
            <span className="text-[#8D9BB0]">Expiry Date</span>
            <span className="text-white font-mono">{membership.expiryDate}</span>
          </div>

          <div className="flex justify-between py-2.5">
            <span className="text-[#8D9BB0]">Days Remaining</span>
            <span className="text-[#39B9FF] font-bold font-mono">
              {membership.daysRemaining} Days
            </span>
          </div>
        </div>
      </div>

      {/* Upgrade / Renew Button with Gold Outline matching Screen 4 */}
      <button
        onClick={onUpgradeRenew}
        className="w-full py-3 rounded-xl bg-transparent border-2 border-[#F59E0B] text-[#F59E0B] hover:bg-[#F59E0B]/10 active:scale-[0.98] font-bold text-sm tracking-wide transition-all shadow-[0_0_12px_rgba(245,158,11,0.15)] flex items-center justify-center gap-2"
      >
        <span>Upgrade / Renew</span>
      </button>
    </div>
  );
};
