import { PropsWithChildren, useEffect, useState } from "react";
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
import { Box, Grid } from "@mui/material";
import Item from "@mui/material/Grid";
import MacroCard from "./components/MacroList/MacroCard";

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
export function MacroGrid() {
  const [macros, setMacros] = useState<MacroDTO[]>([]);
  const [actions, setActions] = useState<ActionDTO[]>([]);
  const [loading, setLoading] = useState(true);
  const [queue, setQueue] = useState<string[]>([]);
  const [executionState, setExecutionState] = useState<ExecutionStateDTO>({
    executionId: "",
    steps: [],
    currentStepIndex: 0,
    status: "IDLE",
  });
  const uuidToMacro = new Map<string, MacroDTO>();
  const uuidToAction = new Map<string, ActionDTO>();
  macros.forEach((macro) => uuidToMacro.set(macro.uid, macro)); //FIXME useMemo ? or sth?
  actions.forEach((action) => uuidToAction.set(action.uid, action));
  useEffect(() => {
    const interval = setInterval(async () => {
      try {
        await fetchExecutionQueue()
          .then((r) => setQueue(r.queuedMacroIds))
          .catch(console.error);
        await fetchExecutionState().then(setExecutionState).catch(console.error);
        await fetchActions().then(setActions).catch(console.error);
        console.log(queue);
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
              />
            </Item>
          </Grid>
        ))}
      </Grid>
    </Box>
  );
}

export default function AppShell({ children }: PropsWithChildren) {
  return (
    <Box
      sx={{
        height: "100vh",
        display: "flex",
        flexDirection: "column",
        overflow: "hidden", // prevents body scroll leak
      }}
    >
      {/* ================= TOP BAR ================= */}
      <Box
        sx={{
          flexShrink: 0,
          height: 64,
          display: "flex",
          alignItems: "center",
          px: 2,
          borderBottom: 1,
          borderColor: "divider",
        }}
      >
        Top Bar
      </Box>

      {/* ================= MAIN AREA ================= */}
      <Box
        sx={{
          flex: 1,
          minHeight: 0, // 🔴 CRITICAL (allows scrolling children)
          display: "flex",
        }}
      >
        {/* ===== SIDEBAR ===== */}
        <Box
          sx={{
            width: 260,
            flexShrink: 0,
            display: "flex",
            flexDirection: "column",
            borderRight: 1,
            borderColor: "divider",
          }}
        >
          <Box sx={{ p: 2, flexShrink: 0 }}>Sidebar top</Box>

          <Box
            sx={{
              flex: 1,
              minHeight: 0,
              overflowY: "auto",
              p: 2,
            }}
          >
            Sidebar scroll content
          </Box>

          <Box sx={{ p: 2, flexShrink: 0 }}>Sidebar bottom</Box>
        </Box>

        {/* ===== MAIN CONTENT AREA ===== */}
        <Box
          sx={{
            flex: 1,
            minHeight: 0,
            display: "flex",
            flexDirection: "column",
          }}
        >
          {/* inner top (optional) */}
          <Box sx={{ p: 2, flexShrink: 0 }}>Inner Top</Box>

          {/* SCROLLABLE CONTENT (THIS IS YOUR APP) */}
          <Box
            sx={{
              flex: 1,
              minHeight: 0,
              overflowY: "auto",
              p: 2,
            }}
          >
            {children}
          </Box>

          {/* inner bottom (optional) */}
          <Box sx={{ p: 2, flexShrink: 0 }}>Inner Bottom</Box>
        </Box>
      </Box>

      {/* ================= BOTTOM BAR ================= */}
      <Box
        sx={{
          flexShrink: 0,
          height: 56,
          display: "flex",
          alignItems: "center",
          px: 2,
          borderTop: 1,
          borderColor: "divider",
        }}
      >
        Bottom Bar
      </Box>
    </Box>
  );
}
