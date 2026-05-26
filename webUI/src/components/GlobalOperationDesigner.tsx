import {
  Box,
  ButtonGroup,
  Divider,
  IconButton,
  Paper,
  Switch,
  TextField,
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
import { filterAutoName, invertFilter, isFilter } from "@/features/Filters";
import { isStepItem, isStepMacro, StepItemType } from "@/features/Execution";
import EditIcon from "@mui/icons-material/Edit";
import { actionAutoName, isSimpleAction } from "@/features/Action";
import { FilterValueDialog } from "./MacroList/ActionDetailsDialog";
import RestartAltIcon from "@mui/icons-material/RestartAlt";
import BugReportIcon from "@mui/icons-material/BugReport";
import { MacroExecuteRequester, runnableMacro, toMacroDTO, toRunnable } from "@/features/Execution";
import equal from "fast-deep-equal";
import AddIcon from "@mui/icons-material/Add";
import { SelectDialog } from "./SelectDialog";

type Props = {
  onSave: (macro: MacroDTO, actions: ActionDTO[]) => void;
  onExecute: MacroExecuteRequester;
  macros: MacroDTO[];
  actions: ActionDTO[];
};

type StepItemProps = {
  item: StepItemType;
  setItem: (item: StepItemType) => void;
  deleteItem: () => void;
  openEditorFor: (item: StepItemType) => void;
};

const StepItem = ({ item, setItem, deleteItem, openEditorFor }: StepItemProps) => {
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
  const [editorItem, setEditorItem] = useState<{
    item: StepItemType;
    type: "filter" | "action";
  } | null>(null);

  const [title, setTitle] = useState<string | undefined>(undefined);
  const [description, setDescription] = useState<string | undefined>(undefined);
  const [uuid, setUUID] = useState<string>(crypto.randomUUID());

  const [addItem, setAddItem] = useState<"filter" | "applier" | undefined>(undefined);
  const [loadMacros, setLoadMacros] = useState<runnableMacro[] | undefined>();

  const currentRunnable = constructRunnable();
  const backendRunnable = toRunnable(
    props.macros.find((macro) => macro.uid === uuid),
    props.actions,
    props.macros,
  );
  const diff = !equal(currentRunnable, backendRunnable);

  const updateFilterItem = (action: StepItemType, isDelete: boolean) => {
    if (isDelete) {
      setFilters((prev) => prev.filter((p) => p.uid !== action?.uid));
    } else {
      const mapper = (item: StepItemType): StepItemType => {
        if (item.uid === action.uid) return filterAutoName(action);
        else return item;
      };
      setFilters((prev) => prev.map(mapper));
    }
  };
  const updateApplyItem = (action: StepItemType, isDelete: boolean) => {
    if (isDelete) {
      setAppliers((prev) => prev.filter((p) => p.uid !== action?.uid));
    } else {
      const mapper = (item: StepItemType): StepItemType => {
        if (item.uid === action.uid) return actionAutoName(action);
        else return item;
      };
      setAppliers((prev) => prev.map(mapper));
    }
  };

  function constructActions(): StepItemType[] {
    const steps = [...filters, ...appliers].map((action) => ({
      ...action,
      uid: crypto.randomUUID(),
    })); //reshuffle UUIDs so no default action is present in two macros
    return steps;
  }

  function constructRunnable(): runnableMacro {
    const actions = constructActions();
    const runnable: runnableMacro = {
      steps: actions,
      name: title ?? "My new global operation macro " + uuid,
      description: description ?? "",
      uid: uuid,
    };
    return runnable;
  }

  function onExecute() {
    props.onExecute(constructRunnable(), false);
  }

  function onSave() {
    const runnable = constructRunnable();
    props.onSave(toMacroDTO(runnable), constructActions());
  }

  function onStartNew() {
    setUUID(crypto.randomUUID());
    setFilters(defaultFilters);
    setAppliers(defaultApplyActions);
    setTitle(undefined);
    setDescription(undefined);
  }

  function onRequestLoadExisting() {
    const allowedMacros = props.macros
      .map((m) => toRunnable(m, props.actions, props.macros))
      .filter((m) => m !== undefined)
      .filter(isGlobalOpsMacro);

    setLoadMacros(allowedMacros);
  }

  function isGlobalOpsMacro(runnable: runnableMacro): true | { error: string } {
    const steps = runnable.steps;
    if (steps.some((step) => isStepMacro(step)))
      return {
        error:
          "this macro contains nested macros. It does not work with this Global Operation Designer.",
      };

    if (steps.some((step) => isStepMacro(step) || (!isFilter(step) && !isSimpleAction(step)))) {
      return {
        error:
          "this macro contains steps that are neither filters nor simple actions. It does not work with this Global Operation Designer.",
      };
    }

    const filters = steps.filter(isStepItem).filter(isFilter);
    const firstXItems = steps.slice(0, filters.length);
    if (!equal(filters, firstXItems)) {
      return {
        error:
          "this macro does not have all filters at the beginning. It does not work with this Global Operation Designer.",
      };
    }
    return true;
  }

  function onLoadExisting(runnable: runnableMacro) {
    const steps = runnable.steps;

    const filters = steps.filter(isStepItem).filter(isFilter);
    const appliers = steps.filter(isStepItem).filter(isSimpleAction);

    // --- allow this macro ---
    setUUID(runnable.uid);
    setFilters(filters);
    setAppliers(appliers);
    setTitle(runnable.name);
    setDescription(runnable.description);
  }

  function onDebug() {
    props.onExecute(constructRunnable(), true);
  }

  function onAddApplier() {
    setAddItem("applier");
  }
  function onAddFilter() {
    setAddItem("filter");
  }

  const filterDTOtoComponent = (action: StepItemType) => {
    return (
      <StepItem
        item={action}
        setItem={(item) => updateFilterItem(item, false)}
        deleteItem={() => updateApplyItem(action, true)}
        openEditorFor={(filter) => setEditorItem({ item: filter, type: "filter" })}
      />
    );
  };
  const applyDTOtoComponent = (action: StepItemType) => {
    return (
      <StepItem
        item={action}
        setItem={(item) => updateApplyItem(item, false)}
        deleteItem={() => updateApplyItem(action, true)}
        openEditorFor={(applyItem) => setEditorItem({ item: applyItem, type: "action" })}
      />
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
          <Tooltip title="Execute this macro on your map.">
            <IconButton size="small" disabled={false} onClick={onExecute}>
              <PlayArrowIcon />
            </IconButton>
          </Tooltip>
          <Tooltip title="Debug-Execute this macro on your map, step-by-step.">
            <IconButton size="small" disabled={false} onClick={onDebug}>
              <BugReportIcon />
            </IconButton>
          </Tooltip>
          <Tooltip title="Save this macro.">
            <IconButton size="small" disabled={!diff} onClick={onSave}>
              <SaveIcon />
            </IconButton>
          </Tooltip>
          <Tooltip title="Start a new macro.">
            <IconButton size="small" disabled={false} onClick={onStartNew}>
              <RestartAltIcon />
            </IconButton>
          </Tooltip>
          <Tooltip title="Load an existing macro that is structured like a global operation.">
            <IconButton size="small" disabled={false} onClick={onRequestLoadExisting}>
              <RestartAltIcon />
            </IconButton>
          </Tooltip>
        </ButtonGroup>
        <Typography>{uuid}</Typography>
      </Box>
      <Box
        sx={{
          flex: 1,
          minHeight: 0,
          overflowY: "auto",
          p: 2,
        }}
      >
        <TextField
          value={title ?? ""}
          onChange={(e) => setTitle(e.target.value)}
          label="Macro Name"
          variant="outlined"
          fullWidth
          placeholder="My new Global Operation Macro"
        />
        <TextField
          value={description ?? ""}
          onChange={(e) => setDescription(e.target.value)}
          label="Macro Description"
          variant="outlined"
          fullWidth
          placeholder="This macro does a complex global operation"
        />
        <Paper sx={{ p: 1, border: 1 }}>
          <Typography>If:</Typography>
          {filters.sort(sortInactiveLast).map(filterDTOtoComponent)}
          <IconButton size="small" disabled={false} onClick={onAddFilter} className="clear-btn">
            <AddIcon />
          </IconButton>
        </Paper>
        <Divider orientation="vertical" flexItem />
        <Paper sx={{ p: 1, border: 1 }}>
          <Typography>Then:</Typography>
          {appliers.sort(sortInactiveLast).map(applyDTOtoComponent)}
          <IconButton size="small" disabled={false} onClick={onAddApplier} className="clear-btn">
            <AddIcon />
          </IconButton>
        </Paper>
      </Box>
      <FilterValueDialog
        key={editorItem?.item.uid}
        open={!!editorItem && editorItem.type === "filter"}
        action={editorItem?.item}
        setAction={(updatedFilter) => updateFilterItem(updatedFilter, false)}
        onClose={() => setEditorItem(null)}
        onViewItem={() => {}}
      />

      <SelectDialog<StepItemType>
        open={addItem !== undefined}
        items={addItem === "applier" ? defaultApplyActions : defaultFilters}
        getId={(item) => item.uid}
        getLabel={(item) => item.name}
        onClose={(selected) => {
          if (addItem === "applier") {
            const list: StepItemType[] = [
              ...appliers,
              ...selected.map((i) =>
                actionAutoName({ ...i, uid: crypto.randomUUID(), active: true }),
              ),
            ];
            setAppliers(list);
          } else if (addItem === "filter") {
            const list: StepItemType[] = [
              ...filters,
              ...selected.map((i) =>
                filterAutoName({ ...i, uid: crypto.randomUUID(), active: true }),
              ),
            ];
            setFilters(list);
          }
          setAddItem(undefined);
        }}
      />
      <SelectDialog<runnableMacro>
        open={loadMacros !== undefined}
        items={loadMacros ?? []}
        getId={(item) => item.uid}
        getLabel={(item) => item.name}
        onClose={(selected) => {
          if (selected.length !== 0) onLoadExisting(selected[0]);
          setLoadMacros(undefined);
        }}
      />
    </Box>
  );
};
