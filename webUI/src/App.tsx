import { useEffect, useState } from "react";
import "./App.css";
import "@fontsource/ubuntu";
import {
  fetchActions,
  fetchExecutionQueue,
  fetchExecutionState,
  fetchMacros,
} from "./API/fetch";
import { Box } from "@mui/material";
import { PrimarySearchAppBar } from "./components/AppBar";
import {
  ExecutionQueueDTO,
  MacroDTO,
  ExecutionStateDTO,
  ActionDTO,
} from "./types/DTO";
import { MacroGrid } from "./MacroGrid";


export default function App() {
  const [search, setSearch] = useState("");

  const [macros, setMacros] = useState<MacroDTO[]>([]);
  const [actions, setActions] = useState<ActionDTO[]>([]);
  const [executionState, setExecutionState] = useState<ExecutionStateDTO>({
    executionId: "",
    steps: [],
    currentStepIndex: 0,
    status: "IDLE",
  });
  const [_queue, setQueue] = useState<ExecutionQueueDTO>({
    queuedMacroIds: [],
  });
  const [connectionLost, setConnectionLost] = useState(false);

  useEffect(() => {
    const interval = setInterval(async () => {
      await fetchExecutionQueue()
        .then((r) => setQueue(r))
        .catch(() => setConnectionLost(true));
      await fetchExecutionState()
        .then(setExecutionState)
        .catch(() => setConnectionLost(true));
      await fetchActions()
        .then(setActions)
        .catch(() => setConnectionLost(true));
      await fetchMacros()
        .then((list) =>
          setMacros(list.sort((a, b) => a.name.localeCompare(b.name))),
        )
        .catch(() => setConnectionLost(true));
    }, 300);

    return () => clearInterval(interval);
  }, []);

  return (
    <Box
      sx={{
        display: "flex",
        flexDirection: "column",
        height: "100vh",
      }}
    >
      <PrimarySearchAppBar
        search={search}
        onSearchChange={setSearch}
        queue={_queue}
        executionState={executionState}
      />
      <Box sx={{ flex: 1, minHeight: 0 }}>
        {connectionLost && (
          <div style={{ color: "red" }}>No connection to backend.</div>
        )}
        {!connectionLost && (
          <MacroGrid
            macros={macros.filter(
              (macro) =>
                search == "" ||
                macro.name.toLowerCase().includes(search.toLowerCase()),
            )}
            actions={actions}
            executionState={executionState}
          />
        )}
      </Box>
    </Box>
  );
}
