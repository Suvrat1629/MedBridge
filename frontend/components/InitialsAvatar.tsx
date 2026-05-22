import React from 'react';

const COLORS = [
  ['#10b981', '#059669'],
  ['#0ea5e9', '#0284c7'],
  ['#8b5cf6', '#7c3aed'],
  ['#f59e0b', '#d97706'],
  ['#ec4899', '#db2777'],
];

function colorFor(name: string) {
  let hash = 0;
  for (let i = 0; i < name.length; i++) hash = name.charCodeAt(i) + ((hash << 5) - hash);
  return COLORS[Math.abs(hash) % COLORS.length];
}

function initials(name: string) {
  const parts = name.trim().split(/\s+/);
  if (parts.length === 1) return parts[0].slice(0, 2).toUpperCase();
  return (parts[0][0] + parts[parts.length - 1][0]).toUpperCase();
}

interface Props {
  name: string;
  size?: number;
  className?: string;
}

export default function InitialsAvatar({ name, size = 128, className = '' }: Props) {
  const [from, to] = colorFor(name || 'U');
  const text = initials(name || 'U');
  const fontSize = Math.round(size * 0.36);

  return (
    <svg
      width={size}
      height={size}
      viewBox={`0 0 ${size} ${size}`}
      xmlns="http://www.w3.org/2000/svg"
      className={className}
      style={{ borderRadius: '50%', display: 'block' }}
    >
      <defs>
        <linearGradient id={`grad-${text}`} x1="0%" y1="0%" x2="100%" y2="100%">
          <stop offset="0%" stopColor={from} />
          <stop offset="100%" stopColor={to} />
        </linearGradient>
      </defs>
      <circle cx={size / 2} cy={size / 2} r={size / 2} fill={`url(#grad-${text})`} />
      <text
        x="50%"
        y="50%"
        dominantBaseline="central"
        textAnchor="middle"
        fill="white"
        fontSize={fontSize}
        fontWeight="700"
        fontFamily="Inter, system-ui, sans-serif"
      >
        {text}
      </text>
    </svg>
  );
}
