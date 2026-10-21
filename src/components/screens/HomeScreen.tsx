import React, { useState, useEffect } from 'react';
import { Bell, Lock, CheckCircle2, ChevronLeft, ChevronRight } from 'lucide-react';
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
  const [currentSlide, setCurrentSlide] = useState(0);

  const carouselItems = [
    {
      title: 'Auto Aim & Guideline Engine',
      subtitle: 'Engineered for Carrom Disc Pool & Billiards',
      tag: 'FEATURED V1.0',
      gradient: 'from-[#10233D] via-[#111B2A] to-[#0D1624]'
    },
    {
      title: 'Zero Recoil & FPS Stabilization',
      subtitle: 'Ultra low latency 120 FPS game acceleration',
      tag: 'PRO TOOLKIT',
      gradient: 'from-[#0F2838] via-[#111B2A] to-[#121E2F]'
    }
  ];

  useEffect(() => {
    const timer = setInterval(() => {
      setCurrentSlide((prev) => (prev + 1) % carouselItems.length);
    }, 3500);
    return () => clearInterval(timer);
  }, [carouselItems.length]);

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
      <div className="w-full bg-[#111B2A] rounded-2xl p-4 mb-4 border border-[#39B9FF]/50 shadow-[0_0_20px_rgba(57,185,255,0.18)] relative overflow-hidden">
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

      {/* Home Page Image Carousel (Requirement A) */}
      <div className="relative w-full mb-4">
        <div className="w-full h-36 rounded-2xl overflow-hidden border border-[#1C2B42] bg-[#111B2A] relative shadow-lg">
          {carouselItems.map((item, idx) => (
            <div
              key={idx}
              className={`absolute inset-0 p-4 flex flex-col justify-between bg-gradient-to-br ${item.gradient} transition-opacity duration-700 ease-in-out ${
                idx === currentSlide ? 'opacity-100 z-10' : 'opacity-0 z-0'
              }`}
            >
              <div className="flex items-center justify-between">
                <span className="text-[10px] font-mono font-bold px-2 py-0.5 rounded-full bg-[#39B9FF]/20 text-[#39B9FF] border border-[#39B9FF]/40 tracking-wider">
                  {item.tag}
                </span>
                <TacticalReticleLogo size={20} glow={false} />
              </div>

              <div>
                <h4 className="text-base font-bold text-white tracking-tight leading-tight">
                  {item.title}
                </h4>
                <p className="text-xs text-[#8D9BB0] mt-1 font-medium">
                  {item.subtitle}
                </p>
              </div>

              {/* Graphical mini lines */}
              <div className="flex items-center gap-1.5 opacity-60">
                <div className="h-0.5 w-12 bg-[#39B9FF]" />
                <div className="h-0.5 w-4 bg-[#39B9FF]/40" />
                <div className="h-0.5 w-2 bg-[#39B9FF]/20" />
              </div>
            </div>
          ))}

          {/* Nav arrow buttons */}
          <button
            onClick={() => setCurrentSlide((prev) => (prev === 0 ? carouselItems.length - 1 : prev - 1))}
            className="absolute left-1.5 top-1/2 -translate-y-1/2 z-20 w-6 h-6 rounded-full bg-black/40 text-white/70 hover:text-white flex items-center justify-center backdrop-blur-xs"
          >
            <ChevronLeft className="w-3.5 h-3.5" />
          </button>
          <button
            onClick={() => setCurrentSlide((prev) => (prev + 1) % carouselItems.length)}
            className="absolute right-1.5 top-1/2 -translate-y-1/2 z-20 w-6 h-6 rounded-full bg-black/40 text-white/70 hover:text-white flex items-center justify-center backdrop-blur-xs"
          >
            <ChevronRight className="w-3.5 h-3.5" />
          </button>
        </div>

        {/* Carousel Indicator Dots */}
        <div className="flex items-center justify-center gap-1.5 mt-2">
          {carouselItems.map((_, idx) => (
            <button
              key={idx}
              onClick={() => setCurrentSlide(idx)}
              className={`transition-all duration-300 rounded-full ${
                idx === currentSlide
                  ? 'w-5 h-1.5 bg-[#39B9FF]'
                  : 'w-1.5 h-1.5 bg-[#1C2B42] hover:bg-[#8D9BB0]'
              }`}
            />
          ))}
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

      {/* 4-column Grid of Games with Carrom #1 active, 8-Ball #2 locked (Requirement G) */}
      <div className="grid grid-cols-4 gap-2.5 mb-5">
        {SUPPORTED_GAMES.map((game) => {
          const isGameActive = game.isActive === true;
          return (
            <button
              key={game.id}
              onClick={() => onSelectGame(game)}
              className={`flex flex-col items-center p-2 rounded-xl bg-[#111B2A] border border-[#1C2B42] transition-all group ${
                isGameActive
                  ? 'hover:border-[#39B9FF]/50 shadow-[0_0_10px_rgba(57,185,255,0.15)] ring-1 ring-[#39B9FF]/30'
                  : 'opacity-45 hover:opacity-75'
              }`}
            >
              <div
                className={`w-12 h-12 rounded-xl bg-gradient-to-br ${game.iconBg} border border-[#233854] flex items-center justify-center p-1 relative overflow-hidden group-hover:scale-105 transition-transform`}
              >
                <div className="w-full h-full flex flex-col items-center justify-center text-center">
                  <span className="text-[10px] font-black uppercase text-white leading-tight drop-shadow-sm">
                    {game.name.slice(0, 4)}
                  </span>
                  <span
                    className={`text-[8px] font-mono leading-none mt-0.5 ${
                      isGameActive ? 'text-[#39B9FF] font-bold' : 'text-slate-400'
                    }`}
                  >
                    {isGameActive ? 'ACTIVE' : 'LOCKED'}
                  </span>
                </div>

                {/* Status Dot */}
                <div
                  className={`absolute top-1 right-1 w-2 h-2 rounded-full ${
                    isGameActive ? 'bg-[#49DCA0] ring-1 ring-white/50' : 'bg-slate-500'
                  }`}
                />
              </div>

              <span className="text-[10px] text-[#F4F7FC] font-medium mt-1.5 text-center leading-tight line-clamp-2 w-full h-7 flex items-center justify-center">
                {game.name}
              </span>
            </button>
          );
        })}
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
