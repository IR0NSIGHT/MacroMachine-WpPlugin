import {useState, useEffect} from 'react'
import Container from '@mui/material/Container'
import Typography from '@mui/material/Typography'
import CircularProgress from '@mui/material/CircularProgress'
import Alert from '@mui/material/Alert'
import MMActionRenderer from './components/MMActionRenderer'
import {assertMMAction, MMAction} from './types/MMAction'
import './App.css'
import DebugPanel from "@/components/DebugPanel";

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
        fetch('/action')
            .then(res => res.json())
            .then(data => {
                assertMMAction(data)
                setAction(data)
            })
            .catch((err) => {
                setError(err.message)
                setAction(null)
            })
            .finally(() => setLoading(false))
    }, [])

    return (
        <Container sx={{py: 4}}>
                <Typography variant="h4" component="h1" gutterBottom>
                    MacroMachine Web UI
                </Typography>

                {loading && <CircularProgress/>}
                {error && <Alert severity="error">Failed to load action: {error}</Alert>}
                <DebugPanel/>

                {action && !loading && !error && <MMActionRenderer action={action}/>}
            <pre
                style={{
                    textAlign: "left",
                    fontFamily: 'monospace',
                    padding: 12,
                    borderRadius: 6,

                }}
            >
                {JSON.stringify(action, null, 3)}
            </pre>
        </Container>
    )
}

export default App
