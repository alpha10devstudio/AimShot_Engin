import React, { useState } from 'react';
import { ArrowLeft, Search, CheckCircle2 } from 'lucide-react';
import { SUPPORTED_GAMES } from '../../data/mockData';
import { Game, AppTheme } from '../../types';
import { Translations } from '../../data/translations';

interface SupportedGamesScreenProps {
  theme: AppTheme;
  t: Translations;
  selectedGame: Game;
  onBack: () => void;
  onSelectGame: (game: Game) => void;
}

export const SupportedGamesScreen: React.FC<SupportedGamesScreenProps> = ({
  theme,
  t,
  selectedGame,
  onBack,
  onSelectGame
}) => {
  const [searchQuery, setSearchQuery] = useState('');
  const [showSearchInput, setShowSearchInput] = useState(false);
  const isDark = theme === 'dark';

  const filteredGames = SUPPORTED_GAMES.filter((g) =>
    g.name.toLowerCase().includes(searchQuery.toLowerCase()) ||
    g.features.toLowerCase().includes(searchQuery.toLowerCase())
  );

  return (
    <div
      className={`w-full h-full flex flex-col overflow-y-auto px-4 py-3 select-none transition-colors duration-200 ${
        isDark ? 'bg-[#0B0F17] text-[#F4F7FC]' : 'bg-[#F1F5F9] text-[#0F172A]'
      }`}
    >
      {/* Top Action Bar */}
      <div className="flex items-center justify-between mb-3 pt-1">
        <div className="flex items-center gap-2">
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
            {t.supportedGames}
          </h2>
        </div>

        <button
          onClick={() => setShowSearchInput(!showSearchInput)}
          className={`w-9 h-9 rounded-xl border flex items-center justify-center transition-colors cursor-pointer ${
            showSearchInput
              ? 'bg-[#39B9FF]/20 border-[#39B9FF] text-[#39B9FF]'
              : isDark
              ? 'bg-[#111B2A] border-[#1C2B42] text-[#8D9BB0] hover:text-[#39B9FF]'
              : 'bg-white border-[#CBD5E1] text-[#64748B] hover:text-[#0284C7] shadow-xs'
          }`}
        >
          <Search className="w-4 h-4" />
        </button>
      </div>

      {/* Expandable Search Input */}
      {showSearchInput && (
        <div className="mb-3 animate-fade-in">
          <input
            type="text"
            value={searchQuery}
            onChange={(e) => setSearchQuery(e.target.value)}
            placeholder="Search game or feature..."
            autoFocus
            className={`w-full rounded-xl px-3.5 py-2.5 text-xs focus:outline-none transition-all ${
              isDark
                ? 'bg-[#080C12] border border-[#39B9FF]/50 text-white placeholder-[#8D9BB0] focus:border-[#39B9FF]'
                : 'bg-white border border-[#CBD5E1] text-[#0F172A] placeholder-[#94A3B8] focus:border-[#0284C7] shadow-xs'
            }`}
          />
        </div>
      )}

      {/* Vertical List of Game Cards with Dedicated Game Logos */}
      <div className="space-y-2.5 pb-4">
        {filteredGames.map((game) => {
          const isSelected = selectedGame.id === game.id;
          return (
            <div
              key={game.id}
              onClick={() => onSelectGame(game)}
              className={`w-full p-3.5 rounded-2xl border transition-all flex items-center justify-between cursor-pointer group ${
                isSelected
                  ? isDark
                    ? 'bg-[#14283D] border-2 border-[#39B9FF] shadow-[0_0_15px_rgba(57,185,255,0.25)]'
                    : 'bg-[#E0F2FE] border-2 border-[#0284C7] shadow-sm'
                  : isDark
                  ? 'bg-[#111B2A] border-[#1C2B42] hover:border-[#39B9FF]/40'
                  : 'bg-white border-[#CBD5E1] hover:border-[#0284C7]/40 shadow-xs'
              }`}
            >
              <div className="flex items-center gap-3 min-w-0">
                <div className="w-12 h-12 rounded-xl bg-black/40 border border-white/10 p-1 flex items-center justify-center shrink-0 overflow-hidden">
                  <img
                    src={game.logoUrl}
                    alt={game.name}
                    className="w-full h-full object-contain rounded-lg group-hover:scale-105 transition-transform"
                    onError={(e) => {
                      e.currentTarget.style.display = 'none';
                    }}
                  />
                </div>

                <div className="flex flex-col min-w-0">
                  <div className="flex items-center gap-2">
                    <span
                      className={`text-sm font-bold group-hover:text-[#39B9FF] transition-colors truncate ${
                        isDark ? 'text-white' : 'text-[#0F172A]'
                      }`}
                    >
                      {game.name}
                    </span>
                    <span
                      className={`text-[9px] font-mono px-1.5 py-0.5 rounded font-bold ${
                        game.isActive
                          ? 'bg-[#49DCA0]/20 text-[#49DCA0] border border-[#49DCA0]/30'
                          : isDark
                          ? 'bg-[#1C2B42] text-[#8D9BB0]'
                          : 'bg-[#E2E8F0] text-[#64748B]'
                      }`}
                    >
                      {game.isActive ? t.activeStatus : t.lockedStatus}
                    </span>
                  </div>
                  <span
                    className={`text-[11px] truncate mt-0.5 ${
                      isDark ? 'text-[#8D9BB0]' : 'text-[#64748B]'
                    }`}
                  >
                    {game.features}
                  </span>
                </div>
              </div>

              {/* Status / Selected Checkmark Badge */}
              <div className="shrink-0 ml-3">
                <CheckCircle2
                  className={`w-5 h-5 ${
                    isSelected
                      ? 'text-[#39B9FF] fill-[#39B9FF]/20'
                      : game.isActive
                      ? 'text-[#49DCA0]'
                      : isDark
                      ? 'text-[#233854]'
                      : 'text-[#CBD5E1]'
                  }`}
                />
              </div>
            </div>
          );
        })}

        {filteredGames.length === 0 && (
          <div className={`text-center py-10 text-xs ${isDark ? 'text-[#8D9BB0]' : 'text-[#64748B]'}`}>
            No games found matching "{searchQuery}"
          </div>
        )}
      </div>
    </div>
  );
};
