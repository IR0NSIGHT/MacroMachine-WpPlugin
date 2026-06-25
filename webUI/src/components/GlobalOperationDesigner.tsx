import {
  Box,
  ButtonGroup,
  List,
  Paper,
  Switch,
  TextField,
  Typography,
  Grid,
  ListItem,
  ListItemButton,
} from "@mui/material";
import Item from "@mui/material/Grid";

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
import { FilterInlineEditor, ioToIconName } from "@/features/FilterComponent";
import { useDefaultAppliersQuery, useDefaultFiltersQuery } from "@/API/queries";
import { PageLoadingSpinner } from "@/PageLoadingSpinner";
import { fillParentSx } from "@/App";
import { MMIconButton } from "./IconButton";
import React from "react";
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

const ApplierInlineEditor = ({ item, setItem, deleteItem, openEditorFor }: StepItemProps) => {
  return (
    <Box
      key={item.uid}
      sx={{
        display: "flex",
        flexDirection: "row",
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
          <MMIconButton
            disabled={!item.active}
            onClick={() => setItem(invertFilter(item))}
            icon={<SwitchLeftIcon />}
          />
        )}
        <MMIconButton
          disabled={!item.active}
          onClick={() => openEditorFor(item)}
          icon={<EditIcon />}
        />
        <MMIconButton disabled={false} onClick={deleteItem} icon={<ClearIcon />}/>
      </ButtonGroup>
      <Typography color={item.active ? "text.primary" : "text.disabled"}>{item.name}</Typography>
    </Box>
  );
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

function EditorPanel({
  title,
  listItems,
  addButtonTitle,
  addButtonVisible,
  onClearList,
  onAddItem,
}: {
  title: String;
  listItems: React.JSX.Element[];
  onClearList: () => void;
  onAddItem: () => void;
  addButtonTitle: string;
  addButtonVisible: boolean;
}) {
  return (
    <Paper sx={{ width: "100%", p: 1 }}>
      <Typography variant="h4">{title}</Typography>
      { listItems.length != 0 &&
        <MMIconButton
          disabled={false}
          onClick={() => onClearList()}
          icon={<ClearIcon />}
          tooltip={"Delete all items"}
          title="Delete all"
        />
      }
      <List
        sx={{
          display: "flex",
          flexDirection: "column",
        }}
      >
        {listItems}
        {addButtonVisible && (
          <ListItem disablePadding>
            <ListItemButton>
              <MMIconButton
                disabled={false}
                onClick={onAddItem}
                icon={<AddIcon />}
                tooltip={addButtonTitle}
                title={addButtonTitle}
              />
            </ListItemButton>
          </ListItem>
        )}
      </List>
    </Paper>
  );
}

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
    props.onSave(
      toMacroDTO(runnable),
      runnable.steps.filter((f) => isStepItem(f)),
    );
    console.log("save global operation:", runnable.steps);
    setTitle(runnable.name);
    setDescription(runnable.description);
  }

  function onStartNew() {
    setUUID(crypto.randomUUID());
    setTitle(undefined);
    setDescription(undefined);
    if (defaultFilters) setFilters(defaultFilters);
    if (defaultAppliers) setAppliers(defaultAppliers);
  }

  function onRequestLoadExisting() {
    const allowedMacros = props
      .macros!.map((m) => toRunnable(m, props.actions!, props.macros!))
      .filter((m) => m !== undefined)
      .filter((m) => isGlobalOpsMacro(m as runnableMacro) === true);

    setLoadMacros(allowedMacros); //FIXME linter doesnt like | undefined here
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
    console.log("load existing:", runnable);
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

  const unusedAppliers = defaultAppliers;

  const sxBreakPoints = { sm: 12, md: 6, xl: 4 };
  return (
    <Box
      sx={{
        ...fillParentSx,
        display: "flex",
        flexDirection: "column",
      }}
      p={1}
    >
      <Typography variant="h5" >Design a new Global Operation</Typography>
      <Box
        sx={{
          ...fillParentSx,
          display: "flex",
          flexDirection: "column",
          overflowY: "auto", // scrollable list

          gap: 1,
          p: 1,
        }}
      >
        <ButtonGroup variant="outlined">
          <MMIconButton
            disabled={false}
            onClick={onExecute}
            icon={<PlayArrowIcon />}
            tooltip="Execute this macro on your map."
          />
          <MMIconButton
            disabled={false}
            onClick={() =>
              props.onExecute(
                constructRunnable(sortedFilters, sortedAppliers, uuid, title, description),
                true,
              )
            }
            icon={<BugReportIcon />}
            tooltip="Debug-Execute this macro on your map, step-by-step."
          />
          <MMIconButton onClick={onSave} icon={<SaveIcon />} tooltip="Save this macro." />
          <MMIconButton
            disabled={false}
            onClick={onStartNew}
            tooltip="Start a new macro"
            icon={<RestartAltIcon />}
          />
          <MMIconButton
            disabled={false}
            onClick={onRequestLoadExisting}
            icon={<ManageSearchIcon />}
            tooltip="Load an existing macro that is structured like a global operation."
          />
        </ButtonGroup>

        <Paper sx={{ width: "100%" }}>
          <TextField
            value={title ?? ""}
            onChange={(e) => setTitle(e.target.value)}
            label="Operation Name"
            variant="outlined"
            fullWidth
            placeholder="My new Global Operation Macro"
          />
          <TextField
            value={description ?? ""}
            onChange={(e) => setDescription(e.target.value)}
            label="Operation Description"
            variant="outlined"
            fullWidth
            placeholder="This macro does a complex global operation"
          />
        </Paper>

        <Grid container spacing={2}>
          <Grid size={sxBreakPoints}>
            <Item>
              {
                <EditorPanel
                  title="Filters"
                  onClearList={() => setFilters([])}
                  listItems={sortedFilters.map((filterAction) => (
                    <FilterInlineEditor
                      key={filterAction.uid}
                      item={filterAction}
                      setItem={(item) => updateFilterItem(item, false, setFilters)}
                      deleteItem={() => updateFilterItem(filterAction, true, setFilters)}
                      openEditorFor={(filter) => setEditorItem({ item: filter, type: "filter" })}
                    />
                  ))}
                  addButtonTitle={"Add filter"}
                  onAddItem={() => setAddItem("filter")}
                  addButtonVisible={(unusedFilters?.length ?? 0) != 0}
                />
              }
            </Item>
          </Grid>
          <Grid size={sxBreakPoints}>
            <Item>
              <EditorPanel
                title={"Actions"}
                onClearList={() => setAppliers([])}
                listItems={sortedAppliers.map((modifierAction) => (
                  <ApplierInlineEditor
                    key={modifierAction.uid}
                    item={modifierAction}
                    setItem={(item) => updateApplyItem(item, false, setAppliers)}
                    deleteItem={() => updateApplyItem(modifierAction, true, setAppliers)}
                    openEditorFor={(applyItem) =>
                      setEditorItem({ item: applyItem, type: "action" })
                    }
                  />
                ))}
                onAddItem={() => setAddItem("applier")}
                addButtonTitle={"Add new action"}
                addButtonVisible={(unusedAppliers?.length ?? 0) != 0}
              />
            </Item>
          </Grid>
        </Grid>

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
            return ioToIconName(item.input);
          }}
          onClose={(selected) => {
            const list: StepItemType[] = [
              ...filters,
              ...selected.map((i) =>
                filterAutoName({
                  ...i,
                  uid: crypto.randomUUID(),
                  active: true,
                }),
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
          renderIcon={(item) => ioToIconName(item.output)}
          onClose={(selected) => {
            const list: StepItemType[] = [
              ...appliers,
              ...selected.map((i) =>
                actionAutoName({
                  ...i,
                  uid: crypto.randomUUID(),
                  active: true,
                }),
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
    </Box>
  );
};
