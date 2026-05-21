import {
  Box,
  ButtonGroup,
  Divider,
  FormControlLabel,
  IconButton,
  Paper,
  Switch,
  Typography,
} from "@mui/material";
import PlayArrowIcon from "@mui/icons-material/PlayArrow";
import SaveIcon from "@mui/icons-material/Save";
import { ActionDTO, MacroDTO } from "@/types/DTO";
import { useState } from "react";
import filters from "../assets/defaultFilters.json";
import applyActions from "../assets/defaultApplyActions.json";
import ClearIcon from "@mui/icons-material/Clear";
type Props = {
  onSave: (macro: MacroDTO | null) => void;
  onRun: (macro: MacroDTO | null) => void;
};
type StepItemProps = {
  item: StepItemType;
  setItem: (item: StepItemType | null) => void;
};

const StepItem = ({ item, setItem }: StepItemProps) => {
  return (
    <Box
      sx={{
        "& .clear-btn": {
          color: "transparent",
        },

        "&:hover .clear-btn": {
          color: "inherit",
        },
      }}
    >
      <FormControlLabel
        control={
          <Switch
            checked={item.active}
            onChange={(e) => {
              setItem({ ...item, active: e.target.checked });
            }}
          />
        }
        label={
          <Typography color={item.active ? "text.primary" : "text.disabled"}>
            {item.name}
          </Typography>
        }
      />
      <IconButton size="small" disabled={false} onClick={() => setItem(null)} className="clear-btn">
        <ClearIcon />
      </IconButton>
    </Box>
  );
};

type StepItemType = ActionDTO & { active: boolean };

const defaultFilters: StepItemType[] = (filters as ActionDTO[]).map((item) => ({
  ...item,
  active: true,
}));

const defaultApplyActions: StepItemType[] = (applyActions as ActionDTO[]).map((item) => ({
  ...item,
  active: true,
}));

export const GlobalOperationDesigner = (props: Props) => {
  const [filters, setFilters] = useState<StepItemType[]>(defaultFilters);
  const [appliers, setAppliers] = useState<StepItemType[]>(defaultApplyActions);

  const updateFilterItem = (action: StepItemType | null, idx: number) => {
    if (action === null) {
      setFilters((prev) => prev.filter((p, i) => i !== idx));
    } else {
      const mapper = (item: StepItemType, itemIdx: number): StepItemType => {
        if (idx === itemIdx) return action;
        else return item;
      };
      setFilters((prev) => prev.map(mapper));
    }
  };
  const updateApplyItem = (action: StepItemType, idx: number) => {
    const mapper = (item: StepItemType, itemIdx: number) => {
      if (idx === itemIdx) return action;
      else return item;
    };
    setAppliers((prev) => prev.map(mapper));
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
          {filters.map(filterDTOtoComponent)}
        </Paper>
        <Divider orientation="vertical" flexItem />
        <Paper sx={{ p: 1, border: 1 }}>
          <Typography>Then:</Typography>
          {appliers.map(applyDTOtoComponent)}
        </Paper>
      </Box>
    </Box>
  );
};
