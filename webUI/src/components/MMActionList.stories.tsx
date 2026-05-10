import type { Meta, StoryObj } from "@storybook/react-vite";
import MMActionList from "./MMActionList";
import { MMAction } from "../types/MMAction";
import {
  grassEverywhere as grassEverywhereAction,
  raiseYonCyan as raiseYonCyanAction,
  slopeToForest as slopeToForestAction,
} from "@/mock/dummyActions";
import Test_Macro from "@/mock/Test_Macro.json";
const sampleActions: MMAction[] = [grassEverywhereAction, slopeToForestAction, raiseYonCyanAction];

const meta: Meta<typeof MMActionList> = {
  component: MMActionList,
};

export default meta;

type Story = StoryObj<typeof MMActionList>;

export const Default: Story = {
  args: {
    actions: sampleActions,
    title: "Sample Actions",
  },
};

export const Empty: Story = {
  args: {
    actions: [],
    title: "No Actions",
  },
};

export const TestMacro: Story = {
  args: {
    actions: Test_Macro as MMAction[],
    title: "Test Macro Actions",
  },
};
