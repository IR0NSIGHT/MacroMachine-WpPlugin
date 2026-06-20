import type { Meta, StoryObj } from "@storybook/react-vite";
import { ActionDetailsDialog } from "./ActionDetailsDialog";
import actions from "../../mocks/data/actions.json";
import { ActionDTO } from "@/types/DTO";

const meta: Meta<typeof ActionDetailsDialog> = {
  title: "Components/ActionDetailsDialog",
  component: ActionDetailsDialog,
};

export default meta;

type Story = StoryObj<typeof ActionDetailsDialog>;

export const Default: Story = {
  args: {
    open: true,
    action: actions[2] as ActionDTO,
    onClose: () => {},
    onViewItem: () => {},
  },
};
