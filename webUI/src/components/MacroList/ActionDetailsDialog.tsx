import {
  explainSingleFilterMapping,
  filterAutoName,
  invertFilterSinglePosition,
  namedMapping,
  StepItemType,
} from "@/features/Filters";
import ReactMarkdown from "react-markdown";
import { valueToString } from "@/features/InputOutput";
import { MacroDTO, ActionDTO } from "@/types/DTO";
import {
  Dialog,
  DialogTitle,
  DialogContent,
  Stack,
  Typography,
  Chip,
  Box,
  Tooltip,
} from "@mui/material";
import { useState } from "react";

type Props = {
  open: boolean;
  action: ActionDTO | undefined;
  setAction: (item: ActionDTO) => void;
  onClose: () => void;
  onViewItem: (item: MacroDTO | ActionDTO) => void;
};

type FilterEditorProps = {
  open: boolean;
  action: StepItemType | undefined;
  setAction: (item: StepItemType) => void;
  onClose: () => void;
  onViewItem: (item: MacroDTO | ActionDTO) => void;
};

export function ActionDetailsDialog({ open, action, onClose }: Props) {
  if (!action) return null;
  const outputToString = valueToString(action.output);
  const inputToString = valueToString(action.input);
  return (
    <Dialog open={open} onClose={onClose} maxWidth="md" fullWidth>
      <DialogTitle>{action.name}</DialogTitle>

      <DialogContent>
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
      </DialogContent>
    </Dialog>
  );
}

export function FilterValueDialog({ open, action, onClose, setAction }: FilterEditorProps) {
  const [actionState, setActionState] = useState<StepItemType | undefined>(action);

  if (!actionState) return null;
  return (
    <Dialog
      open={open}
      onClose={() => {
        if (actionState) setAction(actionState);
        onClose();
      }}
      maxWidth="md"
      fullWidth
    >
      <DialogTitle>{actionState.name}</DialogTitle>

      <DialogContent>
        <Stack spacing={2}>
          <Typography color="text.secondary">{actionState.description}</Typography>
          <Stack direction="column" spacing={1} flexWrap="wrap">
            {namedMapping(actionState).map((mapping) => {
              return (
                <Box key={mapping.input}>
                  <Tooltip
                    title={
                      <ReactMarkdown>
                        {explainSingleFilterMapping(mapping, actionState.input.displayName)}
                      </ReactMarkdown>
                    }
                  >
                    <Chip
                      label={mapping.inputName + ": " + mapping.outputName}
                      size="small"
                      onClick={() => {
                        const newFilter = filterAutoName(
                          invertFilterSinglePosition(actionState, mapping.input),
                        );
                        setActionState(newFilter);
                      }}
                    />
                  </Tooltip>
                </Box>
              );
            })}
          </Stack>
        </Stack>
      </DialogContent>
    </Dialog>
  );
}
