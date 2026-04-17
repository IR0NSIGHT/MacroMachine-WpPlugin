import { useState } from 'react'
import Container from '@mui/material/Container'
import Typography from '@mui/material/Typography'
import CircularProgress from '@mui/material/CircularProgress'
import './App.css'
import MMActionList from './components/MMActionList'
import { useMacroSystem } from './API/api'
import { UUID } from './types/MMacro'
import { MacroSelector } from './MacroSelector'
import '@fontsource/ubuntu';

function App() {
    const [selectedMacroUUID, setSelected] = useState<UUID>("");
    const { macros: macroList, actions, loading } = useMacroSystem(selectedMacroUUID);
    return (
        <Container sx={{ py: 4 }}>
            <Typography variant="h4" component="h1" gutterBottom>
                MacroMachine Web UI
            </Typography>

            {loading && <CircularProgress />}


            {!loading &&
                <div>
                    <MacroSelector macros={macroList} selectedMacroId={selectedMacroUUID} onChange={setSelected} />
                    <MMActionList actions={actions} />
                </div>
            }
            <pre
                style={{
                    textAlign: "left",
                    fontFamily: 'monospace',
                    padding: 12,
                    borderRadius: 6,

                }}
            >
                {JSON.stringify(macroList, null, 3)}
                {JSON.stringify(actions, null, 3)}
            </pre>
        </Container>
    )
}

export default App
