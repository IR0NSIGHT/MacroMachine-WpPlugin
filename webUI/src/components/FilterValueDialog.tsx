import {
  clearFilter,
  filterAutoName,
  filterValueBlock,
  filterValuePass,
  invertFilter,
  NamedMapping,
  namedMapping,
} from "@/features/Filters";
import { getIconForValue } from "@/features/FilterComponent";
import { StepItemType } from "@/features/Execution";
import { MacroDTO, ActionDTO } from "@/types/DTO";
import SwitchLeftIcon from "@mui/icons-material/SwitchLeft";
import { Typography, ButtonGroup } from "@mui/material";
import { MMIconButton } from "./IconButton";
import { useMemo, useState } from "react";
import ClearIcon from "@mui/icons-material/Clear";
import { SelectDialog } from "./SelectDialog";

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
  const sortOrder = "input" as const;
  const sortedMappings = useMemo(() => {
    if (!actionState) return [];
    return namedMapping(actionState).sort(
      sortOrder === "input" ? sortMappingByInput : sortMappingByOutput,
    );
  }, [actionState, sortOrder]);
  const passMappings = useMemo(
    () => sortedMappings.filter((m) => m.output !== filterValueBlock),
    [sortedMappings],
  );
  const compareFn = useMemo(() => {
    if (!actionState?.input.discrete) {
      return (a: NamedMapping, b: NamedMapping) => a.input - b.input;
    }
    return undefined;
  }, [actionState]);
  if (!actionState) return null;
  return (
    <SelectDialog<NamedMapping>
      open={open}
      items={sortedMappings}
      selectedItems={passMappings}
      compare={compareFn}
      getId={(mapping) => String(mapping.input)}
      getLabel={(mapping) => mapping.inputName}
      isSingleSelect={false}
      title={"Select which values of " + actionState.input.displayName + " to allow "}
      renderIcon={(mapping) =>
        getIconForValue(actionState.input, mapping.input, { width: 40, height: 40 })
      }
      toolbar={
        <>
          <Typography color="text.secondary">{action?.name}</Typography>
          <ButtonGroup>
            <MMIconButton
              disabled={false}
              onClick={() => setActionState(filterAutoName(invertFilter(actionState)))}
              icon={<SwitchLeftIcon />}
              tooltip={"Invert filter"}
            />
            <MMIconButton
              disabled={false}
              onClick={() => setActionState(filterAutoName(clearFilter(actionState)))}
              icon={<ClearIcon />}
              tooltip={"Clear filter"}
            />
          </ButtonGroup>
        </>
      }
      onClose={(selected, confirmed) => {
        if (confirmed && actionState) {
          const updatedFilter = { ...actionState };
          namedMapping(actionState).forEach((mapping) => {
            const idx = mapping.input - actionState.input.min;
            const isPass = selected.some((s) => s.input === mapping.input);
            updatedFilter.mappedOutputs[idx] = isPass ? filterValuePass : filterValueBlock;
          });
          setAction(filterAutoName(updatedFilter));
        }
        onClose();
      }}
    />
  );
}
