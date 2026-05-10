import type { Meta, StoryObj } from "@storybook/react-vite";
import MMActionRenderer from "./MMActionRenderer";
import {
  onlyOnCyan as onlyOnCyanFilterAction,
  onlyOnLand as onlyOnLandFilterAction,
  raiseYonCyan,
  slopeToForest,
  slopeToTerrain,
} from "../mock/dummyActions";

const meta: Meta<typeof MMActionRenderer> = {
  component: MMActionRenderer,
};

export default meta;

type Story = StoryObj<typeof MMActionRenderer>;

export const DefaultRaiseOnCyan: Story = {
  args: {
    action: raiseYonCyan,
  },
};

export const SlopeToForest: Story = {
  args: {
    action: slopeToForest,
  },
};

export const DefaultRange: Story = {
  args: {
    action: slopeToTerrain,
  },
};

export const RangeFilter: Story = {
  args: {
    action: onlyOnLandFilterAction,
  },
};

export const DiscreteFilter: Story = {
  args: {
    action: onlyOnCyanFilterAction,
  },
};
