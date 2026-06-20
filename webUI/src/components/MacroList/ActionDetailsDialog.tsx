import {
  clearFilter,
  explainSingleFilterMapping,
  filterAutoName,
  filterValueBlock,
  invertFilter,
  invertFilterSinglePosition,
  NamedMapping,
  namedMapping,
} from "@/features/Filters";
import { StepItemType } from "@/features/Execution";
import ReactMarkdown from "react-markdown";
import { valueToString } from "@/features/InputOutput";
import { MacroDTO, ActionDTO } from "@/types/DTO";
import SwitchLeftIcon from "@mui/icons-material/SwitchLeft";
import { Stack, Typography, Chip, Box, Tooltip, Switch, ButtonGroup } from "@mui/material";
import { MMIconButton } from "../IconButton";
import { useMemo, useState } from "react";
import { theme } from "@/theme";
import SortByAlphaIcon from "@mui/icons-material/SortByAlpha";
import ClearIcon from "@mui/icons-material/Clear";
import { PopupDialog } from "../SelectDialog";
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

const sortMappingByInput = (a: NamedMapping, b: NamedMapping) => {
  return a.inputName.localeCompare(b.inputName);
};

const sortMappingByOutput = (a: NamedMapping, b: NamedMapping): number => {
  if (a.outputName === b.outputName) {
    return sortMappingByInput(a, b);
  }
  return a.outputName.localeCompare(b.outputName);
};

export function FilterValueDialog({ open, action, onClose, setAction }: FilterEditorProps) {
  const [actionState, setActionState] = useState<StepItemType | undefined>(action);
  const [sortOrder, setSortOrder] = useState<"input" | "output">("input");
  const sortedMappings = useMemo(() => {
    if (!actionState) return [];
    return namedMapping(actionState).sort(
      sortOrder === "input" ? sortMappingByInput : sortMappingByOutput,
    );
  }, [actionState, sortOrder]);
  if (!actionState) return null;
  return (
    <PopupDialog
      open={open}
      onConfirm={() => {
        if (actionState) setAction(actionState);
        onClose();
      }}
      onAbort={onClose}
      title={actionState.name}
    >
      <Stack spacing={2}>
        <Typography color="text.secondary">{actionState.description}</Typography>
        <ButtonGroup>
          <Tooltip title={"Order by " + (sortOrder === "input" ? "Input" : "Output")}>
            <MMIconButton
              disabled={false}
              onClick={() => setSortOrder((prev) => (prev === "input" ? "output" : "input"))}
              icon={<SortByAlphaIcon />}
              tooltip={"Order by"}
            />
          </Tooltip>
          <Tooltip title={"Invert filter"}>
            <MMIconButton
              disabled={false}
              onClick={() => setActionState(filterAutoName(invertFilter(actionState)))}
              icon={<SwitchLeftIcon />}
              tooltip={"Invert filter"}
            />
          </Tooltip>
          <Tooltip title={"Clear filter"}>
            <MMIconButton
              disabled={false}
              onClick={() => setActionState(filterAutoName(clearFilter(actionState)))}
              icon={<ClearIcon />}
              tooltip={"Clear filter"}
            />
          </Tooltip>
        </ButtonGroup>

        <Stack direction="column" spacing={0} flexWrap="wrap">
          {sortedMappings.map((mapping) => {
            const isActive = mapping.output !== filterValueBlock;
            return (
              <Box
                sx={{
                  display: "flex",
                  flexDirection: "row", // optional, row is the default
                  alignItems: "center",
                }}
                key={mapping.input}
              >
                <Switch
                  checked={isActive}
                  onChange={() => {
                    const newFilter = filterAutoName(
                      invertFilterSinglePosition(actionState, mapping.input),
                    );
                    setActionState(newFilter);
                  }}
                />{" "}
                <Tooltip
                  title={
                    <ReactMarkdown>
                      {explainSingleFilterMapping(mapping, actionState.input.displayName)}
                    </ReactMarkdown>
                  }
                >
                  <Typography
                    sx={{
                      color: !isActive ? theme.palette.text.disabled : theme.palette.text.primary,
                    }}
                  >
                    {mapping.inputName}
                  </Typography>
                </Tooltip>
              </Box>
            );
          })}
        </Stack>
      </Stack>
    </PopupDialog>
  );
}
