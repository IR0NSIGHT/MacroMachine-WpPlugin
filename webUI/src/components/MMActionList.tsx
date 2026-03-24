import Container from '@mui/material/Container'
import Typography from '@mui/material/Typography'
import Stack from '@mui/material/Stack'
import { MMAction } from '../types/MMAction'
import MMActionRenderer from './MMActionRenderer'

interface MMActionListProps {
  actions: MMAction[]
  title?: string
}

export default function MMActionList({ actions, title = 'Actions' }: MMActionListProps) {
  if (!actions || actions.length === 0) {
    return (
      <Typography color="text.secondary">
        No actions to display.
      </Typography>
    )
  }

  return (
    <Container maxWidth="lg" sx={{ py: 2 }}>
      {title && (
        <Typography variant="h5" gutterBottom>
          {title}
        </Typography>
      )}
      <Stack spacing={1}>
        {actions.map((action) => (
          <MMActionRenderer key={action.uid} action={action} />
        ))}
      </Stack>
    </Container>
  )
}
