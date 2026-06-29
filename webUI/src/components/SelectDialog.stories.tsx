import type { Meta, StoryObj } from "@storybook/react-vite";
import { SelectDialog, SelectDialogProps } from "./SelectDialog";
import defaultAppliers from "@/mocks/data/defaultApplyActions.json";
import defaultFilters from "@/mocks/data/defaultFilters.json";
import { ActionDTO, MacroDTO } from "@/types/DTO";
import defaultMacros from "../mocks/data/macros.json";
import { GetIconForIoType, GrassBlockSvg } from "./CustomSvgIcons";

import { InputOutputDTOTypeEnum } from "@/generated/client";
const meta: Meta<typeof SelectDialog> = {
  title: "Components/SelectDialog",
  component: SelectDialog,
};

export default meta;

type IconItem = {
  uid: string;
  name: string;
  ioType: InputOutputDTOTypeEnum;
};

Object.values(InputOutputDTOTypeEnum).map((v) => ({ uid: v, name: v, ioType: v }));
const iconItems: IconItem[] = Object.values(InputOutputDTOTypeEnum).map((v) => ({
  uid: v,
  name: v,
  ioType: v,
}));

export const AllIcons: StoryObj<typeof SelectDialog<IconItem>> = {
  args: {
    open: true,
    title: "Icon Showcase",
    items: iconItems,

    getId(item: IconItem) {
      return item.uid;
    },

    getLabel(item: IconItem) {
      return item.name;
    },

    isSingleSelect: false,

    onClose(selected: IconItem[], _confirmed: boolean) {
      alert(selected.map((i) => `${i.name} - ${i.uid}`).join("\n"));
    },
  },

  render: (args) => (
    <SelectDialog<IconItem> {...args} renderIcon={(icon) => GetIconForIoType(icon.ioType)} />
  ),
};

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
    onClose(selected: ActionDTO[], _confirmed: boolean): void {
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
    items: Array.from(
      { length: 700 },
      (_, i): ActionDTO => ({
        ...(defaultFilters[0] as ActionDTO),
        uid: crypto.randomUUID(),
        name: "action_" + i,
      }),
    ),
    onClose(selected: ActionDTO[], _confirmed: boolean): void {
      alert("selected: " + selected.map((i) => i.name + " - " + i.uid));
    },
    renderIcon: (_item: ActionDTO) => {
      return GrassBlockSvg();
      //  <IconImage
      //    src={
      //      "https://png.pngtree.com/recommend-works/png-clipart/20250321/ourmid/pngtree-green-check-mark-icon-png-image_15808519.png"
      //    }
      //    alt={item.name}
      //  />
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
  onClose(selected: ActionDTO[], _confirmed: boolean): void {
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
    onClose(selected: ActionDTO[], _confirmed: boolean): void {
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
    onClose(selected: MacroDTO[], _confirmed: boolean): void {
      alert("selected: " + selected.map((i) => i.name + " - " + i.uid));
    },
    open: true,
    title: "Select a macro",
  },
};
