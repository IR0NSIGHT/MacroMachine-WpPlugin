import type { Meta, StoryObj } from "@storybook/react-vite";
import { SelectDialog, SelectDialogProps } from "./SelectDialog";
import defaultAppliers from "../assets/defaultApplyActions.json";
import defaultFilters from "../assets/defaultFilters.json";
import { ActionDTO, MacroDTO } from "@/types/DTO";
import defaultMacros from "../mocks/data/macros.json";
import defaultActions from "../mocks/data/actions.json";
const meta: Meta<typeof SelectDialog> = {
  title: "Components/SelectDialog",
  component: SelectDialog,
};

export default meta;

export const None: StoryObj<typeof SelectDialog<ActionDTO>> = {
  args: {
    getId(item: ActionDTO): string {
      return item.uid;
    },
    getLabel(item: ActionDTO): string {
      return item.name;
    },
    isSingleSelect: false,
    items: [],
    onClose(selected: ActionDTO[]): void {
      alert("selected: " + selected.map((i) => i.name + " - " + i.uid));
    },
    open: true,
    title: "Select actions",
  },
};

export const Many: StoryObj<typeof SelectDialog<ActionDTO>> = {
  args: {
    getId(item: ActionDTO): string {
      return item.uid;
    },
    getLabel(item: ActionDTO): string {
      return item.name;
    },
    isSingleSelect: false,
    items: defaultActions as ActionDTO[],
    onClose(selected: ActionDTO[]): void {
      alert("selected: " + selected.map((i) => i.name + " - " + i.uid));
    },
    open: true,
    title: "Select actions",
  },
};

const defaultFilterProps: SelectDialogProps<ActionDTO> = {
  getId(item: ActionDTO): string {
    return item.uid;
  },
  getLabel(item: ActionDTO): string {
    return item.name;
  },
  isSingleSelect: false,
  items: defaultFilters as ActionDTO[],
  onClose(selected: ActionDTO[]): void {
    alert("selected: " + selected.map((i) => i.name + " - " + i.uid));
  },
  open: true,
  title: "Select filters",
};
export const Filters: StoryObj<typeof SelectDialog<ActionDTO>> = {
  args: defaultFilterProps,
};

export const Appliers: StoryObj<typeof SelectDialog<ActionDTO>> = {
  args: {
    getId(item: ActionDTO): string {
      return item.uid;
    },
    getLabel(item: ActionDTO): string {
      return item.name;
    },
    isSingleSelect: false,
    items: defaultAppliers as ActionDTO[],
    onClose(selected: ActionDTO[]): void {
      alert("selected: " + selected.map((i) => i.name + " - " + i.uid));
    },
    open: true,
    title: "Select appliers",
  },
};

export const Macros: StoryObj<typeof SelectDialog<MacroDTO>> = {
  args: {
    getId(item: MacroDTO): string {
      return item.uid;
    },
    getLabel(item: MacroDTO): string {
      return item.name;
    },
    isSingleSelect: true,
    items: defaultMacros,
    onClose(selected: MacroDTO[]): void {
      alert("selected: " + selected.map((i) => i.name + " - " + i.uid));
    },
    open: true,
    title: "Select a macro",
  },
};
