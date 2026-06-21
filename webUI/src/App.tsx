import { useState } from "react";
import "./App.css";
import "@fontsource/ubuntu";
import { deleteMacro, postActions, postMacro, postQueueMacros } from "./API/fetch";
import EditIcon from "@mui/icons-material/Edit";
import ExploreIcon from "@mui/icons-material/Explore";
import LayersIcon from "@mui/icons-material/Layers";
import SettingsIcon from "@mui/icons-material/Settings";
import { Box, Tab, Tabs, useMediaQuery } from "@mui/material";
import { PrimaryAppBar } from "./components/AppBar";
import { MacroDTO, ActionDTO } from "./types/DTO";
import { MacroGrid } from "./MacroGrid";
import { GlobalOperationDesigner } from "./components/GlobalOperationDesigner";
import { isStepItem, isUUID, MacroExecuteRequester, toMacroDTO } from "./features/Execution";
import { LayerManager } from "./LayerManager";
import HistoryIcon from "@mui/icons-material/History";
import {
  useActionsQuery,
  useExecutionQueueQuery,
  useExecutionStateQuery,
  useLayersQuery,
  useMacrosQuery,
} from "./API/queries";
import { HistoryTab } from "./HistoryViewer";

export const fillParentSx = {
  flex: 1,
  minHeight: 0,
  minWidth: 0,
  border: "2px solid red",
};

export default function App() {
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
    //FIXME this should invalidate the macro and actions queries so they are refetched.
    console.log("post actions:", actions, "then post macro", macro);
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

  const isNarrowSideBare = useMediaQuery("(max-width: 600px), (max-height: 440px)");
  const tabSx = {
    width: isNarrowSideBare ? 36 : 90,
    minWidth: 0,
    flex: "0 0 auto",
  };
  return (
    <Box
      sx={{
        // root component
        height: "100vh",
        width: "100vw",
        display: "flex",
        flexDirection: "column",
        overflow: "hidden",
        border: "2px solid green",
      }}
    >
      <PrimaryAppBar queue={queue} executionState={executionState} connection={connection} />
      <Box
        sx={{
          ...fillParentSx,
          flexDirection: "row",
          display: "flex",
        }}
      >
        <Tabs
          orientation="vertical"
          value={tab}
          onChange={(_, v) => setTab(v)}
          sx={{
            borderRight: 1,
            borderColor: "divider",
            ...tabSx,
          }}
        >
          <Tab
            icon={<ExploreIcon />}
            label={isNarrowSideBare ? undefined : "Explorer"}
            sx={tabSx}
          />
          <Tab icon={<EditIcon />} label={isNarrowSideBare ? undefined : "Editor"} sx={tabSx} />
          <Tab
            icon={<LayersIcon />}
            label={isNarrowSideBare ? undefined : "Layer Manager"}
            sx={tabSx}
          />
          <Tab icon={<HistoryIcon />} label={isNarrowSideBare ? undefined : "History"} sx={tabSx} />
          <Tab
            icon={<SettingsIcon />}
            label={isNarrowSideBare ? undefined : "Settings"}
            sx={tabSx}
          />
        </Tabs>
        {tab === 0 && (
          <MacroGrid
            macros={macros}
            actions={actions}
            executionState={executionState}
            onRequestExecution={onRequestExecution}
            onDeleteMacro={deleteMacro}
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
        {tab === 3 && <HistoryTab />}
        {tab === 4 && <div>Settings - Not implemented yet</div>}
      </Box>
    </Box>
  );
}
