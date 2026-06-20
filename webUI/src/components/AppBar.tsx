import AppBar from "@mui/material/AppBar";
import Box from "@mui/material/Box";
import Toolbar from "@mui/material/Toolbar";
import Typography from "@mui/material/Typography";
import { ExecutionQueueDTO, ExecutionStateDTO } from "@/types/DTO";
import CircularProgress from "@mui/material/CircularProgress";

export const executionProgress = (execution?: ExecutionStateDTO): number => {
  const percentage =
    execution?.currentStepIndex !== undefined && execution?.steps && execution.steps.length !== 0
      ? ((execution.currentStepIndex +
          execution.steps[execution.currentStepIndex].percentComplete / 100) /
          execution.steps.length) *
        100
      : 0;
  return percentage;
};

export function PrimaryAppBar(props: {
  queue?: ExecutionQueueDTO;
  executionState?: ExecutionStateDTO;
  connection: "error" | "ok" | "loading";
}) {
  return (
    <Box sx={{ height: "64px" }}>
      <AppBar position="static">
        <Toolbar>
          <Typography
            variant="h6"
            noWrap
            component="div"
            sx={{ display: { xs: "none", sm: "block" } }}
          >
            MacroMachine
          </Typography>
          {props.executionState && props.executionState.status !== "IDLE" && (
            <CircularProgress
              key={-1}
              enableTrackSlot
              variant="determinate"
              color="secondary"
              value={100 - executionProgress(props.executionState)}
              aria-label="Upload photos"
            />
          )}
          {props.queue?.queuedMacroIds.map((id, idx) => (
            <CircularProgress
              key={idx}
              enableTrackSlot
              variant="determinate"
              color="secondary"
              value={100}
              aria-label="Upload photos"
            />
          ))}
          <Box sx={{ flexGrow: 1 }} />
          {props.connection === "error" && (
            <div style={{ color: "red" }}>No connection to backend.</div>
          )}
          {props.connection === "loading" && <div style={{ color: "yellow" }}>Connecting...</div>}
          {props.connection === "ok" && <div style={{ color: "green" }}>Connected to backend.</div>}
        </Toolbar>
      </AppBar>
    </Box>
  );
}
