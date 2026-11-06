import React, { useState } from 'react';
import { Key, AlertCircle, Loader2, ExternalLink } from 'lucide-react';
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

  const handleSubmit = async (e?: React.FormEvent) => {
    if (e) e.preventDefault();
    const cleanKey = verificationKey.trim();

    if (!cleanKey) {
      setErrorMessage('Verification key cannot be empty.');
      return;
    }

    setErrorMessage('');
    setIsLoading(true);

    try {
      // Authoritative license verification request
      const response = await fetch('/api/verify-license', {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
          'Accept': 'application/json'
        },
        body: JSON.stringify({
          license_key: cleanKey,
          device_id: 'AS-7F3E-9C4D-2A8B-6E1F',
          device_model: 'AimShot Android Client',
          app_version: '2.4.0'
        })
      });

      const data = await response.json();
      setIsLoading(false);

      if (data && data.valid && data.status === 'active') {
        onSuccess(cleanKey);
        onClose();
      } else {
        const status = (data?.status || '').toLowerCase();
        let displayError = 'Verification failed. Please check your key and try again.';
        if (status === 'invalid') {
          displayError = 'Invalid license key. Please generate a real key from the website.';
        } else if (status === 'expired') {
          displayError = 'Your license has expired. Please generate a new key to continue.';
        } else if (status === 'revoked') {
          displayError = 'This key has been revoked by administration due to policy violation.';
        } else if (status === 'device_limit_reached') {
          displayError = 'Access Denied: This key is already registered to another device.';
        } else if (status === 'device_revoked') {
          displayError = 'This device has been suspended from accessing AimShot Engine.';
        } else if (data?.message) {
          displayError = data.message;
        }
        setErrorMessage(displayError);
      }
    } catch {
      setIsLoading(false);
      setErrorMessage('Unable to connect to security server. Please check your internet connection.');
    }
  };

  const handlePasteKey = async () => {
    try {
      if (navigator?.clipboard?.readText) {
        const text = await navigator.clipboard.readText();
        if (text && text.trim()) {
          setVerificationKey(text.trim());
          setErrorMessage('');
          return;
        }
      }
      setErrorMessage('Clipboard is empty. Please copy a key first.');
    } catch {
      setErrorMessage('Unable to read from clipboard.');
    }
  };

  return (
    <div className="absolute inset-0 z-50 flex items-center justify-center p-4 bg-black/80 backdrop-blur-sm animate-fade-in">
      <div className="w-full max-w-sm bg-[#111B2A] rounded-2xl p-6 border border-[#39B9FF]/60 shadow-[0_0_30px_rgba(57,185,255,0.25)] relative">
        {/* Reticle Logo */}
        <div className="flex justify-center mb-3">
          <TacticalReticleLogo size={46} />
        </div>

        {/* Title & Subtitle */}
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
              placeholder="Paste Verification Key"
              disabled={isLoading}
              className="w-full bg-[#080C12] border border-[#39B9FF]/50 rounded-xl py-3 pl-10 pr-20 text-xs text-white placeholder-[#8D9BB0] focus:outline-none focus:border-[#39B9FF] focus:ring-1 focus:ring-[#39B9FF] font-mono tracking-wider transition-all disabled:opacity-50"
            />

            {/* Compact PASTE button inside input field */}
            <button
              type="button"
              onClick={handlePasteKey}
              className="absolute right-2 px-2.5 py-1 rounded-lg bg-[#14283D] border border-[#39B9FF] text-[10px] text-[#39B9FF] font-bold hover:bg-[#39B9FF]/25 active:scale-95 transition-all cursor-pointer"
            >
              PASTE
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
        <div className="mt-5 pt-4 border-t border-[#1C2B42]/70 text-left">
          <span className="text-xs font-bold text-white block mb-2">How to get your key?</span>
          <ol className="text-[11px] text-[#8D9BB0] space-y-1">
            <li className="flex gap-1.5">
              <span className="text-[#39B9FF] font-bold">1.</span>
              <span>Purchase or generate from official portal</span>
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

          {/* Direct Key Portal Button */}
          <a
            href="https://alpha10.freedev.app/index.php?p=generator"
            target="_blank"
            rel="noopener noreferrer"
            className="mt-3 w-full py-2 px-3 rounded-xl border border-[#39B9FF]/50 text-xs font-bold text-[#39B9FF] hover:bg-[#39B9FF]/10 transition-colors flex items-center justify-center gap-1.5 cursor-pointer"
          >
            <span>Get Free Key</span>
            <ExternalLink className="w-3.5 h-3.5" />
          </a>
        </div>

        {/* Close Button Link */}
        <div className="text-center mt-4">
          <button
            type="button"
            onClick={onClose}
            className="text-xs font-semibold text-[#39B9FF] hover:underline cursor-pointer"
          >
            Close
          </button>
        </div>
      </div>
    </div>
  );
};
