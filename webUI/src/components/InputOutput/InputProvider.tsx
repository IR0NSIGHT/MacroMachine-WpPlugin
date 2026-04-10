import Typography from '@mui/material/Typography'
import Stack from '@mui/material/Stack'
import Box from '@mui/material/Box'
import Paper from '@mui/material/Paper'
import Chip from '@mui/material/Chip'
import { PROVIDER_DESCRIPTIONS } from '../../types/ProviderType'
import { InputOutput } from '@/types/InputOutput'

interface InputProviderProps {
  inputOutput: InputOutput
  type: 'input' | 'output'
}

export default function InputOutputDisplay({ inputOutput, type  }: InputProviderProps) {
  const displayName = inputOutput.displayName || 'UNKNOWN'
  const description = PROVIDER_DESCRIPTIONS[displayName] || 'Custom provider'
  const values = inputOutput.parameters.map(param => `${param.name}: ${param.value}`);
  return (
    <Paper variant="outlined" sx={{ p: 2 }}>
      <Stack spacing={1}>
        <Box>
          <Typography variant="subtitle2" color="primary" gutterBottom>
            { type === 'input' ? 'Input Provider' : 'Output Provider' }
          </Typography>
          <Chip label={displayName} size="small" color="primary" variant="outlined" />
        </Box>
        <Typography variant="caption" color="text.secondary">
          {description}
        </Typography>
        {inputOutput.parameters && inputOutput.parameters.length > 0 && (
          <Box>
            <Typography variant="caption" component="div" color="text.secondary">
              <strong>Data:</strong>
            </Typography>
            <Typography variant="caption" component="code" sx={{ display: 'block', wordBreak: 'break-all' }}>
              {JSON.stringify(inputOutput.parameters)}
            </Typography>
          </Box>
        )}
      </Stack>
    </Paper>
  )
}
