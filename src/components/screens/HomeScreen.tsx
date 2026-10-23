import React, { useState, useEffect } from 'react';
import { Bell, Lock, Play, ChevronLeft, ChevronRight, CheckCircle2, ShieldCheck, Gamepad2 } from 'lucide-react';
import { TacticalReticleLogo } from '../TacticalReticleLogo';
import { SUPPORTED_GAMES } from '../../data/mockData';
import { MembershipState, Game, AppTheme } from '../../types';
import { Translations } from '../../data/translations';

interface HomeScreenProps {
  membership: MembershipState;
  theme: AppTheme;
  t: Translations;
  selectedGame: Game;
  unreadNotificationsCount?: number;
  onSelectGame: (game: Game) => void;
  onOpenLoginModal: () => void;
  onViewAllGames: () => void;
  onOpenNotifications: () => void;
  onNavigateToPremium: () => void;
  onLaunchGame: (game: Game) => void;
}

export const HomeScreen: React.FC<HomeScreenProps> = ({
  membership,
  theme,
  t,
  selectedGame,
  unreadNotificationsCount = 0,
  onSelectGame,
  onOpenLoginModal,
  onViewAllGames,
  onOpenNotifications,
  onNavigateToPremium,
  onLaunchGame
}) => {
  const [currentSlide, setCurrentSlide] = useState(0);
  const isDark = theme === 'dark';

  const carouselItems = [
    {
      id: 1,
      image: '/assets/images/carousels/carousel_image_1.png',
      tag: t.carouselTag1,
      title: t.carouselTitle1,
      subtitle: t.carouselSubtitle1
    },
    {
      id: 2,
      image: '/assets/images/carousels/carousel_image_2.png',
      tag: t.carouselTag2,
      title: t.carouselTitle2,
      subtitle: t.carouselSubtitle2
    },
    {
      id: 3,
      image: '/assets/images/carousels/carousel_image_3.png',
      tag: t.carouselTag3,
      title: t.carouselTitle3,
      subtitle: t.carouselSubtitle3
    }
  ];

  // Auto carousel slide every 4 seconds
  useEffect(() => {
    const timer = setInterval(() => {
      setCurrentSlide((prev) => (prev + 1) % carouselItems.length);
    }, 4000);
    return () => clearInterval(timer);
  }, [carouselItems.length]);

  const handlePrevSlide = () => {
    setCurrentSlide((prev) => (prev === 0 ? carouselItems.length - 1 : prev - 1));
  };

  const handleNextSlide = () => {
    setCurrentSlide((prev) => (prev + 1) % carouselItems.length);
  };

  return (
    <div
      className={`w-full h-full flex flex-col overflow-y-auto px-4 py-3 select-none transition-colors duration-200 ${
        isDark ? 'bg-[#0B0F17] text-[#F4F7FC]' : 'bg-[#F1F5F9] text-[#0F172A]'
      }`}
    >
      {/* Top Header Bar */}
      <div className="flex items-center justify-between mb-3 pt-1">
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
              {t.appSlogan}
            </span>
          </div>
        </div>

        {/* Top Notification Icon */}
        <button
          onClick={onOpenNotifications}
          className={`w-9 h-9 rounded-xl flex items-center justify-center transition-colors relative cursor-pointer ${
            isDark
              ? 'bg-[#111B2A] border border-[#1C2B42] text-[#8D9BB0] hover:text-[#39B9FF]'
              : 'bg-white border border-[#CBD5E1] text-[#64748B] hover:text-[#0284C7] shadow-xs'
          }`}
          title="Notifications & Broadcasts"
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

      {/* Hero Banner Card: Pro Gaming Toolkit */}
      <div
        className={`w-full rounded-2xl p-3.5 mb-3.5 border relative overflow-hidden transition-all ${
          isDark
            ? 'bg-[#111B2A] border-[#39B9FF]/50 shadow-[0_0_20px_rgba(57,185,255,0.15)]'
            : 'bg-white border-[#39B9FF]/40 shadow-sm'
        }`}
      >
        <div className="absolute -right-8 -bottom-8 w-28 h-28 rounded-full bg-[#39B9FF]/10 blur-xl pointer-events-none" />

        <div className="flex items-center justify-between relative z-10">
          <div className="flex items-center gap-3">
            <div
              className={`w-11 h-11 rounded-xl flex items-center justify-center shrink-0 border ${
                isDark ? 'bg-[#0B0F17] border-[#39B9FF]/30' : 'bg-[#E0F2FE] border-[#39B9FF]/40'
              }`}
            >
              <TacticalReticleLogo size={32} glow={false} />
            </div>

            <div className="flex flex-col">
              <div className="flex items-baseline gap-1">
                <span className={`text-base font-bold ${isDark ? 'text-white' : 'text-[#0F172A]'}`}>
                  {t.appTitle}
                </span>
                <span className="text-base font-bold text-[#39B9FF]">{t.appEngine}</span>
              </div>
              <span className={`text-xs font-medium ${isDark ? 'text-[#8D9BB0]' : 'text-[#64748B]'}`}>
                {t.toolkitBadge}
              </span>
            </div>
          </div>

          <span className="px-2 py-0.5 rounded-full text-[10px] font-mono font-bold bg-[#39B9FF]/15 text-[#39B9FF] border border-[#39B9FF]/30">
            v1.0.0
          </span>
        </div>
      </div>

      {/* Multiple Image Carousels Section */}
      <div className="relative w-full mb-4">
        {/* Carousel Visual Frame */}
        <div
          className={`w-full h-40 sm:h-44 rounded-2xl overflow-hidden relative border shadow-md ${
            isDark ? 'border-[#1C2B42] bg-[#0E1624]' : 'border-[#CBD5E1] bg-white'
          }`}
        >
          {carouselItems.map((item, idx) => {
            const isVisible = idx === currentSlide;
            return (
              <div
                key={item.id}
                className={`absolute inset-0 transition-opacity duration-700 ease-in-out ${
                  isVisible ? 'opacity-100 z-10 pointer-events-auto' : 'opacity-0 z-0 pointer-events-none'
                }`}
              >
                {/* Background Banner Image with Aspect Ratio Fit & Tactical Gradient */}
                <img
                  src={item.image}
                  alt={item.title}
                  className="w-full h-full object-cover object-center filter brightness-[0.92]"
                />

                {/* Dark Vignette Overlay for Text Legibility */}
                <div className="absolute inset-0 bg-gradient-to-t from-black/90 via-black/45 to-black/20" />

                {/* Foreground Overlay Content */}
                <div className="absolute inset-0 p-3.5 flex flex-col justify-between">
                  <div className="flex items-center justify-between">
                    <span className="text-[10px] font-mono font-bold px-2 py-0.5 rounded-full bg-[#39B9FF]/30 text-[#E0F2FE] border border-[#39B9FF]/60 backdrop-blur-xs tracking-wider">
                      {item.tag}
                    </span>
                    <ShieldCheck className="w-4 h-4 text-[#39B9FF]" />
                  </div>

                  <div>
                    <h4 className="text-sm font-bold text-white tracking-tight leading-snug drop-shadow-md">
                      {item.title}
                    </h4>
                    <p className="text-[11px] text-slate-200 mt-0.5 font-medium line-clamp-1 drop-shadow-sm">
                      {item.subtitle}
                    </p>
                  </div>
                </div>
              </div>
            );
          })}

          {/* Carousel Left Navigation Arrow */}
          <button
            onClick={handlePrevSlide}
            aria-label="Previous Slide"
            className="absolute left-2 top-1/2 -translate-y-1/2 z-20 w-7 h-7 rounded-full bg-black/60 text-white/80 hover:text-white hover:bg-black/80 flex items-center justify-center backdrop-blur-xs transition-all cursor-pointer border border-white/20 active:scale-95"
          >
            <ChevronLeft className="w-4 h-4" />
          </button>

          {/* Carousel Right Navigation Arrow */}
          <button
            onClick={handleNextSlide}
            aria-label="Next Slide"
            className="absolute right-2 top-1/2 -translate-y-1/2 z-20 w-7 h-7 rounded-full bg-black/60 text-white/80 hover:text-white hover:bg-black/80 flex items-center justify-center backdrop-blur-xs transition-all cursor-pointer border border-white/20 active:scale-95"
          >
            <ChevronRight className="w-4 h-4" />
          </button>
        </div>

        {/* Carousel Pagination Dots */}
        <div className="flex items-center justify-center gap-1.5 mt-2">
          {carouselItems.map((_, idx) => (
            <button
              key={idx}
              onClick={() => setCurrentSlide(idx)}
              aria-label={`Go to slide ${idx + 1}`}
              className={`transition-all duration-300 rounded-full cursor-pointer ${
                idx === currentSlide
                  ? 'w-6 h-1.5 bg-[#39B9FF] shadow-[0_0_8px_#39B9FF]'
                  : isDark
                  ? 'w-1.5 h-1.5 bg-[#1C2B42] hover:bg-[#8D9BB0]'
                  : 'w-1.5 h-1.5 bg-[#CBD5E1] hover:bg-[#94A3B8]'
              }`}
            />
          ))}
        </div>
      </div>

      {/* Supported Games Section Header */}
      <div className="flex items-center justify-between mb-2.5">
        <div className="flex items-center gap-2">
          <Gamepad2 className="w-4 h-4 text-[#39B9FF]" />
          <span className={`text-sm font-bold tracking-wide ${isDark ? 'text-white' : 'text-[#0F172A]'}`}>
            {t.supportedGames}
          </span>
        </div>
        <button
          onClick={onViewAllGames}
          className="text-xs font-semibold text-[#39B9FF] hover:underline cursor-pointer"
        >
          {t.viewAll}
        </button>
      </div>

      {/* 4-column Grid of Games with Real Logos from dedicated assets/images/games/ */}
      <div className="grid grid-cols-4 gap-2 mb-4">
        {SUPPORTED_GAMES.map((game) => {
          const isSelected = selectedGame.id === game.id;
          const isGameActive = game.isActive === true;

          return (
            <button
              key={game.id}
              onClick={() => onSelectGame(game)}
              className={`flex flex-col items-center p-2 rounded-xl transition-all cursor-pointer relative group ${
                isSelected
                  ? isDark
                    ? 'bg-[#14283D] border-2 border-[#39B9FF] shadow-[0_0_12px_rgba(57,185,255,0.3)] ring-1 ring-[#39B9FF]/50 scale-[1.02]'
                    : 'bg-[#E0F2FE] border-2 border-[#0284C7] shadow-md scale-[1.02]'
                  : isDark
                  ? 'bg-[#111B2A] border border-[#1C2B42] hover:border-[#39B9FF]/40'
                  : 'bg-white border border-[#CBD5E1] hover:border-[#0284C7]/50 shadow-xs'
              }`}
            >
              {/* Game Logo Container */}
              <div className="w-12 h-12 rounded-xl p-1 relative flex items-center justify-center overflow-hidden bg-black/30 border border-white/10">
                <img
                  src={game.logoUrl}
                  alt={game.name}
                  className="w-full h-full object-contain rounded-lg drop-shadow-sm group-hover:scale-105 transition-transform"
                  loading="lazy"
                  onError={(e) => {
                    // Fallback to text initials if image fails
                    e.currentTarget.style.display = 'none';
                  }}
                />

                {/* Active Indicator Badge Dot */}
                <div
                  className={`absolute top-1 right-1 w-2.5 h-2.5 rounded-full ring-1 ring-white/60 ${
                    isGameActive ? 'bg-[#49DCA0] shadow-[0_0_6px_#49DCA0]' : 'bg-[#64748B]'
                  }`}
                  title={isGameActive ? t.activeStatus : t.lockedStatus}
                />
              </div>

              {/* Game Name */}
              <span
                className={`text-[10px] font-semibold mt-1 text-center leading-tight line-clamp-1 w-full ${
                  isSelected
                    ? isDark
                      ? 'text-[#39B9FF]'
                      : 'text-[#0284C7]'
                    : isDark
                    ? 'text-[#F4F7FC]'
                    : 'text-[#0F172A]'
                }`}
              >
                {game.name}
              </span>

              {/* Small Status Tag */}
              <span
                className={`text-[8px] font-mono leading-none mt-0.5 ${
                  isGameActive ? 'text-[#49DCA0] font-bold' : isDark ? 'text-[#8D9BB0]' : 'text-[#64748B]'
                }`}
              >
                {isGameActive ? t.activeStatus : t.lockedStatus}
              </span>
            </button>
          );
        })}
      </div>

      {/* STATE DEPENDENT BOTTOM CARD: Login Card OR Launch The Game Button (Requirement 4) */}
      <div className="mt-auto mb-2">
        {membership.isLoggedIn ? (
          /* ========================================================
             WHEN LOGGED IN: Display ONLY the "Launch The Game" Button
             ======================================================== */
          <div
            className={`w-full rounded-2xl p-4 border transition-all ${
              isDark
                ? 'bg-[#111B2A] border-[#39B9FF]/40 shadow-[0_0_20px_rgba(57,185,255,0.15)]'
                : 'bg-white border-[#39B9FF]/50 shadow-md'
            }`}
          >
            {/* Selected Game Profile Bar */}
            <div className="flex items-center justify-between mb-3 pb-2.5 border-b border-[#1C2B42]/40">
              <div className="flex items-center gap-2.5 min-w-0">
                <div className="w-10 h-10 rounded-xl bg-black/40 border border-white/10 p-1 shrink-0 overflow-hidden">
                  <img
                    src={selectedGame.logoUrl}
                    alt={selectedGame.name}
                    className="w-full h-full object-contain rounded-lg"
                  />
                </div>
                <div className="flex flex-col min-w-0">
                  <div className="flex items-center gap-1.5">
                    <span
                      className={`text-xs font-bold truncate ${
                        isDark ? 'text-white' : 'text-[#0F172A]'
                      }`}
                    >
                      {selectedGame.name}
                    </span>
                    <span className="px-1.5 py-0.2 rounded text-[9px] font-mono font-bold bg-[#49DCA0]/20 text-[#49DCA0] border border-[#49DCA0]/30 shrink-0">
                      {selectedGame.isActive ? t.activeStatus : t.lockedStatus}
                    </span>
                  </div>
                  <span
                    className={`text-[10px] truncate ${
                      isDark ? 'text-[#8D9BB0]' : 'text-[#64748B]'
                    }`}
                  >
                    {selectedGame.features}
                  </span>
                </div>
              </div>

              <button
                onClick={onNavigateToPremium}
                className="shrink-0 text-[10px] text-[#39B9FF] hover:underline font-medium cursor-pointer ml-2"
              >
                {membership.daysRemaining} {t.days}
              </button>
            </div>

            {/* Prominent Launch The Game Action Button */}
            <button
              onClick={() => onLaunchGame(selectedGame)}
              className="w-full py-3 px-4 rounded-xl bg-gradient-to-r from-[#1A85CC] via-[#39B9FF] to-[#1A85CC] hover:opacity-95 text-[#0B0F17] font-black text-xs uppercase tracking-wider transition-all shadow-[0_0_20px_rgba(57,185,255,0.45)] active:scale-[0.98] flex items-center justify-center gap-2.5 cursor-pointer"
            >
              <Play className="w-4 h-4 fill-[#0B0F17]" />
              <span>{t.launchTheGame}</span>
            </button>
          </div>
        ) : (
          /* ========================================================
             WHEN NOT LOGGED IN: Display the Login Card
             ======================================================== */
          <div
            className={`w-full rounded-2xl p-4 border transition-all ${
              isDark ? 'bg-[#111B2A] border-[#1C2B42]' : 'bg-white border-[#CBD5E1] shadow-sm'
            }`}
          >
            <div className="flex items-center gap-3 mb-1.5">
              <div
                className={`w-9 h-9 rounded-xl flex items-center justify-center shrink-0 ${
                  isDark
                    ? 'bg-[#39B9FF]/15 border border-[#39B9FF]/30 text-[#39B9FF]'
                    : 'bg-[#E0F2FE] border border-[#39B9FF]/40 text-[#0284C7]'
                }`}
              >
                <Lock className="w-4 h-4" />
              </div>
              <span className={`text-sm font-bold ${isDark ? 'text-white' : 'text-[#0F172A]'}`}>
                {t.youAreNotLoggedIn}
              </span>
            </div>

            <p className={`text-xs mt-1 mb-3.5 ${isDark ? 'text-[#8D9BB0]' : 'text-[#64748B]'}`}>
              {t.loginDesc}
            </p>

            <button
              onClick={onOpenLoginModal}
              className="w-full py-2.5 rounded-xl bg-[#39B9FF] hover:bg-[#2da5e6] text-[#0B0F17] font-bold text-xs uppercase tracking-wider transition-all shadow-[0_0_15px_rgba(57,185,255,0.4)] active:scale-[0.98] cursor-pointer"
            >
              {t.loginNow}
            </button>
          </div>
        )}
      </div>
    </div>
  );
};
