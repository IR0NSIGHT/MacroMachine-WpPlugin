import { Box, IconButton, Slider, Switch, Tooltip, Typography } from "@mui/material";
import { StepItemType } from "./Execution";
import { isRangeFilter, namedMapping, invertFilter, isInsideRangeFilter } from "./Filters";
import { theme } from "@/theme";
import SwitchLeftIcon from "@mui/icons-material/SwitchLeft";

export const FilterInlineEditor = ({
  item,
  setItem,
}: {
  item: StepItemType;
  setItem: (item: StepItemType) => void;
}) => {
  const isRanged = isRangeFilter(item);

  if (isRanged) {
    return <RangeFilterInlineEditor item={item} setItem={setItem} />;
  }
  return <Typography>{item.name}</Typography>;
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

  const _maxLabelLength = Math.max(0, ...mappings.map((m) => m.inputName.length));

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
      <Switch
        checked={item.active}
        onChange={(e) => {
          setItem({ ...item, active: e.target.checked });
        }}
      />
      <Typography
        sx={{
          whiteSpace: "nowrap",
          flexShrink: 0,
          color: !item.active ? theme.palette.text.disabled : theme.palette.text.primary,
        }}
      >
        Filter by {item.input.displayName} {value[0]} to {value[1]}
      </Typography>
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
      <Box
        sx={{
          width: 300,
          flexShrink: 0,
          alignItems: "center",
          display: "flex",
          mx: 1,
        }}
      >
        <Slider
          value={value}
          onChange={handleChange}
          min={item.input.min}
          max={item.input.max}
          step={1}
          valueLabelDisplay="auto"
          valueLabelFormat={(v) => valueToString(v)}
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
