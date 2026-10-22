import React, { useState } from 'react';
import { ArrowLeft, Cpu, Copy, Check, Info } from 'lucide-react';
import { DEFAULT_DEVICE_SPECS } from '../../data/mockData';
import { AppTheme } from '../../types';
import { Translations } from '../../data/translations';

interface DeviceInfoScreenProps {
  theme: AppTheme;
  t: Translations;
  onBack: () => void;
  onShowToast: (message: string) => void;
}

export const DeviceInfoScreen: React.FC<DeviceInfoScreenProps> = ({
  theme,
  t,
  onBack,
  onShowToast
}) => {
  const [copied, setCopied] = useState(false);
  const isDark = theme === 'dark';
  const specs = DEFAULT_DEVICE_SPECS;

  const handleCopy = () => {
    navigator.clipboard.writeText(specs.deviceId);
    setCopied(true);
    onShowToast('Device ID copied to clipboard');
    setTimeout(() => setCopied(false), 2000);
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
          {t.deviceInformation}
        </h2>
      </div>

      {/* Device ID Card */}
      <div
        className={`w-full rounded-2xl p-4 mb-4 border transition-all ${
          isDark
            ? 'bg-[#111B2A] border-[#39B9FF]/50 shadow-[0_0_15px_rgba(57,185,255,0.15)]'
            : 'bg-white border-[#39B9FF]/40 shadow-xs'
        }`}
      >
        <div className={`flex items-center gap-2 text-xs mb-2.5 ${isDark ? 'text-[#8D9BB0]' : 'text-[#64748B]'}`}>
          <Cpu className="w-4 h-4 text-[#39B9FF]" />
          <span>{t.deviceId}</span>
        </div>

        <div className="flex items-center justify-between gap-2">
          <span
            className={`text-sm font-bold font-mono tracking-wider truncate ${
              isDark ? 'text-white' : 'text-[#0F172A]'
            }`}
          >
            {specs.deviceId}
          </span>

          <button
            onClick={handleCopy}
            className="px-3 py-1.5 rounded-xl bg-[#39B9FF] hover:bg-[#2da5e6] text-[#0B0F17] font-bold text-xs flex items-center gap-1.5 transition-all shadow-[0_0_10px_rgba(57,185,255,0.3)] shrink-0 cursor-pointer active:scale-95"
          >
            {copied ? <Check className="w-3.5 h-3.5" /> : <Copy className="w-3.5 h-3.5" />}
            <span>{copied ? 'Copied' : 'Copy'}</span>
          </button>
        </div>
      </div>

      {/* Device Hardware Specs Card */}
      <div
        className={`w-full rounded-2xl p-4 mb-4 border transition-all ${
          isDark ? 'bg-[#111B2A] border-[#1C2B42]' : 'bg-white border-[#CBD5E1] shadow-xs'
        }`}
      >
        <div className={`divide-y text-xs ${isDark ? 'divide-[#1C2B42]/50' : 'divide-[#E2E8F0]'}`}>
          <div className="flex justify-between py-2.5">
            <span className={isDark ? 'text-[#8D9BB0]' : 'text-[#64748B]'}>{t.androidVersion}</span>
            <span className={`font-medium ${isDark ? 'text-white' : 'text-[#0F172A]'}`}>{specs.androidVersion}</span>
          </div>

          <div className="flex justify-between py-2.5">
            <span className={isDark ? 'text-[#8D9BB0]' : 'text-[#64748B]'}>{t.deviceModel}</span>
            <span className={`font-medium ${isDark ? 'text-white' : 'text-[#0F172A]'}`}>{specs.brand} {specs.model}</span>
          </div>

          <div className="flex justify-between py-2.5">
            <span className={isDark ? 'text-[#8D9BB0]' : 'text-[#64748B]'}>{t.systemRam}</span>
            <span className={`font-medium ${isDark ? 'text-white' : 'text-[#0F172A]'}`}>{specs.totalRam}</span>
          </div>

          <div className="flex justify-between py-2.5">
            <span className={isDark ? 'text-[#8D9BB0]' : 'text-[#64748B]'}>{t.resolution}</span>
            <span className={`font-medium font-mono ${isDark ? 'text-white' : 'text-[#0F172A]'}`}>{specs.screenResolution}</span>
          </div>

          <div className="flex justify-between py-2.5">
            <span className={isDark ? 'text-[#8D9BB0]' : 'text-[#64748B]'}>{t.processor}</span>
            <span className={`font-medium ${isDark ? 'text-white' : 'text-[#0F172A]'}`}>{specs.cpu}</span>
          </div>
        </div>

        <p className={`text-[11px] mt-3 leading-relaxed border-t pt-2.5 ${isDark ? 'text-[#55657E] border-[#1C2B42]/50' : 'text-[#94A3B8] border-[#E2E8F0]'}`}>
          This Device ID is used for license verification and hardware binding.
        </p>
      </div>

      {/* Keep your Device ID safe Box */}
      <div className="w-full rounded-2xl bg-[#39B9FF]/10 border border-[#39B9FF]/25 p-3.5 flex items-start gap-3">
        <div className="w-6 h-6 rounded-lg bg-[#39B9FF]/20 flex items-center justify-center text-[#39B9FF] shrink-0 mt-0.5">
          <Info className="w-4 h-4" />
        </div>
        <div className="flex flex-col">
          <span className="text-xs font-bold text-[#39B9FF]">Keep your Device ID safe</span>
          <span className={`text-[11px] mt-0.5 leading-snug ${isDark ? 'text-[#8D9BB0]' : 'text-[#64748B]'}`}>
            Do not share it with anyone except the authorized seller.
          </span>
        </div>
      </div>
    </div>
  );
};
