import React, { useState } from 'react';
import {
  ArrowLeft,
  Bell,
  CheckCheck,
  RefreshCw,
  Sparkles,
  Wrench,
  ShieldAlert,
  Megaphone,
  CheckCircle2,
  X
} from 'lucide-react';
import { AppNotification, AppTheme } from '../../types';
import { Translations } from '../../data/translations';

interface NotificationsScreenProps {
  theme: AppTheme;
  t: Translations;
  notifications: AppNotification[];
  onBack: () => void;
  onMarkAsRead: (id: string) => void;
  onMarkAllAsRead: () => void;
  onRefresh: () => void;
  isSyncing: boolean;
}

export const NotificationsScreen: React.FC<NotificationsScreenProps> = ({
  theme,
  t,
  notifications,
  onBack,
  onMarkAsRead,
  onMarkAllAsRead,
  onRefresh,
  isSyncing
}) => {
  const isDark = theme === 'dark';
  const [selectedCategory, setSelectedCategory] = useState<string>('all');
  const [activeDetailItem, setActiveDetailItem] = useState<AppNotification | null>(null);

  const categories = [
    { id: 'all', label: 'All' },
    { id: 'announcement', label: 'Announcements' },
    { id: 'update', label: 'Updates' },
    { id: 'feature', label: 'Features' },
    { id: 'maintenance', label: 'Maintenance' }
  ];

  const filteredNotifications = notifications.filter((item) => {
    if (selectedCategory === 'all') return true;
    return item.category.toLowerCase() === selectedCategory.toLowerCase();
  });

  const unreadCount = notifications.filter((n) => !n.isRead).length;

  const getCategoryIcon = (category: string) => {
    switch (category.toLowerCase()) {
      case 'feature':
        return <Sparkles className="w-4 h-4 text-[#39B9FF]" />;
      case 'update':
        return <CheckCircle2 className="w-4 h-4 text-[#49DCA0]" />;
      case 'maintenance':
        return <Wrench className="w-4 h-4 text-amber-400" />;
      case 'announcement':
      default:
        return <Megaphone className="w-4 h-4 text-purple-400" />;
    }
  };

  const getCategoryBadgeClass = (category: string) => {
    switch (category.toLowerCase()) {
      case 'feature':
        return 'bg-[#39B9FF]/15 text-[#39B9FF] border-[#39B9FF]/30';
      case 'update':
        return 'bg-[#49DCA0]/15 text-[#49DCA0] border-[#49DCA0]/30';
      case 'maintenance':
        return 'bg-amber-400/15 text-amber-400 border-amber-400/30';
      case 'announcement':
      default:
        return 'bg-purple-400/15 text-purple-400 border-purple-400/30';
    }
  };

  const handleItemClick = (item: AppNotification) => {
    if (!item.isRead) {
      onMarkAsRead(item.id);
    }
    setActiveDetailItem(item);
  };

  return (
    <div
      className={`w-full h-full flex flex-col overflow-y-auto px-4 py-3 select-none transition-colors duration-200 ${
        isDark ? 'bg-[#0B0F17] text-[#F4F7FC]' : 'bg-[#F1F5F9] text-[#0F172A]'
      }`}
    >
      {/* Top Header Bar */}
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
          <div>
            <h2 className={`text-base font-bold tracking-wide ${isDark ? 'text-white' : 'text-[#0F172A]'}`}>
              Notifications
            </h2>
            <span className={`text-[10px] block ${isDark ? 'text-[#8D9BB0]' : 'text-[#64748B]'}`}>
              {unreadCount > 0 ? `${unreadCount} unread announcement${unreadCount > 1 ? 's' : ''}` : 'All caught up'}
            </span>
          </div>
        </div>

        <div className="flex items-center gap-1.5">
          <button
            onClick={onRefresh}
            disabled={isSyncing}
            title="Sync with website"
            className={`w-8 h-8 rounded-xl border flex items-center justify-center transition-all cursor-pointer ${
              isDark
                ? 'bg-[#111B2A] border-[#1C2B42] text-[#8D9BB0] hover:text-[#39B9FF]'
                : 'bg-white border-[#CBD5E1] text-[#64748B] hover:text-[#0284C7]'
            }`}
          >
            <RefreshCw className={`w-3.5 h-3.5 ${isSyncing ? 'animate-spin text-[#39B9FF]' : ''}`} />
          </button>

          {unreadCount > 0 && (
            <button
              onClick={onMarkAllAsRead}
              className={`px-2.5 py-1.5 rounded-xl border text-[11px] font-bold flex items-center gap-1 transition-all cursor-pointer ${
                isDark
                  ? 'bg-[#39B9FF]/15 border-[#39B9FF]/40 text-[#39B9FF] hover:bg-[#39B9FF]/25'
                  : 'bg-[#E0F2FE] border-[#39B9FF]/50 text-[#0284C7] hover:bg-[#BAE6FD]'
              }`}
            >
              <CheckCheck className="w-3.5 h-3.5" />
              <span>Mark read</span>
            </button>
          )}
        </div>
      </div>

      {/* Category Filter Pills */}
      <div className="flex items-center gap-1.5 overflow-x-auto pb-2 mb-2.5 scrollbar-none shrink-0">
        {categories.map((cat) => {
          const isSelected = selectedCategory === cat.id;
          return (
            <button
              key={cat.id}
              onClick={() => setSelectedCategory(cat.id)}
              className={`px-3 py-1 rounded-full text-xs font-semibold whitespace-nowrap transition-all cursor-pointer ${
                isSelected
                  ? isDark
                    ? 'bg-[#39B9FF] text-[#0B0F17] shadow-[0_0_10px_rgba(57,185,255,0.4)]'
                    : 'bg-[#0284C7] text-white shadow-xs'
                  : isDark
                  ? 'bg-[#111B2A] text-[#8D9BB0] hover:text-white border border-[#1C2B42]'
                  : 'bg-white text-[#64748B] hover:text-[#0F172A] border border-[#CBD5E1]'
              }`}
            >
              {cat.label}
            </button>
          );
        })}
      </div>

      {/* Notification List */}
      <div className="space-y-2.5 pb-6">
        {filteredNotifications.map((item) => {
          return (
            <div
              key={item.id}
              onClick={() => handleItemClick(item)}
              className={`w-full p-3.5 rounded-2xl border transition-all flex items-start gap-3 cursor-pointer relative group ${
                !item.isRead
                  ? isDark
                    ? 'bg-[#142234] border-[#39B9FF]/60 shadow-[0_0_12px_rgba(57,185,255,0.15)]'
                    : 'bg-[#F0F9FF] border-[#39B9FF]/50 shadow-xs'
                  : isDark
                  ? 'bg-[#111B2A] border-[#1C2B42] hover:border-[#39B9FF]/40'
                  : 'bg-white border-[#CBD5E1] hover:border-[#0284C7]/40 shadow-xs'
              }`}
            >
              <div
                className={`w-9 h-9 rounded-xl flex items-center justify-center shrink-0 border mt-0.5 ${
                  isDark ? 'bg-[#0B0F17] border-[#1C2B42]' : 'bg-[#F1F5F9] border-[#CBD5E1]'
                }`}
              >
                {getCategoryIcon(item.category)}
              </div>

              <div className="flex flex-col min-w-0 flex-1">
                <div className="flex items-center justify-between gap-1.5 mb-1">
                  <span
                    className={`text-xs font-bold truncate group-hover:text-[#39B9FF] transition-colors ${
                      isDark ? 'text-white' : 'text-[#0F172A]'
                    }`}
                  >
                    {item.title}
                  </span>
                  <span
                    className={`text-[9px] font-mono px-1.5 py-0.5 rounded font-bold uppercase border shrink-0 ${getCategoryBadgeClass(
                      item.category
                    )}`}
                  >
                    {item.category}
                  </span>
                </div>

                <p
                  className={`text-[11.5px] line-clamp-2 leading-relaxed ${
                    isDark ? 'text-[#8D9BB0]' : 'text-[#64748B]'
                  }`}
                >
                  {item.message}
                </p>

                <div className="flex items-center justify-between mt-2 pt-1.5 border-t border-white/5">
                  <span className={`text-[10px] font-mono ${isDark ? 'text-[#55657E]' : 'text-[#94A3B8]'}`}>
                    {item.timestamp}
                  </span>
                  {!item.isRead && (
                    <span className="flex items-center gap-1 text-[10px] font-bold text-[#39B9FF]">
                      <span className="w-1.5 h-1.5 rounded-full bg-[#39B9FF] animate-pulse" />
                      NEW
                    </span>
                  )}
                </div>
              </div>
            </div>
          );
        })}

        {filteredNotifications.length === 0 && (
          <div className="flex flex-col items-center justify-center py-12 text-center">
            <div
              className={`w-12 h-12 rounded-2xl flex items-center justify-center mb-3 ${
                isDark ? 'bg-[#111B2A] text-[#55657E]' : 'bg-[#E2E8F0] text-[#94A3B8]'
              }`}
            >
              <Bell className="w-6 h-6" />
            </div>
            <h4 className={`text-sm font-bold mb-1 ${isDark ? 'text-white' : 'text-[#0F172A]'}`}>
              No Notifications Found
            </h4>
            <p className={`text-xs max-w-xs ${isDark ? 'text-[#8D9BB0]' : 'text-[#64748B]'}`}>
              You're all caught up! Official updates and server announcements will appear here.
            </p>
          </div>
        )}
      </div>

      {/* Detail Dialog Modal */}
      {activeDetailItem && (
        <div className="absolute inset-0 z-50 flex items-center justify-center p-4 bg-black/85 backdrop-blur-md animate-fade-in">
          <div
            className={`w-full max-w-xs rounded-2xl p-5 border shadow-2xl transition-all ${
              isDark ? 'bg-[#111B2A] border-[#39B9FF]/50 text-white' : 'bg-white border-[#0284C7]/50 text-[#0F172A]'
            }`}
          >
            <div className="flex items-center justify-between mb-3">
              <span
                className={`text-[9px] font-mono px-2 py-0.5 rounded font-bold uppercase border ${getCategoryBadgeClass(
                  activeDetailItem.category
                )}`}
              >
                {activeDetailItem.category}
              </span>
              <button
                onClick={() => setActiveDetailItem(null)}
                className={`w-7 h-7 rounded-lg flex items-center justify-center cursor-pointer ${
                  isDark ? 'text-[#8D9BB0] hover:text-white' : 'text-[#64748B] hover:text-[#0F172A]'
                }`}
              >
                <X className="w-4 h-4" />
              </button>
            </div>

            <h3 className="text-sm font-bold leading-snug mb-2">{activeDetailItem.title}</h3>

            <p className={`text-xs leading-relaxed mb-4 ${isDark ? 'text-[#8D9BB0]' : 'text-[#64748B]'}`}>
              {activeDetailItem.message}
            </p>

            <div
              className={`p-2.5 rounded-xl text-[10px] font-mono mb-4 border space-y-1 ${
                isDark ? 'bg-[#0B0F17] border-[#1C2B42] text-[#8D9BB0]' : 'bg-[#F8FAFC] border-[#E2E8F0] text-[#64748B]'
              }`}
            >
              <div>Priority: {activeDetailItem.priority?.toUpperCase() || 'NORMAL'}</div>
              <div>Published: {activeDetailItem.timestamp}</div>
              <div>Status: Verified Official Announcement</div>
            </div>

            <button
              onClick={() => setActiveDetailItem(null)}
              className="w-full py-2.5 rounded-xl bg-[#39B9FF] text-xs font-bold text-[#0B0F17] cursor-pointer"
            >
              Dismiss
            </button>
          </div>
        </div>
      )}
    </div>
  );
};
