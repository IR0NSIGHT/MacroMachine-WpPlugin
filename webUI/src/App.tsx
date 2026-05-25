import { useEffect, useState } from "react";
import "./App.css";
import "@fontsource/ubuntu";
import {
  fetchActions,
  fetchExecutionQueue,
  fetchExecutionState,
  fetchMacros,
  postActions,
  postMacro,
  postQueueMacros,
} from "./API/fetch";
import EditIcon from "@mui/icons-material/Edit";
import ExploreIcon from "@mui/icons-material/Explore";
import LayersIcon from "@mui/icons-material/Layers";
import SettingsIcon from "@mui/icons-material/Settings";
import { Box, Tab, Tabs } from "@mui/material";
import { PrimarySearchAppBar } from "./components/AppBar";
import { ExecutionQueueDTO, MacroDTO, ExecutionStateDTO, ActionDTO } from "./types/DTO";
import { MacroGrid } from "./MacroGrid";
import { GlobalOperationDesigner } from "./components/GlobalOperationDesigner";
import { isStepItem, isUUID, MacroExecuteRequester, toMacroDTO } from "./features/Execution";
import equal from "fast-deep-equal";

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
  const [queue, setQueue] = useState<ExecutionQueueDTO>({
    queuedMacroIds: [],
  });
  const [connectionLost, setConnectionLost] = useState(false);

  const [tab, setTab] = useState(0);

  useEffect(() => {
    const interval = setInterval(async () => {
      await fetchExecutionQueue()
        .then((r) => {
          if (!equal(r, queue)) setQueue(r);
          setConnectionLost(false);
        })
        .catch((e) => {
          setConnectionLost(true);
          console.error(e);
        });
      await fetchExecutionState()
        .then((r) => {
          if (!equal(r, executionState)) setExecutionState(r);
        })
        .catch(console.error);
      await fetchActions()
        .then((r) => {
          if (!equal(r, actions)) setActions(r);
        })
        .catch(console.error);
      await fetchMacros()
        .then((r) => {
          if (!equal(r, macros)) setMacros(r);
        })
        .catch(console.error);
    }, 300);

    return () => clearInterval(interval); //cleanup timer
  }, []);

  const onRequestExecution: MacroExecuteRequester = (runnable, isDebug) => {
    console.log(runnable);
    if (!isDebug) {
      if (isUUID(runnable)) {
        postQueueMacros([runnable]).then(console.log).catch(console.error);
      } else {
        postActions(runnable.steps.filter(isStepItem))
          .then(() => postMacro(toMacroDTO(runnable)))
          .then(() => postQueueMacros([runnable.uid]))
          .then(console.log)
          .catch(alert);
      }
    } else alert("Warning: Not implemented");
  };

  const onRequestSave = (macro: MacroDTO, actions: ActionDTO[]) => {
    postActions(actions).then(() => postMacro(macro));
  };

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
        queue={queue}
        executionState={executionState}
      />
      <Box
        sx={{
          display: "flex",
          flexDirection: "row",
        }}
      >
        <Tabs
          orientation="vertical"
          value={tab}
          onChange={(_, v) => setTab(v)}
          sx={{
            borderRight: 1,
            borderColor: "divider",
            minWidth: 180,
          }}
        >
          <Tab icon={<ExploreIcon />} label="Explorer" />
          <Tab icon={<EditIcon />} label="Editor" />
          <Tab icon={<LayersIcon />} label="Layer Manager" />
          <Tab icon={<SettingsIcon />} label="Settings" />
        </Tabs>
        <Box sx={{ flexGrow: 1 }}>
          {connectionLost && <div style={{ color: "red" }}>No connection to backend.</div>}
          {tab === 0 && !connectionLost && (
            <MacroGrid
              macros={macros.filter(
                (macro) => search == "" || macro.name.toLowerCase().includes(search.toLowerCase()),
              )}
              actions={actions}
              executionState={executionState}
              onRequestExecution={onRequestExecution}
            />
          )}
          {tab === 1 && (
            <GlobalOperationDesigner
              onSave={onRequestSave}
              onExecute={onRequestExecution}
              macros={macros}
              actions={actions}
            />
          )}
        </Box>
      </Box>
    </Box>
  );
}
