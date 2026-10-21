import React, { useState } from 'react';
import { ArrowLeft, Search, CheckCircle2 } from 'lucide-react';
import { SUPPORTED_GAMES } from '../../data/mockData';
import { Game } from '../../types';

interface SupportedGamesScreenProps {
  onBack: () => void;
  onSelectGame: (game: Game) => void;
}

export const SupportedGamesScreen: React.FC<SupportedGamesScreenProps> = ({
  onBack,
  onSelectGame
}) => {
  const [searchQuery, setSearchQuery] = useState('');
  const [showSearchInput, setShowSearchInput] = useState(false);

  const filteredGames = SUPPORTED_GAMES.filter((g) =>
    g.name.toLowerCase().includes(searchQuery.toLowerCase()) ||
    g.features.toLowerCase().includes(searchQuery.toLowerCase())
  );

  return (
    <div className="w-full h-full flex flex-col bg-[#0B0F17] overflow-y-auto px-4 py-3 select-none">
      {/* Top Action Bar matching Screen 5 */}
      <div className="flex items-center justify-between mb-3 pt-1">
        <div className="flex items-center gap-2">
          <button
            onClick={onBack}
            className="w-9 h-9 rounded-xl bg-[#111B2A] border border-[#1C2B42] flex items-center justify-center text-white hover:text-[#39B9FF] transition-colors"
          >
            <ArrowLeft className="w-5 h-5" />
          </button>
          <h2 className="text-base font-bold text-white tracking-wide">Supported Games</h2>
        </div>

        <button
          onClick={() => setShowSearchInput(!showSearchInput)}
          className={`w-9 h-9 rounded-xl border flex items-center justify-center transition-colors ${
            showSearchInput
              ? 'bg-[#39B9FF]/20 border-[#39B9FF] text-[#39B9FF]'
              : 'bg-[#111B2A] border-[#1C2B42] text-[#8D9BB0] hover:text-[#39B9FF]'
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
            className="w-full bg-[#080C12] border border-[#39B9FF]/50 rounded-xl px-3.5 py-2.5 text-xs text-white placeholder-[#8D9BB0] focus:outline-none focus:border-[#39B9FF]"
          />
        </div>
      )}

      {/* Vertical List of Game Cards matching Screen 5 */}
      <div className="space-y-2.5 pb-4">
        {filteredGames.map((game) => (
          <div
            key={game.id}
            onClick={() => onSelectGame(game)}
            className="w-full p-3.5 rounded-2xl bg-[#111B2A] border border-[#1C2B42] hover:border-[#39B9FF]/40 transition-all flex items-center justify-between cursor-pointer group"
          >
            <div className="flex items-center gap-3 min-w-0">
              <div className="w-11 h-11 rounded-xl bg-[#16253A] border border-[#233854] flex items-center justify-center shrink-0">
                <span className="text-[11px] font-black text-[#39B9FF] uppercase">
                  {game.name.slice(0, 4)}
                </span>
              </div>

              <div className="flex flex-col min-w-0">
                <span className="text-sm font-bold text-white group-hover:text-[#39B9FF] transition-colors truncate">
                  {game.name}
                </span>
                <span className="text-[11px] text-[#8D9BB0] truncate mt-0.5">
                  {game.features}
                </span>
              </div>
            </div>

            {/* Verified Green Badge */}
            <div className="shrink-0 ml-3">
              <CheckCircle2 className="w-5 h-5 text-[#49DCA0]" />
            </div>
          </div>
        ))}

        {filteredGames.length === 0 && (
          <div className="text-center py-10 text-xs text-[#8D9BB0]">
            No games found matching "{searchQuery}"
          </div>
        )}
      </div>
    </div>
  );
};
