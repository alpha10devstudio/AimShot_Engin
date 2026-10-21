import React, { useState } from 'react';
import { ArrowLeft, Cpu, Copy, Check, Info } from 'lucide-react';
import { DEFAULT_DEVICE_SPECS } from '../../data/mockData';

interface DeviceInfoScreenProps {
  onBack: () => void;
  onShowToast: (message: string) => void;
}

export const DeviceInfoScreen: React.FC<DeviceInfoScreenProps> = ({
  onBack,
  onShowToast
}) => {
  const [copied, setCopied] = useState(false);
  const specs = DEFAULT_DEVICE_SPECS;

  const handleCopy = () => {
    navigator.clipboard.writeText(specs.deviceId);
    setCopied(true);
    onShowToast('Device ID copied to clipboard');
    setTimeout(() => setCopied(false), 2000);
  };

  return (
    <div className="w-full h-full flex flex-col bg-[#0B0F17] overflow-y-auto px-4 py-3 select-none">
      {/* Top Action Bar matching Screen 7 */}
      <div className="flex items-center gap-2 mb-4 pt-1">
        <button
          onClick={onBack}
          className="w-9 h-9 rounded-xl bg-[#111B2A] border border-[#1C2B42] flex items-center justify-center text-white hover:text-[#39B9FF] transition-colors"
        >
          <ArrowLeft className="w-5 h-5" />
        </button>
        <h2 className="text-base font-bold text-white tracking-wide">Device Information</h2>
      </div>

      {/* Device ID Card matching Screen 7 */}
      <div className="w-full bg-[#111B2A] rounded-2xl p-4 mb-4 border border-[#39B9FF]/50 shadow-[0_0_15px_rgba(57,185,255,0.15)]">
        <div className="flex items-center gap-2 text-xs text-[#8D9BB0] mb-2.5">
          <Cpu className="w-4 h-4 text-[#39B9FF]" />
          <span>Device ID</span>
        </div>

        <div className="flex items-center justify-between gap-2">
          <span className="text-sm font-bold text-white font-mono tracking-wider truncate">
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

      {/* Device Hardware Specs Card matching Screen 7 */}
      <div className="w-full bg-[#111B2A] rounded-2xl p-4 mb-4 border border-[#1C2B42]">
        <div className="divide-y divide-[#1C2B42]/50 text-xs">
          <div className="flex justify-between py-2.5">
            <span className="text-[#8D9BB0]">Android Version</span>
            <span className="text-white font-medium">{specs.androidVersion}</span>
          </div>

          <div className="flex justify-between py-2.5">
            <span className="text-[#8D9BB0]">Model</span>
            <span className="text-white font-medium">{specs.model}</span>
          </div>

          <div className="flex justify-between py-2.5">
            <span className="text-[#8D9BB0]">Brand</span>
            <span className="text-white font-medium">{specs.brand}</span>
          </div>

          <div className="flex justify-between py-2.5">
            <span className="text-[#8D9BB0]">Total RAM</span>
            <span className="text-white font-medium">{specs.totalRam}</span>
          </div>

          <div className="flex justify-between py-2.5">
            <span className="text-[#8D9BB0]">Screen Resolution</span>
            <span className="text-white font-medium font-mono">{specs.screenResolution}</span>
          </div>

          <div className="flex justify-between py-2.5">
            <span className="text-[#8D9BB0]">CPU</span>
            <span className="text-white font-medium">{specs.cpu}</span>
          </div>
        </div>

        <p className="text-[11px] text-[#55657E] mt-3 leading-relaxed border-t border-[#1C2B42]/50 pt-2.5">
          This Device ID is used for license verification and device binding.
        </p>
      </div>

      {/* Keep your Device ID safe Box matching Screen 7 */}
      <div className="w-full rounded-2xl bg-[#39B9FF]/10 border border-[#39B9FF]/25 p-3.5 flex items-start gap-3">
        <div className="w-6 h-6 rounded-lg bg-[#39B9FF]/20 flex items-center justify-center text-[#39B9FF] shrink-0 mt-0.5">
          <Info className="w-4 h-4" />
        </div>
        <div className="flex flex-col">
          <span className="text-xs font-bold text-[#39B9FF]">Keep your Device ID safe</span>
          <span className="text-[11px] text-[#8D9BB0] mt-0.5 leading-snug">
            Do not share it with anyone except the official seller.
          </span>
        </div>
      </div>
    </div>
  );
};
