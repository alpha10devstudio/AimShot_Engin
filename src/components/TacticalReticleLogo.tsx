import React from 'react';

interface LogoProps {
  className?: string;
  size?: number;
  glow?: boolean;
}

export const TacticalReticleLogo: React.FC<LogoProps> = ({
  className = '',
  size = 64,
  glow = true
}) => {
  return (
    <div
      className={`relative inline-flex items-center justify-center ${className}`}
      style={{ width: size, height: size }}
    >
      {/* Outer subtle glow */}
      {glow && (
        <div
          className="absolute inset-0 rounded-full bg-[#39B9FF]/20 blur-md pointer-events-none"
          style={{ transform: 'scale(1.15)' }}
        />
      )}

      <svg
        viewBox="0 0 96 96"
        fill="none"
        xmlns="http://www.w3.org/2000/svg"
        className="w-full h-full relative z-10 drop-shadow-[0_0_8px_rgba(57,185,255,0.6)]"
      >
        {/* Outer Circular Reticle with tick gaps */}
        <circle
          cx="48"
          cy="48"
          r="40"
          stroke="#39B9FF"
          strokeWidth="3.2"
          strokeDasharray="52 10"
        />

        {/* 4 Cardinal Crosshair Ticks */}
        <line x1="48" y1="2" x2="48" y2="15" stroke="#39B9FF" strokeWidth="3.5" strokeLinecap="round" />
        <line x1="48" y1="81" x2="48" y2="94" stroke="#39B9FF" strokeWidth="3.5" strokeLinecap="round" />
        <line x1="2" y1="48" x2="15" y2="48" stroke="#39B9FF" strokeWidth="3.5" strokeLinecap="round" />
        <line x1="81" y1="48" x2="94" y2="48" stroke="#39B9FF" strokeWidth="3.5" strokeLinecap="round" />

        {/* Tactical Outer Chevron / Delta Arrow */}
        <polygon
          points="48,22 72,62 56,62 48,48 40,62 24,62"
          fill="#39B9FF"
        />

        {/* Inner Dark Cutout Accent */}
        <polygon
          points="48,34 58,52 52,52 48,44 44,52 38,52"
          fill="#0B0F17"
        />

        {/* Bottom Horizontal Reticle Anchor */}
        <line x1="36" y1="70" x2="60" y2="70" stroke="#39B9FF" strokeWidth="3.5" strokeLinecap="round" />
      </svg>
    </div>
  );
};
