import React, { useState } from 'react';
import { Key, AlertCircle, Loader2 } from 'lucide-react';
import { TacticalReticleLogo } from '../TacticalReticleLogo';

interface VerificationModalProps {
  isOpen: boolean;
  onClose: () => void;
  onSuccess: (key: string) => void;
}

export const VerificationModal: React.FC<VerificationModalProps> = ({
  isOpen,
  onClose,
  onSuccess
}) => {
  const [verificationKey, setVerificationKey] = useState('');
  const [isLoading, setIsLoading] = useState(false);
  const [errorMessage, setErrorMessage] = useState('');

  if (!isOpen) return null;

  const handleSubmit = (e?: React.FormEvent) => {
    if (e) e.preventDefault();
    const cleanKey = verificationKey.trim();

    if (!cleanKey) {
      setErrorMessage('Verification key cannot be empty.');
      return;
    }

    setErrorMessage('');
    setIsLoading(true);

    // Simulate authoritative HTTPS verification call with realistic delay
    setTimeout(() => {
      setIsLoading(false);
      // Valid key or demo key
      if (cleanKey.toUpperCase() === 'AIM-PRO-2025-X9' || cleanKey.startsWith('AIM-')) {
        onSuccess(cleanKey);
        onClose();
      } else {
        setErrorMessage('Verification service is not configured yet. (Use test key AIM-PRO-2025-X9)');
      }
    }, 1200);
  };

  const handlePasteDemoKey = () => {
    setVerificationKey('AIM-PRO-2025-X9');
    setErrorMessage('');
  };

  return (
    <div className="absolute inset-0 z-50 flex items-center justify-center p-4 bg-black/80 backdrop-blur-sm animate-fade-in">
      <div className="w-full max-w-sm bg-[#111B2A] rounded-2xl p-6 border border-[#39B9FF]/60 shadow-[0_0_30px_rgba(57,185,255,0.25)] relative">
        {/* Reticle Logo */}
        <div className="flex justify-center mb-3">
          <TacticalReticleLogo size={46} />
        </div>

        {/* Title & Subtitle matching Screen 3 */}
        <h2 className="text-center text-lg font-bold text-white tracking-wide">
          Login to AimShot Engine
        </h2>
        <p className="text-center text-xs text-[#8D9BB0] mt-1 mb-5">
          Enter your verification key to continue
        </p>

        {/* Key Input Field */}
        <form onSubmit={handleSubmit} className="space-y-4">
          <div className="relative flex items-center">
            <div className="absolute left-3.5 text-[#39B9FF] pointer-events-none">
              <Key className="w-4 h-4" />
            </div>

            <input
              type="text"
              value={verificationKey}
              onChange={(e) => {
                setVerificationKey(e.target.value);
                if (errorMessage) setErrorMessage('');
              }}
              placeholder="Paste Key"
              disabled={isLoading}
              className="w-full bg-[#080C12] border border-[#39B9FF]/50 rounded-xl py-3 pl-10 pr-24 text-xs text-white placeholder-[#8D9BB0] focus:outline-none focus:border-[#39B9FF] focus:ring-1 focus:ring-[#39B9FF] font-mono tracking-wider transition-all disabled:opacity-50"
            />

            {/* Quick Paste helper */}
            <button
              type="button"
              onClick={handlePasteDemoKey}
              className="absolute right-2.5 px-2 py-1 rounded bg-[#14283D] border border-[#39B9FF]/30 text-[10px] text-[#39B9FF] font-bold hover:bg-[#39B9FF]/20 transition-colors"
            >
              Paste Demo
            </button>
          </div>

          {/* Error Message */}
          {errorMessage && (
            <div className="flex items-start gap-1.5 p-2 rounded-lg bg-red-950/40 border border-red-500/40 text-red-400 text-[11px] leading-tight">
              <AlertCircle className="w-3.5 h-3.5 shrink-0 mt-0.5" />
              <span>{errorMessage}</span>
            </div>
          )}

          {/* Login Button */}
          <button
            type="submit"
            disabled={isLoading}
            className="w-full py-3 rounded-xl bg-[#39B9FF] hover:bg-[#2da5e6] text-[#0B0F17] font-bold text-sm tracking-wide transition-all shadow-[0_0_15px_rgba(57,185,255,0.4)] flex items-center justify-center gap-2 disabled:opacity-60 cursor-pointer"
          >
            {isLoading ? (
              <>
                <Loader2 className="w-4 h-4 animate-spin" />
                <span>Verifying Key...</span>
              </>
            ) : (
              <span>Login</span>
            )}
          </button>
        </form>

        {/* How to get your key? */}
        <div className="mt-6 pt-4 border-t border-[#1C2B42]/70 text-left">
          <span className="text-xs font-bold text-white block mb-2">How to get your key?</span>
          <ol className="text-[11px] text-[#8D9BB0] space-y-1">
            <li className="flex gap-1.5">
              <span className="text-[#39B9FF] font-bold">1.</span>
              <span>Purchase from authorized seller</span>
            </li>
            <li className="flex gap-1.5">
              <span className="text-[#39B9FF] font-bold">2.</span>
              <span>Get your unique verification key</span>
            </li>
            <li className="flex gap-1.5">
              <span className="text-[#39B9FF] font-bold">3.</span>
              <span>Paste the key and login</span>
            </li>
          </ol>
        </div>

        {/* Close Button Link */}
        <div className="text-center mt-5">
          <button
            type="button"
            onClick={onClose}
            className="text-xs font-semibold text-[#39B9FF] hover:underline"
          >
            Close
          </button>
        </div>
      </div>
    </div>
  );
};
