import {
  filterAutoName,
  filterValueBlock,
  filterValuePass,
  NamedMapping,
  namedMapping,
} from "@/features/Filters";
import { getIconForValue } from "@/features/FilterComponent";
import { StepItemType } from "@/features/Execution";
import { MacroDTO, ActionDTO } from "@/types/DTO";
import SwitchLeftIcon from "@mui/icons-material/SwitchLeft";
import { Typography, ButtonGroup } from "@mui/material";
import { MMIconButton } from "./IconButton";
import { useEffect, useMemo, useState } from "react";
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

export function FilterValueDialog({ open, action, onClose, setAction }: FilterEditorProps) {
  const sortedMappings = useMemo(() => {
    if (!action) return [];
    return namedMapping(action).sort(sortMappingByInput);
  }, [action]);

  const passMappings = useMemo(
    () => sortedMappings.filter((m) => m.output !== filterValueBlock),
    [sortedMappings],
  );

  const [dialogSelected, setDialogSelected] = useState<NamedMapping[]>(passMappings);

  useEffect(() => {
    if (open && action) {
      setDialogSelected(passMappings);
    }
  }, [open, action, passMappings]);

  const compareFn = useMemo(() => {
    if (!action?.input.discrete) {
      return (a: NamedMapping, b: NamedMapping) => a.input - b.input;
    }
    return undefined;
  }, [action]);

  if (!action) return null;

  const selectedInputs = new Set(dialogSelected.map((m) => m.input));

  const handleInvert = () => {
    const inverted = sortedMappings.filter((m) => !selectedInputs.has(m.input));
    setDialogSelected(inverted);
  };

  const handleClear = () => {
    setDialogSelected([]);
  };

  return (
    <SelectDialog<NamedMapping>
      open={open}
      items={sortedMappings}
      selectedItems={dialogSelected}
      onSelectionChange={setDialogSelected}
      compare={compareFn}
      getId={(mapping) => String(mapping.input)}
      getLabel={(mapping) => mapping.inputName}
      isSingleSelect={false}
      title={"Select which '" + action.input.displayName + "' values the filter allows "}
      renderIcon={(mapping) =>
        getIconForValue(action.input, mapping.input, { width: 40, height: 40 })
      }
      toolbar={
        <>
          <Typography color="text.secondary">{action.name}</Typography>
          <ButtonGroup>
            <MMIconButton
              disabled={false}
              onClick={handleInvert}
              icon={<SwitchLeftIcon />}
              tooltip={"Invert filter"}
            />
            <MMIconButton
              disabled={false}
              onClick={handleClear}
              icon={<ClearIcon />}
              tooltip={"Clear filter"}
            />
          </ButtonGroup>
        </>
      }
      onClose={(selected, confirmed) => {
        if (confirmed && action) {
          const updatedFilter = { ...action };
          namedMapping(action).forEach((mapping) => {
            const idx = mapping.input - action.input.min;
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
