import Card from '@mui/material/Card';
import CardContent from '@mui/material/CardContent';
import Typography from '@mui/material/Typography';

interface KpiCardProps {
  value: string | number;
  label: string;
  color?: string;
}

export default function KpiCard({ value, label, color = 'primary.main' }: KpiCardProps) {
  return (
    <Card variant="outlined">
      <CardContent sx={{ textAlign: 'center', py: 1.5, '&:last-child': { pb: 1.5 } }}>
        <Typography variant="h4" color={color}>
          {value}
        </Typography>
        <Typography variant="caption" color="text.secondary">
          {label}
        </Typography>
      </CardContent>
    </Card>
  );
}
