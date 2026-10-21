import React, { useEffect, useState } from 'react';
import { TacticalReticleLogo } from '../TacticalReticleLogo';

interface SplashScreenProps {
  onFinish: () => void;
}

export const SplashScreen: React.FC<SplashScreenProps> = ({ onFinish }) => {
  const [progress, setProgress] = useState(0);

  useEffect(() => {
    const timer = setInterval(() => {
      setProgress((prev) => {
        if (prev >= 100) {
          clearInterval(timer);
          setTimeout(onFinish, 300);
          return 100;
        }
        return prev + 5;
      });
    }, 45);

    return () => clearInterval(timer);
  }, [onFinish]);

  return (
    <div className="relative w-full h-full flex flex-col items-center justify-between p-8 bg-[#0B0F17] overflow-hidden select-none">
      {/* Background Cyber Military Grid & Ambient Glow */}
      <div className="absolute inset-0 bg-[radial-gradient(ellipse_at_top,_var(--tw-gradient-stops))] from-[#14283D] via-[#0B0F17] to-[#05080E] opacity-70 pointer-events-none" />
      <div className="absolute top-1/4 -left-20 w-64 h-64 bg-[#39B9FF]/10 rounded-full blur-3xl pointer-events-none" />
      <div className="absolute bottom-1/4 -right-20 w-64 h-64 bg-[#39B9FF]/10 rounded-full blur-3xl pointer-events-none" />

      {/* Cyber line accents */}
      <div className="absolute top-0 left-0 w-full h-[2px] bg-gradient-to-r from-transparent via-[#39B9FF]/50 to-transparent" />
      <div className="absolute bottom-0 left-0 w-full h-[2px] bg-gradient-to-r from-transparent via-[#39B9FF]/50 to-transparent" />

      <div className="w-full flex justify-between items-center z-10 pt-2 text-[11px] text-[#8D9BB0] font-mono">
        <span>V1.0.0</span>
        <span className="flex items-center gap-1">
          <span className="w-2 h-2 rounded-full bg-[#49DCA0] animate-pulse" />
          SYSTEM ONLINE
        </span>
      </div>

      {/* Center Branding Content matching Mockup Screen 1 */}
      <div className="flex flex-col items-center text-center my-auto z-10">
        <div className="relative mb-6">
          <TacticalReticleLogo size={130} />
        </div>

        <div className="flex items-baseline gap-2 mb-2">
          <span className="text-3xl font-extrabold tracking-wider text-white">AimShot</span>
          <span className="text-3xl font-extrabold tracking-widest text-[#39B9FF] drop-shadow-[0_0_12px_rgba(57,185,255,0.8)]">
            ENGINE
          </span>
        </div>

        <p className="text-[11px] font-semibold tracking-[0.2em] text-[#8D9BB0] uppercase">
          SHARPER AIM • BETTER GAME • HIGHER YOU
        </p>
      </div>

      {/* Bottom Loading Progress Indicator */}
      <div className="w-full max-w-xs flex flex-col items-center z-10 pb-6">
        <div className="flex justify-between w-full text-[12px] text-[#8D9BB0] mb-2 font-mono">
          <span>Loading Engine...</span>
          <span className="text-[#39B9FF]">{progress}%</span>
        </div>

        <div className="w-full h-1.5 bg-[#111B2A] rounded-full overflow-hidden border border-[#1C2B42]">
          <div
            className="h-full bg-gradient-to-r from-[#1A85CC] to-[#39B9FF] rounded-full transition-all duration-75 ease-out shadow-[0_0_10px_#39B9FF]"
            style={{ width: `${progress}%` }}
          />
        </div>

        <button
          onClick={onFinish}
          className="mt-4 text-[11px] text-[#55657E] hover:text-[#39B9FF] transition-colors"
        >
          Skip Intro →
        </button>
      </div>
    </div>
  );
};
