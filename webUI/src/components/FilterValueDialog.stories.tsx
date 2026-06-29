import type { Meta, StoryObj } from "@storybook/react-vite";
import { FilterValueDialog } from "./FilterValueDialog";
import filters from "../mocks/data/defaultFilters.json";
import { StepItemType } from "@/features/Execution";

const meta: Meta<typeof FilterValueDialog> = {
  title: "Components/FilterValueDialog",
  component: FilterValueDialog,
};

export default meta;

type Story = StoryObj<typeof FilterValueDialog>;

export const Default: Story = {
  args: {
    open: true,
    action: { ...filters[0], active: true } as StepItemType,
    onClose: () => {},
    setAction: () => {},
    onViewItem: () => {},
  },
};
