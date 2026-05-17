import { useEffect, useState } from "react";
import "./App.css";
import "@fontsource/ubuntu";
import { components } from "./generated/api-types";
import {
  fetchActions,
  fetchExecutionQueue,
  fetchExecutionState,
  fetchMacros,
  postQueueMacros,
} from "./API/fetch";
import { Box, FormControlLabel, Grid, Switch } from "@mui/material";
import Item from "@mui/material/Grid";
import MacroCard from "./components/MacroList/MacroCard";
import { PrimarySearchAppBar } from "./components/AppBar";
import { MacroDetailsDialog } from "./components/MacroList/MacroDetailsDialog";
import { ExecutionQueueDTO } from "./types/DTO";
type MacroDTO = components["schemas"]["MacroDTO"];
type ActionDTO = components["schemas"]["ActionDTO"];

const imageURLs = [
  "https://images.unsplash.com/photo-1506744038136-46273834b3fb?q=80&w=1600&auto=format&fit=crop",
  "https://images.unsplash.com/photo-1500530855697-b586d89ba3ee?q=80&w=1600&auto=format&fit=crop",
  "https://images.unsplash.com/photo-1470770841072-f978cf4d019e?q=80&w=1600&auto=format&fit=crop",
  "https://images.unsplash.com/photo-1441974231531-c6227db76b6e?q=80&w=1600&auto=format&fit=crop",
  "https://images.unsplash.com/photo-1501785888041-af3ef285b470?q=80&w=1600&auto=format&fit=crop",
  "https://images.unsplash.com/photo-1439066615861-d1af74d74000?q=80&w=1600&auto=format&fit=crop",
  "https://images.unsplash.com/photo-1464822759023-fed622ff2c3b?q=80&w=1600&auto=format&fit=crop",
  "https://images.unsplash.com/photo-1500375592092-40eb2168fd21?q=80&w=1600&auto=format&fit=crop",
  "https://images.unsplash.com/photo-1433086966358-54859d0ed716?q=80&w=1600&auto=format&fit=crop",
  "https://images.unsplash.com/photo-1426604966848-d7adac402bff?q=80&w=1600&auto=format&fit=crop",
  "https://images.unsplash.com/photo-1447752875215-b2761acb3c5d?q=80&w=1600&auto=format&fit=crop",
  "https://images.unsplash.com/photo-1443890923422-7819ed4101c0?q=80&w=1600&auto=format&fit=crop",
  "https://images.unsplash.com/photo-1507525428034-b723cf961d3e?q=80&w=1600&auto=format&fit=crop",
  "https://images.unsplash.com/photo-1493246507139-91e8fad9978e?q=80&w=1600&auto=format&fit=crop",
  "https://images.unsplash.com/photo-1469474968028-56623f02e42e?q=80&w=1600&auto=format&fit=crop",
  "https://images.unsplash.com/photo-1451187580459-43490279c0fa?q=80&w=1600&auto=format&fit=crop",
  "https://images.unsplash.com/photo-1418065460487-3e41a6c84dc5?q=80&w=1600&auto=format&fit=crop",
  "https://images.unsplash.com/photo-1502082553048-f009c37129b9?q=80&w=1600&auto=format&fit=crop",
  "https://images.unsplash.com/photo-1448375240586-882707db888b?q=80&w=1600&auto=format&fit=crop",
  "https://images.unsplash.com/photo-1472396961693-142e6e269027?q=80&w=1600&auto=format&fit=crop",
  "https://images.unsplash.com/photo-1465146344425-f00d5f5c8f07?q=80&w=1600&auto=format&fit=crop",
  "https://images.unsplash.com/photo-1431794062232-2a99a5431c6c?q=80&w=1600&auto=format&fit=crop",
  "https://images.unsplash.com/photo-1500534314209-a25ddb2bd429?q=80&w=1600&auto=format&fit=crop",
  "https://images.unsplash.com/photo-1458668383970-8ddd3927deed?q=80&w=1600&auto=format&fit=crop",
  "https://images.unsplash.com/photo-1472214103451-9374bd1c798e?q=80&w=1600&auto=format&fit=crop",
  "https://images.unsplash.com/photo-1501594907352-04cda38ebc29?q=80&w=1600&auto=format&fit=crop",
  "https://images.unsplash.com/photo-1449034446853-66c86144b0ad?q=80&w=1600&auto=format&fit=crop",
  "https://images.unsplash.com/photo-1465189684280-6a8fa9b19a7a?q=80&w=1600&auto=format&fit=crop",
  "https://images.unsplash.com/photo-1504208434309-cb69f4fe52b0?q=80&w=1600&auto=format&fit=crop",
  "https://images.unsplash.com/photo-1505761671935-60b3a7427bad?q=80&w=1600&auto=format&fit=crop",
];

