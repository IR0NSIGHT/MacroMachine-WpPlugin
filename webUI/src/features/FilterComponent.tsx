import {
  Avatar,
  Box,
  ButtonGroup,
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
} from "@mui/material";
import { SelectChangeEvent } from '@mui/material/Select';
import { MMIconButton } from "../components/IconButton";
import { StepItemType } from "./Execution";
import {
  namedMapping,
  invertFilter,
  isInsideRangeFilter,
  setFilterRange,
  filterAutoName,
  ioNamedValues,
} from "./Filters";
import { theme } from "@/theme";
import SwitchLeftIcon from "@mui/icons-material/SwitchLeft";
import EditIcon from "@mui/icons-material/Edit";
import ClearIcon from "@mui/icons-material/Clear";
import {
  InputOutputDTO,
  InputOutputDTOIoParametersInner,
} from "@/generated/client";
import { fillParentSx } from "@/App";
import { useState } from "react";
import { ActionDTO } from "@/types/DTO";

export const ioToIconName = (io: InputOutputDTO) => {
  //FIXME icons are not built into dist
  if (
    io.type === "NIBBLE_LAYER" &&
    io.ioParameters.length >= 2 &&
    io.ioParameters[1] === ""
  ) {
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
  return (
    <ButtonGroup>
      <InvertFilterButton onClick={() => setItem(invertFilter(item))} />
      <MMIconButton
        disabled={false}
        onClick={() => openEditorFor(item)}
        icon={<EditIcon />}
        tooltip={""}
      />
    </ButtonGroup>
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
  const [isSelected, setSelected] = useState(false);

  return (
    <ListItem
      disablePadding
      color={item.active ? "default" : "text.disabled"}
      sx={{ alignItems: "flex-start" }}
      key={item.uid}
    >
      <ListItemButton
        disableRipple
        disableTouchRipple
        sx={{
          ...fillParentSx,
          display: "flex",
          flexDirection: "column",
          alignItems: "stretch",
          cursor: "default",
          borderBottom: 2,
          py: 1,
          borderColor: "divider",
        }}
        onClick={(e) => {
          const target = e.target as HTMLElement;
          console.log("target clicked:", target);
          if (target.closest("button,input,label,span")) {
            return;
          }

          setSelected((s) => !s);
        }}
      >
        <Box sx={fillParentSx}>
          <Box sx={{ display: "flex", gap: 1 }}>
            <ListItemAvatar
              sx={{
                display: "flex",
                alignItems: "center",
                justifyContent: "center",
              }}
            >
              <Avatar
                src={ioToIconName(relevantIo)}
                sx={{
                  filter: item.active ? "none" : "grayscale(100%)",
                  opacity: item.active ? 1 : 0.4,
                }}
              />
            </ListItemAvatar>
            <ListItemText
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

          {isSelected && (
            <Box
              sx={{
                display: "flex",
                gap: 1,
              }}
            >
              <Switch
                checked={item.active}
                onChange={(e) => {
                  setItem({ ...item, active: e.target.checked });
                }}
              />
              <ButtonGroup>
                <MMIconButton
                  disabled={false}
                  onClick={deleteItem}
                  icon={<ClearIcon />}
                  tooltip={"Delete this item"}
                />
              </ButtonGroup>
            </Box>
          )}
          {item.active && editor}
        </Box>
      </ListItemButton>
    </ListItem>
  );
};

export const ApplyActionInlineEditor = ({
  item,
  setItem
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
    <Box sx={{ minWidth: 120, maxWidth: 180 }}>      <FormControl fullWidth>
        <InputLabel id="select-action-output-">Set to</InputLabel>
        <Select
          labelId="demo-simple-select-label"
          id="demo-simple-select"
          value={item.mappingPointsY}
          label="Age"
          onChange={handleChange as any}   //FIXME this is an ugy hack
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
