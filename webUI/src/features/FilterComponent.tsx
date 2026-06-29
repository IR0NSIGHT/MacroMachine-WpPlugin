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
import * as React from "react";
import { SelectChangeEvent } from "@mui/material/Select";
import { MMIconButton } from "../components/IconButton";
import { StepItemType } from "./Execution";
import CircleIcon from "@mui/icons-material/Circle";
import {
  namedMapping,
  invertFilter,
  isInsideRangeFilter,
  setFilterRange,
  filterAutoName,
  ioNamedValues,
  destructureSimpleFilter,
  NamedMapping,
} from "./Filters";
import SwitchLeftIcon from "@mui/icons-material/SwitchLeft";
import EditIcon from "@mui/icons-material/Edit";
import ClearIcon from "@mui/icons-material/Clear";
import { InputOutputDTO, InputOutputDTOIoParametersInner } from "@/generated/client";
import { fillParentSx } from "@/App";
import { API_BASE } from "@/API/api";

export const colorForValue = (io: InputOutputDTO, value: number): string | undefined => {
  const idx = value - io.min;
  const color = io.colors.length > idx ? io.colors[idx] : 0;
  return color ? `#${(color & 0xffffff).toString(16).padStart(6, "0")}` : undefined;
};

export const getIconForValue = (_io: InputOutputDTO, _value: number) => {
  const valueIcon = _io.iconByValue[_value - _io.min];
  if (valueIcon) {
    return <Avatar src={staticAssetUrl(valueIcon)} sx={{ width: 20, height: 20 }} />;
  }
  return <CircleIcon sx={{ color: colorForValue(_io, _value) }} />;
};

const staticAssetUrl = (assetName: string) => {
  const iconUrl = `${import.meta.env.BASE_URL}icons/${assetName}`;
  return iconUrl;
};

export const ioToIconName = (io: InputOutputDTO) => {
  //FIXME icons are not built into dist
  if (io.type === "BINARY_LAYER" || io.type === "NIBBLE_LAYER" || io.type === "BINARY_SPRAYPAINT") {
    let layerId: InputOutputDTOIoParametersInner = "";
    switch (io.type) {
      case "BINARY_LAYER":
        layerId = io.ioParameters[1];
        break;
      case "NIBBLE_LAYER":
        layerId = io.ioParameters[1];
        break;
      case "BINARY_SPRAYPAINT":
        layerId = io.ioParameters[0];
        break;
    }
    const iconBackendUrl = `${API_BASE}/layers/${layerId}/icon`;
    console.log("try fetching icon from backend: " + iconBackendUrl);
    return iconBackendUrl;
  }
  if (io.iconName) {
    return staticAssetUrl(io.iconName);
  }
  return staticAssetUrl("minecraft_grass_block.png");
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
          <ChipForValue key={m.input} mapping={m} io={item.input} />
        ))}
      </Box>
    </>
  );
};

export const ChipForValue = ({ mapping, io }: { mapping: NamedMapping; io: InputOutputDTO }) => {
  const color = colorForValue(io, mapping.input);
  return (
    <Chip
      label={mapping.inputName}
      variant="outlined"
      icon={getIconForValue(io, mapping.input)}
      sx={{
        "& .MuiChip-icon": {
          color: color,
        },
      }}
    />
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
              <ChipForValue
                mapping={{
                  input: option.value,
                  output: option.value,
                  inputName: option.name,
                  outputName: option.name,
                }}
                io={item.output}
              />
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
    const mapping = mappings.find((m) => m.input === v);
    if (!mapping) return "error for value " + v;
    return <ChipForValue mapping={mapping} io={item.input} />;
  };

  const _insideRangeFilter = isInsideRangeFilter(item);
  return (
    <Box
      sx={{
        display: "flex",
        justifyContent: "left",
        alignItems: "center",
        marginTop: 4,
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
        valueLabelDisplay="on"
        valueLabelFormat={(v) => valueToString(v)}
        disabled={!item.active}
        track={_insideRangeFilter ? "normal" : "inverted"}
        sx={{
          width: "100%",
          maxWidth: "400px",

          "& .MuiSlider-valueLabel": {
            background: "transparent",
          },
        }}
      />
      <InvertFilterButton onClick={() => setItem(invertFilter(item))} />
    </Box>
  );
};
