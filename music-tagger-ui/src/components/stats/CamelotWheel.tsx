import { useTheme } from '@mui/material/styles';
import Typography from '@mui/material/Typography';
import Box from '@mui/material/Box';

/**
 * Standard key → Camelot code mapping.
 * Outer ring = major (B), inner ring = minor (A).
 */
const CAMELOT_MAP: Record<string, { code: string; ring: 'outer' | 'inner'; index: number }> = {
  'B':   { code: '1B', ring: 'outer', index: 0 },
  'F#':  { code: '2B', ring: 'outer', index: 1 },
  'Db':  { code: '3B', ring: 'outer', index: 2 },
  'Ab':  { code: '4B', ring: 'outer', index: 3 },
  'Eb':  { code: '5B', ring: 'outer', index: 4 },
  'Bb':  { code: '6B', ring: 'outer', index: 5 },
  'F':   { code: '7B', ring: 'outer', index: 6 },
  'C':   { code: '8B', ring: 'outer', index: 7 },
  'G':   { code: '9B', ring: 'outer', index: 8 },
  'D':   { code: '10B', ring: 'outer', index: 9 },
  'A':   { code: '11B', ring: 'outer', index: 10 },
  'E':   { code: '12B', ring: 'outer', index: 11 },
  'G#m': { code: '1A', ring: 'inner', index: 0 },
  'Abm': { code: '1A', ring: 'inner', index: 0 },
  'D#m': { code: '2A', ring: 'inner', index: 1 },
  'Ebm': { code: '2A', ring: 'inner', index: 1 },
  'Bbm': { code: '3A', ring: 'inner', index: 2 },
  'Fm':  { code: '4A', ring: 'inner', index: 3 },
  'Cm':  { code: '5A', ring: 'inner', index: 4 },
  'Gm':  { code: '6A', ring: 'inner', index: 5 },
  'Dm':  { code: '7A', ring: 'inner', index: 6 },
  'Am':  { code: '8A', ring: 'inner', index: 7 },
  'Em':  { code: '9A', ring: 'inner', index: 8 },
  'Bm':  { code: '10A', ring: 'inner', index: 9 },
  'F#m': { code: '11A', ring: 'inner', index: 10 },
  'C#m': { code: '12A', ring: 'inner', index: 11 },
  'Dbm': { code: '12A', ring: 'inner', index: 11 },
};

/** Reverse lookup: Camelot code → accumulated count. */
function buildCamelotCounts(keyDistribution: Record<string, number>): Map<string, number> {
  const counts = new Map<string, number>();
  for (const [key, count] of Object.entries(keyDistribution)) {
    const entry = CAMELOT_MAP[key];
    if (!entry) continue;
    counts.set(entry.code, (counts.get(entry.code) ?? 0) + count);
  }
  return counts;
}

function polarToCartesian(cx: number, cy: number, r: number, angleDeg: number) {
  const rad = ((angleDeg - 90) * Math.PI) / 180;
  return { x: cx + r * Math.cos(rad), y: cy + r * Math.sin(rad) };
}

function arcPath(cx: number, cy: number, rInner: number, rOuter: number, startDeg: number, endDeg: number) {
  const s1 = polarToCartesian(cx, cy, rOuter, startDeg);
  const e1 = polarToCartesian(cx, cy, rOuter, endDeg);
  const s2 = polarToCartesian(cx, cy, rInner, endDeg);
  const e2 = polarToCartesian(cx, cy, rInner, startDeg);
  return [
    `M ${s1.x} ${s1.y}`,
    `A ${rOuter} ${rOuter} 0 0 1 ${e1.x} ${e1.y}`,
    `L ${s2.x} ${s2.y}`,
    `A ${rInner} ${rInner} 0 0 0 ${e2.x} ${e2.y}`,
    'Z',
  ].join(' ');
}

interface CamelotWheelProps {
  keyDistribution: Record<string, number>;
}

export default function CamelotWheel({ keyDistribution }: CamelotWheelProps) {
  const theme = useTheme();
  const counts = buildCamelotCounts(keyDistribution);
  const maxCount = Math.max(1, ...counts.values());

  const cx = 150;
  const cy = 150;
  const segments = 12;
  const segAngle = 360 / segments;
  const textColor = theme.palette.text.primary;
  const accentColor = theme.palette.primary.main;

  const rings: { label: string; rInner: number; rOuter: number; suffix: string }[] = [
    { label: 'Major', rInner: 80, rOuter: 140, suffix: 'B' },
    { label: 'Minor', rInner: 30, rOuter: 75, suffix: 'A' },
  ];

  if (counts.size === 0) {
    return (
      <Box sx={{ textAlign: 'center', py: 4 }}>
        <Typography variant="body2" color="text.secondary">No key data available</Typography>
      </Box>
    );
  }

  return (
    <svg viewBox="0 0 300 300" width="100%" style={{ maxWidth: 300 }}>
      {rings.map((ring) =>
        Array.from({ length: segments }, (_, i) => {
          const code = `${i + 1}${ring.suffix}`;
          const count = counts.get(code) ?? 0;
          const opacity = count > 0 ? 0.2 + 0.8 * (count / maxCount) : 0.05;
          const startDeg = i * segAngle;
          const endDeg = startDeg + segAngle;
          const midDeg = startDeg + segAngle / 2;
          const labelR = (ring.rInner + ring.rOuter) / 2;
          const labelPos = polarToCartesian(cx, cy, labelR, midDeg);

          return (
            <g key={code}>
              <path
                d={arcPath(cx, cy, ring.rInner, ring.rOuter, startDeg, endDeg)}
                fill={accentColor}
                fillOpacity={opacity}
                stroke={theme.palette.divider}
                strokeWidth={1}
              />
              <text
                x={labelPos.x}
                y={labelPos.y}
                textAnchor="middle"
                dominantBaseline="central"
                fill={textColor}
                fontSize={ring.suffix === 'B' ? 10 : 8}
                fontWeight={count > 0 ? 600 : 400}
              >
                {code}
              </text>
            </g>
          );
        }),
      )}
    </svg>
  );
}
