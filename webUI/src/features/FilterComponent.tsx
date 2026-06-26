import {
  Avatar,
  Box,
  ButtonGroup,
  Chip,
  FormControl,
  InputLabel,
  ListItem,
  ListItemAvatar,
  ListItemButton,
  ListItemText,
  MenuItem,
  Select,
  Slider,
  Switch,
  Tooltip,
} from "@mui/material";
import { SelectChangeEvent } from "@mui/material/Select";
import { MMIconButton } from "../components/IconButton";
import { StepItemType } from "./Execution";
import FaceIcon from "@mui/icons-material/Face";
import {
  namedMapping,
  invertFilter,
  isInsideRangeFilter,
  setFilterRange,
  filterAutoName,
  ioNamedValues,
  destructureSimpleFilter,
} from "./Filters";
import { theme } from "@/theme";
import SwitchLeftIcon from "@mui/icons-material/SwitchLeft";
import EditIcon from "@mui/icons-material/Edit";
import ClearIcon from "@mui/icons-material/Clear";
import { InputOutputDTO, InputOutputDTOIoParametersInner } from "@/generated/client";
import { fillParentSx } from "@/App";

export const getIconForValue = (_io: InputOutputDTO, _value: number) => {
  return <FaceIcon />;
};

export const ioToIconName = (io: InputOutputDTO) => {
  //FIXME icons are not built into dist
  if (io.type === "NIBBLE_LAYER" && io.ioParameters.length >= 2 && io.ioParameters[1] === "") {
    const layerId: InputOutputDTOIoParametersInner = io.ioParameters[1];
    const API_BASE = import.meta.env.VITE_API_BASE_URL;
    return `${API_BASE}/api/layers/${layerId}/icon`;
  }
  const iconUrl = `${import.meta.env.BASE_URL}icons/minecraft_grass_block.png`;
  return iconUrl;
};

// eslint-disable-next-line no-undef
const InvertFilterButton = (props: {
  onClick: React.MouseEventHandler<HTMLButtonElement> | undefined;
}) => {
  return (
    <MMIconButton
      disabled={false}
      onClick={props.onClick}
      icon={<SwitchLeftIcon />}
      tooltip={"Invert filter"}
    />
  );
};

export const SimpleFilterInlineEditor = ({
  item,
  setItem,
  openEditorFor,
}: {
  item: StepItemType;
  setItem: (item: StepItemType) => void;
  openEditorFor: (item: StepItemType) => void;
}) => {
  const filterData = destructureSimpleFilter(item);
  return (
    <>
      <Box
        sx={{
          ...fillParentSx,
          flexDirection: "row",
          display: "flex",
          alignItems: "center",
          gap: 1,
          flexWrap: "wrap",
        }}
      >
        <ButtonGroup>
          <InvertFilterButton onClick={() => setItem(invertFilter(item))} />
          <MMIconButton
            disabled={false}
            onClick={() => openEditorFor(item)}
            icon={<EditIcon />}
            tooltip={""}
          />
        </ButtonGroup>
        <Chip
          key={"isOnlyOn"}
          label={filterData.isOnlyOn ? "Only on" : "Except on"}
          color="info"
          variant="outlined"
        />
        {filterData.relevantMappings.map((m) => (
          <Chip
            key={m.input}
            label={m.inputName}
            variant="outlined"
            icon={getIconForValue(item.input, m.input)}
          />
        ))}
      </Box>
    </>
  );
};

export const StepInlineEditor = ({
  item,
  setItem,
  deleteItem,
  relevantIo,
  secondaryText,
  primaryText,
  editor,
}: {
  item: StepItemType;
  setItem: (item: StepItemType) => void;
  deleteItem: () => void;
  relevantIo: InputOutputDTO;
  primaryText: string;
  secondaryText: string;
  // eslint-disable-next-line no-undef
  editor: JSX.Element;
}) => {
  return (
    <ListItem
      disablePadding
      color={item.active ? "default" : "text.disabled"}
      sx={{ alignItems: "flex-start" }}
    >
      <ListItemButton
        disableRipple
        disableTouchRipple
        sx={{
          ...fillParentSx,
          cursor: "default",
          borderBottom: 2,
          py: 0,
          borderColor: "divider",
          alignItems: "stretch",

          filter: item.active ? "none" : "grayscale(100%)",
          opacity: item.active ? 1 : 0.4,
        }}
      >
        <Box
          sx={{
            ...fillParentSx,
            flexDirection: "column",
            display: "flex",
            p: 1,
          }}
        >
          <Box
            sx={{
              ...fillParentSx,
              flexDirection: "row",
              display: "flex",
              gap: 1,
              alignItems: "center",
              flexWrap: "wrap",
            }}
          >
            <Box
              sx={{
                ...fillParentSx,
                flexDirection: "row",
                display: "flex",
                gap: 1,
                alignItems: "center",
                minWidth: 250,
              }}
            >
              <ListItemAvatar
                sx={{
                  display: "flex",
                  alignItems: "center",
                  justifyContent: "center",
                }}
              >
                <Avatar src={ioToIconName(relevantIo)} />
              </ListItemAvatar>

              <ListItemText
                sx={{ maxWidth: 400 }}
                primary={primaryText}
                secondary={secondaryText}
                primaryTypographyProps={{
                  color: item.active ? "text.primary" : "text.disabled",
                }}
                secondaryTypographyProps={{
                  color: item.active ? "text.secondary" : "text.disabled",
                }}
              />
            </Box>

            <Box>
              <Tooltip title={"Enable item"}>
                <Switch
                  checked={item.active}
                  onChange={(e) =>
                    setItem({
                      ...item,
                      active: e.target.checked,
                    })
                  }
                />
              </Tooltip>

              <MMIconButton
                disabled={false}
                onClick={deleteItem}
                icon={<ClearIcon />}
                tooltip="Delete this item"
              />
            </Box>
          </Box>
          <Box sx={{ pointerEvents: item.active ? "auto" : "none" }}>{editor}</Box>
        </Box>
      </ListItemButton>
    </ListItem>
  );
};

export const ApplyActionInlineEditor = ({
  item,
  setItem,
}: {
  item: StepItemType;
  setItem: (item: StepItemType) => void;
}) => {
  const outputOptions = ioNamedValues(item.output);
  const handleChange = (event: SelectChangeEvent<string>) => {
    const value = Number(event.target.value);

    setItem({
      ...item,
      mappingPointsY: [value],
    });
  };
  return (
    <Box sx={{ minWidth: 120, maxWidth: 400, m: 1 }}>
      {" "}
      <FormControl fullWidth>
        <InputLabel id="select-action-output-">Set to</InputLabel>
        <Select
          labelId="demo-simple-select-label"
          id="demo-simple-select"
          value={item.mappingPointsY}
          label="Age"
          onChange={handleChange as any} //FIXME this is an ugy hack
        >
          {outputOptions.map((option) => (
            <MenuItem key={option.value} value={option.value}>
              {option.name}
            </MenuItem>
          ))}
        </Select>
      </FormControl>
    </Box>
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
    const newFilter = setFilterRange(item, newValue[0], newValue[1], true);
    setItem(filterAutoName(newFilter));
    console.log("handle change:", newValue, newFilter.name);
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
        justifyContent: "left",
        alignItems: "center",
        py: 2,
        px: 4,
        borderRadius: 2,
        gap: 1,
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
        disabled={!item.active}
        sx={(theme) => ({
          width: "100%",
          maxWidth: "400px",
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
      <InvertFilterButton onClick={() => setItem(invertFilter(item))} />
    </Box>
  );
};
