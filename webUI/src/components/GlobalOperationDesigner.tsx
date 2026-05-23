import {
  Box,
  ButtonGroup,
  Divider,
  IconButton,
  Paper,
  Switch,
  Tooltip,
  Typography,
} from "@mui/material";
import PlayArrowIcon from "@mui/icons-material/PlayArrow";
import SaveIcon from "@mui/icons-material/Save";
import { ActionDTO, MacroDTO } from "@/types/DTO";
import { useState } from "react";
import filters from "../assets/defaultFilters.json";
import applyActions from "../assets/defaultApplyActions.json";
import ClearIcon from "@mui/icons-material/Clear";
import SwitchLeftIcon from "@mui/icons-material/SwitchLeft";
import { _filterValuePass, filterAutoName, invertFilter, StepItemType } from "@/features/Filters";
import EditIcon from "@mui/icons-material/Edit";
import { actionAutoName } from "@/features/Action";

type Props = {
  onSave: (macro: MacroDTO | null) => void;
  onRun: (macro: MacroDTO | null) => void;
};
type StepItemProps = {
  item: StepItemType;
  setItem: (item: StepItemType | null) => void;
};

const StepItem = ({ item, setItem }: StepItemProps) => {
  const isFilter = item.input.type !== "ALWAYS" && item.output.type === "INTERMEDIATE_SELECTION";
  return (
    <Box
      key={item.uid}
      sx={{
        display: "flex",
        flexDirection: "row",
        "& .clear-btn": {
          color: "transparent",
        },

        "&:hover .clear-btn": {
          color: "inherit",
        },
      }}
    >
      <Switch
        checked={item.active}
        onChange={(e) => {
          setItem({ ...item, active: e.target.checked });
        }}
      />

      <Tooltip
        title={item.name + "   " + item.uid}
        onClick={() => navigator.clipboard.writeText(item.uid)}
      >
        <Typography color={item.active ? "text.primary" : "text.disabled"}>{item.name}</Typography>
      </Tooltip>

      <ButtonGroup>
        {isFilter && (
          <IconButton
            size="small"
            disabled={false}
            onClick={() => setItem(invertFilter(item))}
            className="clear-btn"
          >
            <SwitchLeftIcon />
          </IconButton>
        )}
        <IconButton
          size="small"
          disabled={false}
          onClick={() => setItem(invertFilter(item))}
          className="clear-btn"
        >
          <EditIcon />
        </IconButton>
        <IconButton
          size="small"
          disabled={false}
          onClick={() => setItem(null)}
          className="clear-btn"
        >
          <ClearIcon />
        </IconButton>
      </ButtonGroup>
    </Box>
  );
};

const defaultFilters: StepItemType[] = (filters as ActionDTO[])
  .map((item) => ({
    ...item,
    active: true,
  }))
  .map(filterAutoName);

const defaultApplyActions: StepItemType[] = (applyActions as ActionDTO[])
  .map((item) => ({
    ...item,
    active: true,
  }))
  .map(actionAutoName);

const sortInactiveLast = (a: StepItemType, b: StepItemType): number => {
  if (a.active === b.active) {
    return sortAlphabetical(a, b);
  }
  return Number(b.active) - Number(a.active);
};

const sortAlphabetical = (a: StepItemType, b: StepItemType): number => {
  const isFilter = a.input.type !== "ALWAYS" && a.output.type === "INTERMEDIATE_SELECTION";

  if (isFilter) return a.input.displayName.localeCompare(b.input.displayName);
  else return a.output.displayName.localeCompare(b.output.displayName);
};

export const GlobalOperationDesigner = (props: Props) => {
  const [filters, setFilters] = useState<StepItemType[]>(defaultFilters);
  const [appliers, setAppliers] = useState<StepItemType[]>(defaultApplyActions);

  const updateFilterItem = (action: StepItemType | null, idx: number) => {
    if (action === null) {
      setFilters((prev) => prev.filter((p, i) => i !== idx));
    } else {
      const mapper = (item: StepItemType, itemIdx: number): StepItemType => {
        if (idx === itemIdx) return filterAutoName(action);
        else return item;
      };
      setFilters((prev) => prev.map(mapper));
    }
  };
  const updateApplyItem = (action: StepItemType | null, idx: number) => {
    if (action === null) {
      setAppliers((prev) => prev.filter((p, i) => i !== idx));
    } else {
      const mapper = (item: StepItemType, itemIdx: number) => {
        if (idx === itemIdx) return actionAutoName(action);
        else return item;
      };
      setAppliers((prev) => prev.map(mapper));
    }
  };

  const filterDTOtoComponent = (action: StepItemType, idx: number) => {
    return <StepItem item={action} setItem={(item) => updateFilterItem(item, idx)} />;
  };
  const applyDTOtoComponent = (action: StepItemType, idx: number) => {
    return <StepItem item={action} setItem={(item) => updateApplyItem(item, idx)} />;
  };
  return (
    <Box
      sx={{
        p: 1,
        display: "flex",
        flexDirection: "column",
        height: "95vh", // FIXME ugly hack to make the fucking flexbox work
      }}
    >
      <Box
        sx={{
          display: "flex",
          flexDirection: "row",
        }}
      >
        <ButtonGroup variant="contained" aria-label="Basic button group">
          <IconButton size="small" disabled={false} onClick={() => props.onRun(null)}>
            <PlayArrowIcon />
          </IconButton>
          <IconButton size="small" disabled={false} onClick={() => props.onSave(null)}>
            <SaveIcon />
          </IconButton>
        </ButtonGroup>
        <Typography>Name: My Global Operation</Typography>
      </Box>

      <Box
        sx={{
          flex: 1,
          minHeight: 0,
          overflowY: "auto",
          p: 2,
        }}
      >
        <Paper sx={{ p: 1, border: 1 }}>
          <Typography>If:</Typography>
          {filters.sort(sortInactiveLast).map(filterDTOtoComponent)}
        </Paper>
        <Divider orientation="vertical" flexItem />
        <Paper sx={{ p: 1, border: 1 }}>
          <Typography>Then:</Typography>
          {appliers.sort(sortInactiveLast).map(applyDTOtoComponent)}
        </Paper>
      </Box>
    </Box>
  );
};
