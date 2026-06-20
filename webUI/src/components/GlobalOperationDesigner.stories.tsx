import type { Meta, StoryObj } from "@storybook/react-vite";
import { GlobalOperationDesigner } from "./GlobalOperationDesigner";

const meta: Meta<typeof GlobalOperationDesigner> = {
  title: "Components/GlobalOperationDesigner",
  component: GlobalOperationDesigner,
};

export default meta;

type Story = StoryObj<typeof GlobalOperationDesigner>;

export const Default: Story = {
  args: {
    actions: [],
    macros: [],
    onSave: alert,
    onExecute: alert,
  },
};
