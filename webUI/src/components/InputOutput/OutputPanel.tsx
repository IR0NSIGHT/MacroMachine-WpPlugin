import Typography from '@mui/material/Typography'
import Stack from '@mui/material/Stack'
import Box from '@mui/material/Box'
import Paper from '@mui/material/Paper'
import Chip from '@mui/material/Chip'
import { PROVIDER_DESCRIPTIONS } from '../../types/ProviderType'

interface OutputProviderProps {
  outputId: string
  outputData: unknown[]
}

export default function OutputPanel({ outputId, outputData }: OutputProviderProps) {
  const displayName = outputId || 'UNKNOWN'
  const description = PROVIDER_DESCRIPTIONS[displayName] || 'Custom provider'

  return (
    <Paper variant="outlined" sx={{ p: 2, backgroundColor: 'action.hover' }}>
      <Stack spacing={1}>
        <Box>
          <Typography variant="subtitle2" color="success" gutterBottom>
            Output Provider
          </Typography>
          <Chip label={displayName} size="small" color="success" variant="outlined" />
        </Box>
        <Typography variant="caption" color="text.secondary">
          {description}
        </Typography>
        {outputData && outputData.length > 0 && (
          <Box>
            <Typography variant="caption" component="div" color="text.secondary">
              <strong>Data:</strong>
            </Typography>
            <Typography variant="caption" component="code" sx={{ display: 'block', wordBreak: 'break-all' }}>
              {JSON.stringify(outputData)}
            </Typography>
          </Box>
        )}
      </Stack>
    </Paper>
  )
}
