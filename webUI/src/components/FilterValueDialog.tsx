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
import { ChipForValue } from "@/features/FilterComponent";
import { StepItemType } from "@/features/Execution";
import { MacroDTO, ActionDTO } from "@/types/DTO";
import ReactMarkdown from "react-markdown";
import SwitchLeftIcon from "@mui/icons-material/SwitchLeft";
import { Stack, Typography, Box, Tooltip, Switch, ButtonGroup } from "@mui/material";
import { MMIconButton } from "./IconButton";
import { useMemo, useState } from "react";
import SortByAlphaIcon from "@mui/icons-material/SortByAlpha";
import ClearIcon from "@mui/icons-material/Clear";
import { PopupDialog } from "./SelectDialog";

type FilterEditorProps = {
  open: boolean;
  action: StepItemType | undefined;
  setAction: (item: StepItemType) => void;
  onClose: () => void;
  onViewItem: (item: MacroDTO | ActionDTO) => void;
};

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
                  flexDirection: "row",
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
                  <Box sx={{ opacity: !isActive ? 0.5 : 1 }}>
                    <ChipForValue mapping={mapping} io={actionState.input} />
                  </Box>
                </Tooltip>
              </Box>
            );
          })}
        </Stack>
      </Stack>
    </PopupDialog>
  );
}
