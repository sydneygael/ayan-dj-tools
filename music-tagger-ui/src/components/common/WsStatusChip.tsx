import Chip from '@mui/material/Chip';
import FiberManualRecordIcon from '@mui/icons-material/FiberManualRecord';
import { useTranslation } from 'react-i18next';

interface Props {
  connected: boolean;
}

export default function WsStatusChip({ connected }: Props) {
  const { t } = useTranslation();

  return (
    <Chip
      icon={<FiberManualRecordIcon sx={{ fontSize: 10 }} />}
      label={connected ? t('ws.connected') : t('ws.disconnected')}
      size="small"
      variant="outlined"
      color={connected ? 'success' : 'warning'}
      sx={{ '& .MuiChip-icon': { ml: 0.5 } }}
    />
  );
}
