import type { Meta, StoryObj } from "@storybook/react-vite";
import { ActionSelectDialog } from "./ActionSelectDialog";
import defaultAppliers from "../assets/defaultApplyActions.json";
import defaultFilters from "../assets/defaultFilters.json";
import { ActionDTO } from "@/types/DTO";

const meta: Meta<typeof ActionSelectDialog> = {
  title: "Components/ActionSelectDialog",
  component: ActionSelectDialog,
};

export default meta;

type Story = StoryObj<typeof ActionSelectDialog>;

const longList = defaultAppliers
  .map((_) => defaultAppliers.map((a) => ({ ...a, uid: crypto.randomUUID() })))
  .flat() as ActionDTO[];
export const Many: Story = {
  args: {
    open: true,
    actions: longList as ActionDTO[],
    onClose: console.log,
  },
};

export const DefaultFilter: Story = {
  args: {
    open: true,
    actions: [...defaultAppliers, ...defaultFilters] as ActionDTO[],
    onClose: console.log,
  },
};
export const DefaultAppliers: Story = {
  args: {
    open: true,
    actions: [...defaultAppliers] as ActionDTO[],
    onClose: console.log,
  },
};
