import React from 'react';
import { Bell, Lock, CheckCircle2 } from 'lucide-react';
import { TacticalReticleLogo } from '../TacticalReticleLogo';
import { SUPPORTED_GAMES } from '../../data/mockData';
import { MembershipState, Game } from '../../types';

interface HomeScreenProps {
  membership: MembershipState;
  onOpenLoginModal: () => void;
  onViewAllGames: () => void;
  onOpenNotifications: () => void;
  onNavigateToPremium: () => void;
  onSelectGame: (game: Game) => void;
}

export const HomeScreen: React.FC<HomeScreenProps> = ({
  membership,
  onOpenLoginModal,
  onViewAllGames,
  onOpenNotifications,
  onNavigateToPremium,
  onSelectGame
}) => {
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
              Sharper Aim • Better Game • Higher You
            </span>
          </div>
        </div>

        <button
          onClick={onOpenNotifications}
          className="w-9 h-9 rounded-xl bg-[#111B2A] border border-[#1C2B42] flex items-center justify-center text-[#8D9BB0] hover:text-[#39B9FF] transition-colors relative"
          title="Seller & Support"
        >
          <Bell className="w-4 h-4" />
          <span className="absolute top-2 right-2 w-2 h-2 rounded-full bg-[#39B9FF]" />
        </button>
      </div>

      {/* Hero Banner Card: Pro Gaming Toolkit */}
      <div className="w-full bg-[#111B2A] rounded-2xl p-4 mb-5 border border-[#39B9FF]/50 shadow-[0_0_20px_rgba(57,185,255,0.18)] relative overflow-hidden">
        {/* Subtle background glow circle */}
        <div className="absolute -right-8 -bottom-8 w-32 h-32 rounded-full bg-[#39B9FF]/10 blur-xl pointer-events-none" />

        <div className="flex items-center gap-3.5 relative z-10">
          <div className="w-12 h-12 rounded-xl bg-[#0B0F17] border border-[#39B9FF]/30 flex items-center justify-center shrink-0">
            <TacticalReticleLogo size={34} glow={false} />
          </div>

          <div className="flex flex-col">
            <div className="flex items-baseline gap-1.5">
              <span className="text-lg font-bold text-white">AimShot</span>
              <span className="text-lg font-bold text-[#39B9FF]">Engine</span>
            </div>
            <span className="text-xs text-[#8D9BB0] font-medium">Pro Gaming Toolkit</span>
          </div>
        </div>
      </div>

      {/* Supported Games Section Header */}
      <div className="flex items-center justify-between mb-3">
        <div className="flex items-center gap-2">
          <TacticalReticleLogo size={18} glow={false} />
          <span className="text-sm font-bold text-white tracking-wide">Supported Games</span>
        </div>
        <button
          onClick={onViewAllGames}
          className="text-xs font-semibold text-[#39B9FF] hover:underline"
        >
          View All
        </button>
      </div>

      {/* 4-column Grid of Games (8 games from Mockup) */}
      <div className="grid grid-cols-4 gap-2.5 mb-5">
        {SUPPORTED_GAMES.map((game) => (
          <button
            key={game.id}
            onClick={() => onSelectGame(game)}
            className="flex flex-col items-center p-2 rounded-xl bg-[#111B2A] border border-[#1C2B42] hover:border-[#39B9FF]/40 transition-all group"
          >
            <div className="w-12 h-12 rounded-xl bg-gradient-to-br from-[#16253A] to-[#0D1623] border border-[#233854] flex items-center justify-center p-1 relative overflow-hidden group-hover:scale-105 transition-transform">
              {/* Tactical reticle icon or initial badge */}
              <div className="w-full h-full flex flex-col items-center justify-center text-center">
                <span className="text-[10px] font-black uppercase text-[#39B9FF] leading-tight">
                  {game.name.slice(0, 4)}
                </span>
                <span className="text-[8px] text-[#8D9BB0] font-mono leading-none mt-0.5">
                  PRO
                </span>
              </div>

              {/* Verified check indicator */}
              <div className="absolute top-1 right-1 w-2 h-2 rounded-full bg-[#49DCA0]" />
            </div>

            <span className="text-[10px] text-[#F4F7FC] font-medium mt-1.5 text-center leading-tight line-clamp-2 w-full h-7 flex items-center justify-center">
              {game.name}
            </span>
          </button>
        ))}
      </div>

      {/* Login / License Status Card matching Mockup */}
      <div className="w-full bg-[#111B2A] rounded-2xl p-4 mb-3 border border-[#1C2B42]">
        {membership.status === 'active' ? (
          <div className="flex flex-col">
            <div className="flex items-center gap-3 mb-1.5">
              <div className="w-9 h-9 rounded-xl bg-[#49DCA0]/15 flex items-center justify-center text-[#49DCA0]">
                <CheckCircle2 className="w-5 h-5" />
              </div>
              <div className="flex flex-col">
                <span className="text-sm font-bold text-white">
                  License Active ({membership.daysRemaining} Days Left)
                </span>
                <span className="text-xs text-[#49DCA0] font-medium">
                  {membership.planType} • Verified
                </span>
              </div>
            </div>

            <p className="text-xs text-[#8D9BB0] mt-1 mb-3.5">
              Pro gaming features and hardware optimization are active on this device.
            </p>

            <button
              onClick={onNavigateToPremium}
              className="w-full py-2.5 rounded-xl bg-[#39B9FF] hover:bg-[#2da5e6] text-[#0B0F17] font-bold text-xs uppercase tracking-wider transition-all shadow-[0_0_15px_rgba(57,185,255,0.4)]"
            >
              View License Details
            </button>
          </div>
        ) : (
          <div className="flex flex-col">
            <div className="flex items-center gap-3 mb-1.5">
              <div className="w-9 h-9 rounded-xl bg-[#39B9FF]/15 border border-[#39B9FF]/30 flex items-center justify-center text-[#39B9FF]">
                <Lock className="w-4 h-4" />
              </div>
              <span className="text-sm font-bold text-white">You are not logged in</span>
            </div>

            <p className="text-xs text-[#8D9BB0] mt-1 mb-3.5">
              Login to access premium features and full tools.
            </p>

            <button
              onClick={onOpenLoginModal}
              className="w-full py-2.5 rounded-xl bg-[#39B9FF] hover:bg-[#2da5e6] text-[#0B0F17] font-bold text-xs uppercase tracking-wider transition-all shadow-[0_0_15px_rgba(57,185,255,0.4)] active:scale-[0.98]"
            >
              Login Now
            </button>
          </div>
        )}
      </div>
    </div>
  );
};
