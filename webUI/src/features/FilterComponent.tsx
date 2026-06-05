import { Box, ButtonGroup, IconButton, Slider, Switch, Tooltip, Typography } from "@mui/material";
import { StepItemType } from "./Execution";
import {
  isRangeFilter,
  namedMapping,
  invertFilter,
  isInsideRangeFilter,
  isFilter,
} from "./Filters";
import { theme } from "@/theme";
import SwitchLeftIcon from "@mui/icons-material/SwitchLeft";
import EditIcon from "@mui/icons-material/Edit";
import ClearIcon from "@mui/icons-material/Clear";

export const SimpleFilterInlineEditor = ({
  item,
  setItem,
  deleteItem,
  openEditorFor,
}: {
  item: StepItemType;
  setItem: (item: StepItemType) => void;
  deleteItem: () => void;
  openEditorFor: (item: StepItemType) => void;
}) => {
  return (
    <Box
      key={item.uid}
      sx={{
        display: "flex",
        flexDirection: "row",
        alignItems: "center",
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

      <Typography color={item.active ? "text.primary" : "text.disabled"}>{item.name}</Typography>
      <ButtonGroup>
        {isFilter(item) && (
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
          onClick={() => openEditorFor(item)}
          className="clear-btn"
        >
          <EditIcon />
        </IconButton>
        <IconButton size="small" disabled={false} onClick={deleteItem} className="clear-btn">
          <ClearIcon />
        </IconButton>
      </ButtonGroup>
    </Box>
  );
};

export const FilterInlineEditor = ({
  item,
  setItem,
  deleteItem,
  openEditorFor,
}: {
  item: StepItemType;
  setItem: (item: StepItemType) => void;
  deleteItem: () => void;
  openEditorFor: (item: StepItemType) => void;
}) => {
  const isRanged = isRangeFilter(item);

  if (isRanged) {
    return <RangeFilterInlineEditor item={item} setItem={setItem} />;
  }
  return (
    <SimpleFilterInlineEditor
      item={item}
      setItem={setItem}
      deleteItem={deleteItem}
      openEditorFor={openEditorFor}
    />
  );
};

export const RangeFilterInlineEditor = ({
  item,
  setItem,
}: {
  item: StepItemType;
  setItem: (item: StepItemType) => void;
}) => {
  const mappings = namedMapping(item);
  const mappingPoints = item.mappingPointsX;
  const value = [mappingPoints[0], mappingPoints[1]];

  const handleChange = (_event: Event, newValue: number[]) => {
    setItem({ ...item, mappingPointsX: newValue });
  };

  const valueToString = (v: number) => {
    return mappings.find((m) => m.input === v)?.inputName ?? String(v);
  };

  const insideRangeFilter = isInsideRangeFilter(item);

  const trackColor = !item.active
    ? theme.palette.text.disabled
    : insideRangeFilter
      ? theme.palette.primary.main
      : theme.palette.text.disabled;
  const railColor = !item.active
    ? theme.palette.text.disabled
    : insideRangeFilter
      ? theme.palette.text.disabled
      : theme.palette.primary.main;
  return (
    <Box
      sx={{
        display: "flex",
        flexDirection: "row",
        alignItems: "center",
      }}
    >
      <Box sx={{ display: "flex", alignItems: "center", width: "50%" }}>
        <Switch
          checked={item.active}
          onChange={(e) => {
            setItem({ ...item, active: e.target.checked });
          }}
        />
        <Typography
          sx={{ color: !item.active ? theme.palette.text.disabled : theme.palette.text.secondary }}
        >
          Filter by
          <span
            style={{
              color: !item.active ? theme.palette.text.disabled : theme.palette.text.primary,
            }}
          >
            {" "}
            {item.input.displayName}{" "}
          </span>
          {insideRangeFilter ? "inside range" : "outside range"} {valueToString(value[0])} to{" "}
          {valueToString(value[1])}
        </Typography>
      </Box>
      <Box
        sx={{
          width: 300,
          flexShrink: 0,
          alignItems: "center",
          display: "flex",
          mx: 1,
          gap: 1,
        }}
      >
        <Tooltip title={"Invert filter"}>
          <IconButton
            color="primary"
            size="small"
            disabled={!item.active}
            onClick={() => setItem(invertFilter(item))}
          >
            <SwitchLeftIcon />
          </IconButton>
        </Tooltip>
        <Slider
          value={value}
          onChange={handleChange}
          min={item.input.min}
          max={item.input.max}
          step={1}
          valueLabelDisplay="auto"
          valueLabelFormat={(v) => valueToString(v)}
          disabled={!item.active}
          sx={(theme) => ({
            alignSelf: "center",
            "& .MuiSlider-rail": {
              backgroundColor: railColor,
              opacity: 1,
            },

            "& .MuiSlider-track": {
              backgroundColor: trackColor,
              borderColor: trackColor,
              opacity: 1,
            },

            "& .MuiSlider-thumb": {
              backgroundColor: !item.active
                ? theme.palette.text.disabled
                : theme.palette.primary.main,
              opacity: 1,
            },
          })}
        />
      </Box>
    </Box>
  );
};
