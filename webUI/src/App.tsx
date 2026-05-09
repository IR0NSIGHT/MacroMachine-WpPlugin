import { useEffect, useState } from 'react'
import Container from '@mui/material/Container'
import Typography from '@mui/material/Typography'
import './App.css'
import '@fontsource/ubuntu';
import { components } from './generated/api-types'
import { fetchExecutionQueue, fetchMacros, postQueueMacros } from './API/fetch'
import { Button, Grid } from '@mui/material'
import Item from '@mui/material/Grid'
import MacroCard from './components/MacroList/MacroCard'


type MacroDTO = components["schemas"]["MacroDTO"];

function MacroList() {
    const [macros, setMacros] = useState<MacroDTO[]>([]);
    const [loading, setLoading] = useState(true);
    const [queue, setQueue] = useState<string[]>([]);

    const uuidToMacro = new Map<string, MacroDTO>();
    macros.forEach(macro => uuidToMacro.set(macro.uid, macro));

    // Poll every 200ms
    setInterval(async () => {
        try {
            const queue = await fetchExecutionQueue();
            setQueue(queue.queuedMacroIds);
            console.log("Updated queue:", queue);
        } catch (err) {
            console.error(err);
        }
    }, 2000);

    useEffect(() => {
        fetchMacros()
            .then(list => setMacros(list.sort((a, b) => a.name.localeCompare(b.name))))
            .catch(console.error)
            .finally(() => setLoading(false));
    }, []);

    if (loading) {
        return <div>Loading...</div>;
    }

    return (
        <div>
            <div>
                <h2>Execution Queue:</h2>
                {queue.map(uid => uuidToMacro.get(uid)).map(macro => <div key={macro?.uid}>{macro?.name}</div>)}
            </div>

            <Grid container spacing={1}>
                {macros.map((macro) => (
                    <Grid key={macro.uid} size={4}>
                        < Item  >
                            <MacroCard
                                macro={macro}
                                execution={{ isRunning: false, percentage: 0 }}
                            />
                            <Button variant="contained" onClick={() => postQueueMacros([macro.uid]).then(console.log).catch(console.error)}>
                                Queue
                            </Button>
                        </Item>

                    </Grid>
                ))}
            </Grid>
        </div>
    );
}

function App() {
    return (
        <Container sx={{ py: 4 }}>
            <Typography variant="h4" component="h1" gutterBottom>
                MacroMachine Web UI
            </Typography>

            <MacroList />
        </Container>
    )
}

export default App
