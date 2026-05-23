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
import {
  _filterValuePass,
  allowedValues,
  filterValueBlock,
  forbiddenValues,
  invertFilter,
  NamedMapping,
  namedMapping,
  StepItemType,
} from "@/features/Filters";
import EditIcon from "@mui/icons-material/Edit";
import { collectRanges } from "@/features/Ranges";

type Props = {
  onSave: (macro: MacroDTO | null) => void;
  onRun: (macro: MacroDTO | null) => void;
};
type StepItemProps = {
  item: StepItemType;
  setItem: (item: StepItemType | null) => void;
};

const explainAlwaysAction = (item: ActionDTO): string => {
  if (item.input.type !== "ALWAYS") return "Complex: " + item.name;

  const all = namedMapping(item);
  const [incrementStr, byStr] = (() => {
    switch (item.actionType as string) {
      case "DIVIDE":
        return ["divide", "by"];
      case "INCREMENT":
        return ["increment", "by"];
      case "LIMIT_TO":
        return ["limit", "to"];
      case "MULTIPLY":
        return "multiplies";
      case "SET":
        return ["set", "to"];
      case "AT_LEAST":
        return ["set ", "to at least"];
      case "DECREMENT":
        return ["decrement", "by"];
      default:
        return [item.actionType, "??"];
    }
  })();
  return [
    incrementStr,
    item.output.displayName,
    byStr,
    all.map((mapping) => mapping.outputName),
  ].join(" ");
};

const isRangeFilter = (filter: ActionDTO): boolean => {
  if (filter.input.discrete) return false;
  const all = getRelevantMappings(filter);
  const ranges = collectRanges(all);
  return ranges.some((range) => range.length > 1); // continuous ranges exist that are interesting
};

const getRelevantMappings = (filter: ActionDTO): NamedMapping[] => {
  const passValues = allowedValues(filter);
  const blockValues = forbiddenValues(filter);
  // only show either PASS or BLOCK values to keep it simple as "Only on .." or "Except on"
  const relevant =
    passValues.length > blockValues.length ? blockValues : passValues;
  return relevant;
};

const explainRangeFilter = (filter: ActionDTO): string => {
  // only show either PASS or BLOCK values to keep it simple as "Only on .." or "Except on"
  const all = getRelevantMappings(filter);
  const ranges = collectRanges(all);

  return ranges
    .map((range) => {
      let onlyOn = "";
      switch (range.start.output) {
        case filterValueBlock:
          onlyOn = "Except on ";
          break;
        case _filterValuePass: //FIXME this is misleading. its not "only on", its an OR operation
          onlyOn = "Only on ";
          break;
        case filter.output.ignoreValue:
          onlyOn = "Only on ";
          break;
      }
      return (
        onlyOn +
        filter.input.displayName +
        ": " +
        range.start.inputName +
        " to " +
        range.end.inputName
      );
    })
    .join(", ");
};

const StepItem = ({ item, setItem }: StepItemProps) => {
  const isFilter =
    item.input.type !== "ALWAYS" &&
    item.output.type === "INTERMEDIATE_SELECTION";
  const rangeFilter = isFilter && isRangeFilter(item);
  const passValues = allowedValues(item);
  const blockValues = forbiddenValues(item);
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
        <Typography color={item.active ? "text.primary" : "text.disabled"}>
          {!isFilter && explainAlwaysAction(item)}
          {isFilter &&
            !rangeFilter &&
            blockValues.length >= passValues.length &&
            "Only on " +
              item.input.displayName +
              ": " +
              passValues.map((mapping) => mapping.inputName).join(", ")}
          {isFilter &&
            !rangeFilter &&
            blockValues.length < passValues.length &&
            "Except on " +
              item.input.displayName +
              ": " +
              blockValues.map((mapping) => mapping.inputName).join(", ")}
          {isFilter && rangeFilter && explainRangeFilter(item)}
        </Typography>
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
  .filter((item) => item.name.startsWith("Filter: "));

const defaultApplyActions: StepItemType[] = (applyActions as ActionDTO[]).map(
  (item) => ({
    ...item,
    active: true,
  }),
);

const sortInactiveLast = (a: StepItemType, b: StepItemType): number => {
  if (a.active === b.active) {
    return sortAlphabetical(a, b);
  }
  return Number(b.active) - Number(a.active);
};

const sortAlphabetical = (a: StepItemType, b: StepItemType): number => {
  return a.name.localeCompare(b.name);
};

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
  const updateApplyItem = (action: StepItemType | null, idx: number) => {
    if (action === null) {
      setAppliers((prev) => prev.filter((p, i) => i !== idx));
    } else {
      const mapper = (item: StepItemType, itemIdx: number) => {
        if (idx === itemIdx) return action;
        else return item;
      };
      setAppliers((prev) => prev.map(mapper));
    }
  };

  const filterDTOtoComponent = (action: StepItemType, idx: number) => {
    return (
      <StepItem item={action} setItem={(item) => updateFilterItem(item, idx)} />
    );
  };
  const applyDTOtoComponent = (action: StepItemType, idx: number) => {
    return (
      <StepItem item={action} setItem={(item) => updateApplyItem(item, idx)} />
    );
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
          <IconButton
            size="small"
            disabled={false}
            onClick={() => props.onRun(null)}
          >
            <PlayArrowIcon />
          </IconButton>
          <IconButton
            size="small"
            disabled={false}
            onClick={() => props.onSave(null)}
          >
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
