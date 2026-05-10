import { useEffect, useState } from "react";
import Container from "@mui/material/Container";
import Typography from "@mui/material/Typography";
import "./App.css";
import "@fontsource/ubuntu";
import { components } from "./generated/api-types";
import { fetchActions, fetchExecutionQueue, fetchExecutionState, fetchMacros, postQueueMacros } from "./API/fetch";
import { Box, Grid } from "@mui/material";
import Item from "@mui/material/Grid";
import MacroCard from "./components/MacroList/MacroCard";

type MacroDTO = components["schemas"]["MacroDTO"];
type ActionDTO = components["schemas"]["ActionDTO"];

type ExecutionStateDTO = components["schemas"]["ExecutionStateDTO"];
function MacroList() {
    const [macros, setMacros] = useState<MacroDTO[]>([]);
    const [actions, setActions] = useState<ActionDTO[]>([]);
    const [loading, setLoading] = useState(true);
    const [queue, setQueue] = useState<string[]>([]);
    const [executionState, setExecutionState] = useState<ExecutionStateDTO>({
        executionId: "",
        steps: [],
        currentStepIndex: 0,
        status: "IDLE"
    });
    const uuidToMacro = new Map<string, MacroDTO>();
    const uuidToAction = new Map<string, ActionDTO>();
    macros.forEach((macro) => uuidToMacro.set(macro.uid, macro)); //FIXME useMemo ? or sth?
    actions.forEach((action) => uuidToAction.set(action.uid, action));
    useEffect(() => {
        const interval = setInterval(async () => {
            try {
                await fetchExecutionQueue().then(r => setQueue(r.queuedMacroIds)).catch(console.error);
                await fetchExecutionState().then(setExecutionState).catch(console.error);
                await fetchActions().then(setActions).catch(console.error);
                console.log(queue)


            } catch (err) {
                console.error(err);
            }
        }, 100);

        return () => clearInterval(interval);
    }, []);

    useEffect(() => {
        fetchMacros()
            .then((list) => setMacros(list.sort((a, b) => a.name.localeCompare(b.name))))
            .catch(console.error)
            .finally(() => setLoading(false));
    }, []);

    if (loading) {
        return <div>Loading...</div>;
    }

     return (
    <Box
      sx={{
        height: "100%",
        display: "flex",
        flexDirection: "column",
        minHeight: 0,     // 🔴 CRITICAL
      }}
    >
      {/* TOP */}
      <Box sx={{ p: 2, flexShrink: 0 }}>
        some text top
      </Box>

      {/* SCROLL AREA ONLY */}
      <Box
        sx={{
          flex: 1,
          minHeight: 0,        // 🔴 CRITICAL
          overflowY: "auto",
          p: 2,
        }}
      >
        <Grid container spacing={1}>
          {macros.map((macro) => (
            <Grid key={macro.uid} size={4}>
              <Item>
                <MacroCard
                  macro={macro}
                  execution={executionState}
                  onRequestExecution={() =>
                    postQueueMacros([macro.uid])
                      .then(console.log)
                      .catch(console.error)
                  }
                />
              </Item>
            </Grid>
          ))}
        </Grid>
      </Box>

      {/* BOTTOM */}
      <Box sx={{ p: 2, flexShrink: 0 }}>
        some text bottom
      </Box>
    </Box>
  );
}

function App() {
  return (
    <Container
      disableGutters
      sx={{
        height: "100vh",
        display: "flex",
        flexDirection: "column",
        overflow: "hidden",
      }}
    >
      {/* HEADER */}
      <Typography variant="h4" component="h1" sx={{ p: 2, flexShrink: 0 }}>
        MacroMachine Web UI
      </Typography>

      {/* IMPORTANT: flex child constraint */}
      <Box sx={{ flex: 1, minHeight: 0 }}>
        <MacroList />
      </Box>
    </Container>
  );
}

export default App;
