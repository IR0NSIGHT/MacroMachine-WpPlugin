import { useState, useEffect } from 'react'
import Container from '@mui/material/Container'
import Stack from '@mui/material/Stack'
import Typography from '@mui/material/Typography'
import CircularProgress from '@mui/material/CircularProgress'
import Alert from '@mui/material/Alert'
import MMActionRenderer from './components/MMActionRenderer'
import { MMAction } from './types/MMAction'
import './App.css'

function App() {
  const [action, setAction] = useState<MMAction | null>(null)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)

  useEffect(() => {
    fetch('/action')
      .then((res) => {
        if (!res.ok) throw new Error(`HTTP ${res.status}`)
        return res.json()
      })
      .then((data) => {
        setAction(data)
        setError(null)
      })
      .catch((err) => {
        setError(err.message)
        setAction(null)
      })
      .finally(() => setLoading(false))
  }, [])

  return (
    <Container sx={{ py: 4 }}>
      <Stack spacing={3}>
        <Typography variant="h4" component="h1" gutterBottom>
          MacroMachine Web UI
        </Typography>

        {loading && <CircularProgress />}
        {error && <Alert severity="error">Failed to load action: {error}</Alert>}
        {action && <MMActionRenderer action={action} />}
      </Stack>
    </Container>
  )
}

export default App