type ExecutionStateDTO = components["schemas"]["ExecutionStateDTO"];
export function MacroGrid({
  macros,
  actions,
  executionState,
}: {
  macros: MacroDTO[];
  actions: ActionDTO[];
  executionState: ExecutionStateDTO;
}) {
  const uuidToMacroOrAction = new Map<string, MacroDTO | ActionDTO>();
  macros.forEach((macro) => uuidToMacroOrAction.set(macro.uid, macro)); //FIXME useMemo ? or sth?
  actions.forEach((action) => uuidToMacroOrAction.set(action.uid, action));
  const [viewedMacro, setViewedMacro] = useState<
    (MacroDTO & { steps: (ActionDTO | MacroDTO)[] }) | null
  >(null);

  const onShare = (macro: MacroDTO) => {
    console.log("USER WANTS TO SHARE THE MARCO", macro.name);
  };

  const onEdit = (macro: MacroDTO) => {
    console.log("USER WANTS TO EDIT THE MARCO", macro.name);
  };

  const onView = (macro: MacroDTO) => {
    const steps: (MacroDTO | ActionDTO)[] = macro.executionUUIDs.map(
      (uid) => uuidToMacroOrAction.get(uid)!,
    );
    const macroWithSteps = { ...macro, steps: steps };
    console.log("fat mamcro:", macroWithSteps);
    console.log("actions:", uuidToMacroOrAction);
    setViewedMacro(macroWithSteps);
  };

  return (
    <div>
      <FormControlLabel control={<Switch defaultChecked />} label="Hide nested macros" />
      <Box
        sx={{
          flex: 1,
          minHeight: 0, // 🔴 CRITICAL
          overflowY: "auto",
          p: 2,
        }}
      >
        <Grid container spacing={1}>
          {macros.map((macro, idx) => (
            <Grid key={macro.uid} size={4}>
              <Item>
                <MacroCard
                  macro={macro}
                  execution={executionState}
                  imageURL={imageURLs[idx % imageURLs.length]}
                  onRequestExecution={() =>
                    postQueueMacros([macro.uid]).then(console.log).catch(console.error)
                  }
                  onView={() => onView(macro)}
                  onShare={() => onShare(macro)}
                  onEdit={() => onEdit(macro)}
                />
              </Item>
            </Grid>
          ))}
        </Grid>

        <MacroDetailsDialog
          open={!!viewedMacro}
          macro={viewedMacro}
          onClose={() => setViewedMacro(null)}
        />
      </Box>
    </div>
  );
}

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
  const [_queue, setQueue] = useState<ExecutionQueueDTO>({ queuedMacroIds: [] });
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
        .then((list) => setMacros(list.sort((a, b) => a.name.localeCompare(b.name))))
        .catch(() => setConnectionLost(true));
    }, 300);

    return () => clearInterval(interval);
  }, []);

  const centerContent = (
    <div>
      {connectionLost && <div style={{ color: "red" }}>No connection to backend.</div>}
      {!connectionLost && (
        <MacroGrid
          macros={macros.filter(
            (macro) => search == "" || macro.name.toLowerCase().includes(search.toLowerCase()),
          )}
          actions={actions}
          executionState={executionState}
        />
      )}
    </div>
  );

  return (
    <div>
      <PrimarySearchAppBar
        search={search}
        onSearchChange={setSearch}
        queue={_queue}
        executionState={executionState}
      />
      {centerContent}
    </div>
  );
}
