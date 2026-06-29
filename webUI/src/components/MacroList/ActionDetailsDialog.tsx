import { valueToString } from "@/features/InputOutput";
import { MacroDTO, ActionDTO } from "@/types/DTO";
import { Stack, Typography, Chip, Box } from "@mui/material";
import { PopupDialog } from "../SelectDialog";
type Props = {
  open: boolean;
  action: ActionDTO | undefined;
  setAction: (item: ActionDTO) => void;
  onClose: () => void;
  onViewItem: (item: MacroDTO | ActionDTO) => void;
};

export function ActionDetailsDialog({ open, action, onClose }: Props) {
  if (!action) return null;
  const outputToString = valueToString(action.output);
  const inputToString = valueToString(action.input);
  return (
    <PopupDialog open={open} onAbort={onClose} title={action.name}>
      <Stack spacing={2}>
        <Typography color="text.secondary">{action.description}</Typography>

        <Typography variant="subtitle2">Macro ID</Typography>
        <Typography variant="body2" sx={{ fontFamily: "monospace" }}>
          {action.uid}
        </Typography>

        <Typography variant="subtitle2">Input</Typography>
        <Typography variant="body2" sx={{ fontFamily: "monospace" }}>
          {action.input.displayName}
        </Typography>

        <Typography variant="subtitle2">ActionType</Typography>
        <Typography variant="body2" sx={{ fontFamily: "monospace" }}>
          {action.actionType}
        </Typography>

        <Typography variant="subtitle2">Output</Typography>
        <Typography variant="body2" sx={{ fontFamily: "monospace" }}>
          {action.output.displayName}
        </Typography>

        <Typography variant="subtitle2">Steps</Typography>

        <Stack direction="column" spacing={1} flexWrap="wrap">
          {Array.from(
            { length: action.input.max - action.input.min + 1 },
            (_, i) => action.input.min + i,
          ).map((inputNumericValue, idx) => {
            const outputNumericValue = action.mappedOutputs[idx];
            const inputName = inputToString(inputNumericValue);
            const outputName = outputToString(outputNumericValue);
            return (
              <Box key={inputNumericValue}>
                <Chip label={inputName + "->" + outputName} size="small" />
              </Box>
            );
          })}
        </Stack>
      </Stack>
      ;
    </PopupDialog>
  );
}
