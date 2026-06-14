import {
  Box,
  ButtonGroup,
  Divider,
  IconButton,
  Paper,
  Stack,
  Switch,
  TextField,
  Tooltip,
  Typography,
} from "@mui/material";
import PlayArrowIcon from "@mui/icons-material/PlayArrow";
import SaveIcon from "@mui/icons-material/Save";
import { ActionDTO, MacroDTO } from "@/types/DTO";
import { useEffect, useMemo, useState } from "react";
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
import ManageSearchIcon from "@mui/icons-material/ManageSearch";
import { FilterInlineEditor } from "@/features/FilterComponent";
import { useDefaultAppliersQuery, useDefaultFiltersQuery } from "@/API/queries";
import { PageLoadingSpinner } from "@/PageLoadingSpinner";
import { GetIconForIoType } from "./CustomSvgIcons";
type Props = {
  onSave: (macro: MacroDTO, actions: ActionDTO[]) => void;
  onExecute: MacroExecuteRequester;
  macros?: MacroDTO[];
  actions?: ActionDTO[];
};

type StepItemProps = {
  item: StepItemType;
  setItem: (item: StepItemType) => void;
  deleteItem: () => void;
  openEditorFor: (item: StepItemType) => void;
};

function constructRunnable(
  sortedFiltersArg: StepItemType[],
  sortedAppliersArg: StepItemType[],
  uuid: string,
  title?: string,
  description?: string,
): runnableMacro {
  const actions = constructActions(sortedFiltersArg, sortedAppliersArg);
  const runnable: runnableMacro = {
    steps: actions,
    name: title ?? "My new global operation macro " + uuid,
    description: description ?? "",
    uid: uuid,
  };
  return runnable;
}

const StepItem = ({ item, setItem, deleteItem, openEditorFor }: StepItemProps) => {
  if (isFilter(item)) {
    return (
      <FilterInlineEditor
        item={item}
        setItem={setItem}
        deleteItem={deleteItem}
        openEditorFor={openEditorFor}
      />
    );
  } else {
    return (
      <Box
        key={item.uid}
        sx={{
          display: "flex",
          flexDirection: "row",
          "& .clear-btn": {
            opacity: 0.3,
            transition: "opacity 0.2s",
          },

          "&:hover .clear-btn": {
            opacity: 1,
          },
        }}
      >
        <Switch
          checked={item.active}
          onChange={(e) => {
            setItem({ ...item, active: e.target.checked });
          }}
        />
        <ButtonGroup>
          {isFilter(item) && (
            <IconButton
              size="small"
              disabled={!item.active}
              onClick={() => setItem(invertFilter(item))}
              className="clear-btn"
            >
              <SwitchLeftIcon />
            </IconButton>
          )}
          <IconButton
            size="small"
            disabled={!item.active}
            onClick={() => openEditorFor(item)}
            className="clear-btn"
          >
            <EditIcon />
          </IconButton>
          <IconButton size="small" disabled={false} onClick={deleteItem} className="clear-btn">
            <ClearIcon />
          </IconButton>
        </ButtonGroup>
        <Typography color={item.active ? "text.primary" : "text.disabled"}>{item.name}</Typography>
      </Box>
    );
  }
};

const sortInactiveLast = (a: StepItemType, b: StepItemType): number => {
  return sortAlphabetical(a, b);
};

const sortAlphabetical = (a: StepItemType, b: StepItemType): number => {
  const isFilter = a.input.type !== "ALWAYS" && a.output.type === "INTERMEDIATE_SELECTION";

  if (isFilter) return a.input.displayName.localeCompare(b.input.displayName);
  else return a.output.displayName.localeCompare(b.output.displayName);
};
function constructActions(filters: StepItemType[], appliers: StepItemType[]): StepItemType[] {
  const steps = [...filters, ...appliers].map((action) => ({
    ...action,
    uid: crypto.randomUUID(),
  })); //reshuffle UUIDs so no default action is present in two macros
  return steps;
}

