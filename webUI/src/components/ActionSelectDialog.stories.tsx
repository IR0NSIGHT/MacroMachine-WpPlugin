import type { Meta, StoryObj } from "@storybook/react-vite";
import { SelectDialog } from "./ActionSelectDialog";
import defaultAppliers from "../assets/defaultApplyActions.json";
import defaultFilters from "../assets/defaultFilters.json";
import { ActionDTO } from "@/types/DTO";

const meta: Meta<typeof SelectDialog> = {
  title: "Components/SelectDialog",
  component: SelectDialog,
};

export default meta;

type Story = StoryObj<typeof SelectDialog>;

const longList = defaultAppliers
  .map((_) => defaultAppliers.map((a) => ({ ...a, uid: crypto.randomUUID() })))
  .flat() as ActionDTO[];
export const Many: Story = {
  args: {
    open: true,
    items: longList as ActionDTO[],
    onClose: console.log,
  },
};

export const DefaultFilter: Story = {
  args: {
    open: true,
    items: [...defaultAppliers, ...defaultFilters] as ActionDTO[],
    onClose: console.log,
  },
};
export const DefaultAppliers: Story = {
  args: {
    open: true,
    items: [...defaultAppliers] as ActionDTO[],
    onClose: console.log,
  },
};
