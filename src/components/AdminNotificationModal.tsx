import React, { useState } from 'react';
import {
  X,
  Send,
  Trash2,
  Bell,
  Sparkles,
  Wrench,
  Megaphone,
  CheckCircle2,
  AlertTriangle,
  RefreshCw
} from 'lucide-react';
import { AppNotification } from '../types';

interface AdminNotificationModalProps {
  isOpen: boolean;
  onClose: () => void;
  notifications: AppNotification[];
  onPublishNotification: (newNotif: {
    title: string;
    message: string;
    category: 'announcement' | 'update' | 'feature' | 'maintenance';
    priority: 'normal' | 'high' | 'urgent';
    minVersion: string;
  }) => Promise<void>;
  onDeleteNotification: (id: string) => Promise<void>;
  onRefresh: () => void;
}

export const AdminNotificationModal: React.FC<AdminNotificationModalProps> = ({
  isOpen,
  onClose,
  notifications,
  onPublishNotification,
  onDeleteNotification,
  onRefresh
}) => {
  const [title, setTitle] = useState('');
  const [message, setMessage] = useState('');
  const [category, setCategory] = useState<'announcement' | 'update' | 'feature' | 'maintenance'>('announcement');
  const [priority, setPriority] = useState<'normal' | 'high' | 'urgent'>('normal');
  const [minVersion, setMinVersion] = useState('1.0.0');
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [statusMessage, setStatusMessage] = useState<string | null>(null);

  if (!isOpen) return null;

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!title.trim() || !message.trim()) {
      setStatusMessage('Please enter both title and message.');
      return;
    }

    setIsSubmitting(true);
    setStatusMessage(null);
    try {
      await onPublishNotification({
        title: title.trim(),
        message: message.trim(),
        category,
        priority,
        minVersion
      });
      setTitle('');
      setMessage('');
      setStatusMessage('Announcement published successfully to connected AimShot Engine APKs!');
      setTimeout(() => setStatusMessage(null), 3500);
    } catch {
      setStatusMessage('Failed to publish announcement. Please check server status.');
    } finally {
      setIsSubmitting(false);
    }
  };

  const handleDelete = async (id: string) => {
    try {
      await onDeleteNotification(id);
      setStatusMessage('Announcement deleted.');
      setTimeout(() => setStatusMessage(null), 3000);
    } catch {
      setStatusMessage('Failed to delete announcement.');
    }
  };

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-black/80 backdrop-blur-sm animate-fade-in">
      <div className="w-full max-w-2xl bg-[#0B0F17] border border-[#1C2B42] rounded-2xl shadow-2xl overflow-hidden flex flex-col max-h-[90vh]">
        {/* Header */}
        <div className="px-6 py-4 border-b border-[#1C2B42] flex items-center justify-between bg-[#111B2A]">
          <div className="flex items-center gap-3">
            <div className="w-9 h-9 rounded-xl bg-[#39B9FF]/15 border border-[#39B9FF]/30 flex items-center justify-center text-[#39B9FF]">
              <Megaphone className="w-5 h-5" />
            </div>
            <div>
              <h2 className="text-base font-bold text-white flex items-center gap-2">
                <span>AimShot Broadcast Admin</span>
                <span className="px-2 py-0.5 rounded text-[10px] font-mono bg-[#49DCA0]/15 text-[#49DCA0] border border-[#49DCA0]/30 font-bold">
                  SERVER SYNC ACTIVE
                </span>
              </h2>
              <p className="text-xs text-[#8D9BB0]">
                Publish official announcements & update alerts to all connected AimShot Engine apps
              </p>
            </div>
          </div>
          <button
            onClick={onClose}
            className="w-8 h-8 rounded-lg flex items-center justify-center text-[#8D9BB0] hover:text-white hover:bg-white/10 transition-colors cursor-pointer"
          >
            <X className="w-5 h-5" />
          </button>
        </div>

        {/* Modal Body */}
        <div className="flex-1 overflow-y-auto p-6 space-y-6">
          {statusMessage && (
            <div className="p-3 rounded-xl bg-[#14283D] border border-[#39B9FF]/40 text-xs text-[#39B9FF] flex items-center gap-2 font-medium">
              <CheckCircle2 className="w-4 h-4 shrink-0 text-[#49DCA0]" />
              <span>{statusMessage}</span>
            </div>
          )}

          {/* Publishing Form */}
          <form onSubmit={handleSubmit} className="bg-[#111B2A] rounded-xl p-4 border border-[#1C2B42] space-y-4">
            <h3 className="text-xs font-bold uppercase tracking-wider text-[#39B9FF] flex items-center gap-1.5">
              <Send className="w-3.5 h-3.5" />
              <span>Compose New App Announcement</span>
            </h3>

            <div className="grid grid-cols-1 md:grid-cols-3 gap-3">
              <div className="md:col-span-2 space-y-1">
                <label className="text-[11px] font-semibold text-[#8D9BB0]">Announcement Title</label>
                <input
                  type="text"
                  value={title}
                  onChange={(e) => setTitle(e.target.value)}
                  placeholder="e.g. AimShot Engine v1.0.1 Update Available"
                  className="w-full px-3 py-2 rounded-lg bg-[#0B0F17] border border-[#1C2B42] text-xs text-white placeholder-slate-500 focus:outline-none focus:border-[#39B9FF]"
                />
              </div>

              <div className="space-y-1">
                <label className="text-[11px] font-semibold text-[#8D9BB0]">Category</label>
                <select
                  value={category}
                  onChange={(e) => setCategory(e.target.value as any)}
                  className="w-full px-3 py-2 rounded-lg bg-[#0B0F17] border border-[#1C2B42] text-xs text-white focus:outline-none focus:border-[#39B9FF]"
                >
                  <option value="announcement">Announcement</option>
                  <option value="update">Update</option>
                  <option value="feature">Feature</option>
                  <option value="maintenance">Maintenance</option>
                </select>
              </div>
            </div>

            <div className="grid grid-cols-1 md:grid-cols-2 gap-3">
              <div className="space-y-1">
                <label className="text-[11px] font-semibold text-[#8D9BB0]">Priority Level</label>
                <select
                  value={priority}
                  onChange={(e) => setPriority(e.target.value as any)}
                  className="w-full px-3 py-2 rounded-lg bg-[#0B0F17] border border-[#1C2B42] text-xs text-white focus:outline-none focus:border-[#39B9FF]"
                >
                  <option value="normal">Normal Priority</option>
                  <option value="high">High Priority</option>
                  <option value="urgent">Urgent / Critical</option>
                </select>
              </div>

              <div className="space-y-1">
                <label className="text-[11px] font-semibold text-[#8D9BB0]">Target Version</label>
                <input
                  type="text"
                  value={minVersion}
                  onChange={(e) => setMinVersion(e.target.value)}
                  placeholder="1.0.0"
                  className="w-full px-3 py-2 rounded-lg bg-[#0B0F17] border border-[#1C2B42] text-xs text-white placeholder-slate-500 focus:outline-none focus:border-[#39B9FF]"
                />
              </div>
            </div>

            <div className="space-y-1">
              <label className="text-[11px] font-semibold text-[#8D9BB0]">Broadcast Message</label>
              <textarea
                value={message}
                onChange={(e) => setMessage(e.target.value)}
                rows={3}
                placeholder="Write the full announcement text that users will see in their in-app notification center..."
                className="w-full px-3 py-2 rounded-lg bg-[#0B0F17] border border-[#1C2B42] text-xs text-white placeholder-slate-500 focus:outline-none focus:border-[#39B9FF] resize-none"
              />
            </div>

            <button
              type="submit"
              disabled={isSubmitting}
              className="w-full py-2.5 rounded-xl bg-[#39B9FF] hover:bg-[#2da5e6] text-[#0B0F17] font-bold text-xs uppercase tracking-wider transition-all flex items-center justify-center gap-2 cursor-pointer shadow-[0_0_15px_rgba(57,185,255,0.3)] disabled:opacity-50"
            >
              <Send className="w-4 h-4" />
              <span>{isSubmitting ? 'Broadcasting...' : 'Publish to AimShot Engine Mobile APK'}</span>
            </button>
          </form>

          {/* Active Broadcasts List */}
          <div className="space-y-3">
            <div className="flex items-center justify-between">
              <h3 className="text-xs font-bold uppercase tracking-wider text-[#8D9BB0] flex items-center gap-1.5">
                <Bell className="w-3.5 h-3.5 text-[#39B9FF]" />
                <span>Active Server Announcements ({notifications.length})</span>
              </h3>
              <button
                type="button"
                onClick={onRefresh}
                className="text-[11px] text-[#39B9FF] hover:underline flex items-center gap-1 cursor-pointer"
              >
                <RefreshCw className="w-3 h-3" />
                <span>Refresh Live State</span>
              </button>
            </div>

            <div className="space-y-2">
              {notifications.map((item) => (
                <div
                  key={item.id}
                  className="p-3 rounded-xl bg-[#111B2A] border border-[#1C2B42] flex items-start justify-between gap-3"
                >
                  <div className="flex-1 min-w-0">
                    <div className="flex items-center gap-2 mb-1">
                      <span className="text-xs font-bold text-white truncate">{item.title}</span>
                      <span className="text-[9px] uppercase font-mono px-1.5 py-0.2 rounded border bg-[#39B9FF]/15 text-[#39B9FF] border-[#39B9FF]/30">
                        {item.category}
                      </span>
                      {item.priority === 'urgent' && (
                        <span className="text-[9px] uppercase font-mono px-1.5 py-0.2 rounded border bg-rose-500/20 text-rose-400 border-rose-500/30">
                          Urgent
                        </span>
                      )}
                    </div>
                    <p className="text-[11px] text-[#8D9BB0] line-clamp-2 leading-relaxed">{item.message}</p>
                    <div className="flex items-center gap-3 mt-1.5 text-[10px] text-slate-500 font-mono">
                      <span>ID: {item.id}</span>
                      <span>Published: {item.timestamp}</span>
                      <span>Status: {item.isRead ? 'Read' : 'Unread'}</span>
                    </div>
                  </div>

                  <button
                    onClick={() => handleDelete(item.id)}
                    className="p-2 rounded-lg text-slate-400 hover:text-rose-400 hover:bg-rose-500/10 transition-colors cursor-pointer shrink-0"
                    title="Delete announcement"
                  >
                    <Trash2 className="w-4 h-4" />
                  </button>
                </div>
              ))}

              {notifications.length === 0 && (
                <div className="p-6 text-center text-xs text-[#8D9BB0] bg-[#111B2A] rounded-xl border border-[#1C2B42]">
                  No active server announcements. Use the form above to broadcast one.
                </div>
              )}
            </div>
          </div>
        </div>
      </div>
    </div>
  );
};
