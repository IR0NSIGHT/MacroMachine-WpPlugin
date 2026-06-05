import { useEffect, useState } from "react";
import "./App.css";
import "@fontsource/ubuntu";
import {
  api,
  deleteMacro,
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
import { PrimaryAppBar } from "./components/AppBar";
import { ExecutionQueueDTO, MacroDTO, ExecutionStateDTO, ActionDTO } from "./types/DTO";
import { MacroGrid } from "./MacroGrid";
import { GlobalOperationDesigner } from "./components/GlobalOperationDesigner";
import { isStepItem, isUUID, MacroExecuteRequester, toMacroDTO } from "./features/Execution";
import equal from "fast-deep-equal";
import { LayerManager } from "./LayerManager";
import { InputOutputDTOTypeEnum } from "./generated/client";

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
  const [lastActionChange, setLastActionChange] = useState(0);
  const [lastMacroChange, setLastMacroChange] = useState(0);

  useEffect(() => {
    fetchActions()
      .then((r) => {
        console.log("ACTION CHANGED!");
        setActions((prev) => (equal(prev, r) ? prev : r));
      })
      .catch(console.error);
  }, [lastActionChange]);

  useEffect(() => {
    fetchMacros()
      .then((r) => {
        console.log("MACROS CHANGED!");
        setMacros((prev) => (equal(prev, r) ? prev : r));
      })
      .catch(console.error);
  }, [lastMacroChange]);

  useEffect(() => {
    const interval = setInterval(async () => {
      await fetchExecutionQueue()
        .then((r) => {
          setQueue((prev) => (equal(prev, r) ? prev : r));
          setConnectionLost(false);
        })
        .catch((e) => {
          setConnectionLost(true);
          console.error(e);
        });
      await fetchExecutionState()
        .then((r) => {
          setExecutionState((prev) => (equal(prev, r) ? prev : r));
        })
        .catch(console.error);

      await api.getActionLastChange().then((lastChangeBackend) => {
        setLastActionChange(lastChangeBackend);
      });

      await api.getMacroLastChange().then((lastChangeBackend) => {
        setLastMacroChange(lastChangeBackend);
      });
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

  const layers = Array.from({ length: 20 }, (_, i) => ({
    id: `${i + 1}`,
    name: `Layer ${i + 1}`,
    type: ["NIBBLE", "BIT", "BYTE"][i % 3] as "NIBBLE" | "BIT" | "BYTE",
    custom: i % 2 === 0,
    usedMacros: i % 4 === 0 ? [`Macro${i + 1}`] : [],
    existsInProject: i % 3 !== 0,
  }));

  return (
    <Box
      sx={{
        display: "flex",
        flexDirection: "column",
        height: "100vh",
      }}
    >
      <PrimaryAppBar
        queue={queue}
        executionState={executionState}
        connectionLost={connectionLost}
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
          {tab === 0 && (
            <MacroGrid
              macros={macros.filter(
                (macro) => search == "" || macro.name.toLowerCase().includes(search.toLowerCase()),
              )}
              actions={actions}
              executionState={executionState}
              onRequestExecution={onRequestExecution}
              onDeleteMacro={deleteMacro}
              search={search}
              setSearch={setSearch}
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
          {tab === 2 && <LayerManager layers={layers} />}
        </Box>
      </Box>
    </Box>
  );
}