const updateFilterItem = (
  action: StepItemType,
  isDelete: boolean,
  setFilters: (update: (prev: StepItemType[]) => StepItemType[]) => void,
) => {
  console.log("updateFilterItem", action, isDelete);
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
const updateApplyItem = (
  action: StepItemType,
  isDelete: boolean,
  setAppliers: (update: (prev: StepItemType[]) => StepItemType[]) => void,
) => {
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

export const GlobalOperationDesigner = (props: Props) => {
  console.log("Rerender Global Operation Designer!");

  const [filters, setFilters] = useState<StepItemType[]>([]);
  const [appliers, setAppliers] = useState<StepItemType[]>([]);
  const [editorItem, setEditorItem] = useState<{
    item: StepItemType;
    type: "filter" | "action";
  } | null>(null);

  const [title, setTitle] = useState<string | undefined>(undefined);
  const [description, setDescription] = useState<string | undefined>(undefined);
  const [uuid, setUUID] = useState<string>(crypto.randomUUID());

  const [addItem, setAddItem] = useState<"filter" | "applier" | undefined>(undefined);
  const [loadMacros, setLoadMacros] = useState<runnableMacro[] | undefined>();

  const { data: defaultFilters } = useDefaultFiltersQuery();
  const { data: defaultAppliers } = useDefaultAppliersQuery();

  if (!props.macros || !props.actions) {
    return <PageLoadingSpinner />;
  }

  useEffect(() => {
    onStartNew();
  }, []);

  useEffect(() => {
    if (defaultFilters) setFilters(defaultFilters);

    if (defaultAppliers) setAppliers(defaultAppliers);
  }, [uuid]);

  const sortedFilters = useMemo(() => filters.sort(sortInactiveLast), [filters]);
  const sortedAppliers = useMemo(() => appliers.sort(sortInactiveLast), [appliers]);

  function onExecute() {
    props.onExecute(
      constructRunnable(sortedFilters, sortedAppliers, uuid, title, description),
      false,
    );
  }

  function onSave() {
    const runnable = constructRunnable(sortedFilters, sortedAppliers, uuid, title, description);
    props.onSave(toMacroDTO(runnable), constructActions(sortedFilters, sortedAppliers));
    setTitle(runnable.name);
    setDescription(runnable.description);
  }

  function onStartNew() {
    setUUID(crypto.randomUUID());
    setTitle(undefined);
    setDescription(undefined);
  }

  function onRequestLoadExisting() {
    const allowedMacros = props
      .macros!.map((m) => toRunnable(m, props.actions!, props.macros!))
      .filter((m) => m !== undefined)
      .filter((m) => isGlobalOpsMacro(m as runnableMacro) === true);

    setLoadMacros(allowedMacros as runnableMacro[]); //FIXME linter doesnt like | undefined here
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

  const unusedFilters = useMemo(() => {
    const filterToString = (f: ActionDTO) => f.input.type + "_" + f.input.displayName;
    const existingFilters = new Set<string>(filters.map(filterToString));
    const unusedFilters = defaultFilters?.filter((f) => !existingFilters.has(filterToString(f)));
    console.log(
      "recalculate unused filters:",
      unusedFilters,
      " out of ",
      defaultFilters,
      " with used filters:",
      filters,
    );
    return unusedFilters;
  }, [defaultFilters, filters]);

  const filterDTOtoComponent = (action: StepItemType) => {
    return (
      <Box>
        <StepItem
          key={action.uid}
          item={action}
          setItem={(item) => updateFilterItem(item, false, setFilters)}
          deleteItem={() => updateFilterItem(action, true, setFilters)}
          openEditorFor={(filter) => setEditorItem({ item: filter, type: "filter" })}
        />
        <Divider />
      </Box>
    );
  };
  const applyDTOtoComponent = (action: StepItemType) => {
    return (
      <StepItem
        key={action.uid}
        item={action}
        setItem={(item) => updateApplyItem(item, false, setAppliers)}
        deleteItem={() => updateApplyItem(action, true, setAppliers)}
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
        gap: 1,
      }}
    >
      <Box
        sx={{
          display: "flex",
          flexDirection: "row",
          gap: 1,
        }}
      >
        <ButtonGroup variant="contained" aria-label="Basic button group">
          <Tooltip title="Execute this macro on your map.">
            <IconButton size="small" disabled={false} onClick={onExecute}>
              <PlayArrowIcon />
            </IconButton>
          </Tooltip>
          <Tooltip title="Debug-Execute this macro on your map, step-by-step.">
            <IconButton
              size="small"
              disabled={false}
              onClick={() =>
                props.onExecute(
                  constructRunnable(sortedFilters, sortedAppliers, uuid, title, description),
                  true,
                )
              }
            >
              <BugReportIcon />
            </IconButton>
          </Tooltip>
          <Tooltip title="Save this macro.">
            <IconButton size="small" onClick={onSave}>
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
              <ManageSearchIcon />
            </IconButton>
          </Tooltip>
        </ButtonGroup>
        <Typography>{uuid}</Typography>
      </Box>
      <Stack
        spacing={1}
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

        {/* --- Filters --- */}
        <Paper sx={{ p: 1 }}>
          <Typography>Filter by:</Typography>
          <ButtonGroup>
            <IconButton
              size="small"
              disabled={false}
              onClick={() => setFilters([])}
              className="clear-btn"
            >
              <ClearIcon />
            </IconButton>
          </ButtonGroup>
          {sortedFilters.map(filterDTOtoComponent)}
          <IconButton
            size="small"
            disabled={false}
            onClick={() => setAddItem("filter")}
            className="clear-btn"
          >
            <AddIcon />
          </IconButton>
        </Paper>

        <Divider orientation="vertical" flexItem />
        <Paper sx={{ p: 1 }}>
          <Typography>Apply:</Typography>
          <ButtonGroup>
            <IconButton
              size="small"
              disabled={false}
              onClick={() => setAppliers([])}
              className="clear-btn"
            >
              <ClearIcon />
            </IconButton>
          </ButtonGroup>
          {sortedAppliers.map(applyDTOtoComponent)}
          <IconButton
            size="small"
            disabled={false}
            onClick={() => setAddItem("applier")}
            className="clear-btn"
          >
            <AddIcon />
          </IconButton>
        </Paper>
      </Stack>

      <FilterValueDialog
        key={editorItem?.item.uid}
        open={!!editorItem && editorItem.type === "filter"}
        action={editorItem?.item}
        setAction={(updatedFilter) => updateFilterItem(updatedFilter, false, setFilters)}
        onClose={() => setEditorItem(null)}
        onViewItem={() => {}}
      />

      <SelectDialog<StepItemType>
        key={"filters_dlg"}
        open={addItem === "filter"}
        items={unusedFilters}
        getId={(item) => item.uid}
        getLabel={(item) => item.input.displayName}
        getSecondaryText={(item) => item.input.type}
        isSingleSelect={false}
        title={"Select additional filters"}
        renderIcon={(item) => {
          return GetIconForIoType(item.input.type);
        }}
        onClose={(selected) => {
          const list: StepItemType[] = [
            ...filters,
            ...selected.map((i) =>
              filterAutoName({ ...i, uid: crypto.randomUUID(), active: true }),
            ),
          ];
          setFilters(list);
          setAddItem(undefined);
        }}
      />

      <SelectDialog<StepItemType>
        key={"appliers_dlg"}
        open={addItem === "applier"}
        items={defaultAppliers}
        getId={(item) => item.uid}
        getLabel={(item) => item.output.displayName}
        getSecondaryText={(item) => item.output.type}
        isSingleSelect={false}
        title={"Select additional modifiers"}
        onClose={(selected) => {
          const list: StepItemType[] = [
            ...appliers,
            ...selected.map((i) =>
              actionAutoName({ ...i, uid: crypto.randomUUID(), active: true }),
            ),
          ];
          setAppliers(list);
          setAddItem(undefined);
        }}
      />

      <SelectDialog<runnableMacro>
        open={loadMacros !== undefined}
        items={loadMacros ?? []}
        getId={(item) => item.uid}
        getLabel={(item) => item.name}
        isSingleSelect={true}
        onClose={(selected) => {
          if (selected.length !== 0) onLoadExisting(selected[0]);
          setLoadMacros(undefined);
        }}
        title={"Select a macro to load"}
      />
    </Box>
  );
};
