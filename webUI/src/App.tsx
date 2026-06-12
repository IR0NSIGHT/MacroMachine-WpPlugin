import { useState } from "react";
import "./App.css";
import "@fontsource/ubuntu";
import { deleteMacro, postActions, postMacro, postQueueMacros } from "./API/fetch";
import EditIcon from "@mui/icons-material/Edit";
import ExploreIcon from "@mui/icons-material/Explore";
import LayersIcon from "@mui/icons-material/Layers";
import SettingsIcon from "@mui/icons-material/Settings";
import { Box, Tab, Tabs } from "@mui/material";
import { PrimaryAppBar } from "./components/AppBar";
import { MacroDTO, ActionDTO } from "./types/DTO";
import { MacroGrid } from "./MacroGrid";
import { GlobalOperationDesigner } from "./components/GlobalOperationDesigner";
import { isStepItem, isUUID, MacroExecuteRequester, toMacroDTO } from "./features/Execution";
import { LayerManager } from "./LayerManager";
import {
  useActionsQuery,
  useExecutionQueueQuery,
  useExecutionStateQuery,
  useLayersQuery,
  useMacrosQuery,
} from "./API/queries";

export default function App() {
  const [search, setSearch] = useState("");

  const [tab, setTab] = useState(0);

  const {
    data: executionState,
    isLoading: isExecutionStateLoading,
    isError: isExecutionStateError,
    error: executionStateError,
  } = useExecutionStateQuery();
  const { data: macros } = useMacrosQuery();
  const { data: actions } = useActionsQuery();
  const { data: queue } = useExecutionQueueQuery();
  const { data: layers } = useLayersQuery();

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

  console.log(
    "isLoading",
    isExecutionStateLoading,
    "Execution state error:",
    isExecutionStateError,
    executionStateError,
  );

  const connection =
    !executionState && isExecutionStateLoading ? "loading" : executionState ? "ok" : "error";
  console.log("Rerender App!");
  return (
    <Box
      sx={{
        display: "flex",
        flexDirection: "column",
        height: "100vh",
      }}
    >
      <PrimaryAppBar queue={queue} executionState={executionState} connection={connection} />
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
              macros={macros}
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
