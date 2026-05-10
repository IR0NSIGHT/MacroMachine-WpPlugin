import type { Meta, StoryObj } from "@storybook/react-vite";
import MacroCard from "./MacroCard";

const meta: Meta<typeof MacroCard> = {
  title: "Components/MacroCard",
  component: MacroCard,
};

export default meta;

type Story = StoryObj<typeof MacroCard>;

export const Default: Story = {
  args: {
    macro: {
      executionUUIDs: [
        "ccdb9830-a63a-4b35-a6fb-e55ed4baef15",
        "8526cdb2-bb3c-4ce3-b6b7-9eeeb5ce2bef",
      ],
      activeActions: [true, false],
      name: "Apply: Auto Beach",
      description: "Simple beach generation under water at flat angles.",
      uid: "28558748-ff19-4ecf-9feb-7dec9173996f",
    },
  },
};

export const LongDescription: Story = {
  args: {
    macro: {
      executionUUIDs: ["11111111-a63a-4b35-a6fb-e55ed4baef15"],
      activeActions: [true],
      name: "Massive Terrain Cleanup",
      description:
        "This macro applies multiple terrain cleanup passes including slope smoothing, water correction, vegetation filtering, and height normalization across the selected region.",
      uid: "22222222-ff19-4ecf-9feb-7dec9173996f",
    },
  },
};

export const Minimal: Story = {
  args: {
    macro: {
      executionUUIDs: [],
      activeActions: [],
      name: "Empty Macro",
      description: "",
      uid: "33333333-ff19-4ecf-9feb-7dec9173996f",
    },
  },
};

export const RunningMacro: Story = {
  args: {
    macro: {
      executionUUIDs: [
        "ccdb9830-a63a-4b35-a6fb-e55ed4baef15",
        "8526cdb2-bb3c-4ce3-b6b7-9eeeb5ce2bef",
      ],
      activeActions: [true, false],
      name: "Apply: Auto Beach",
      description: "Simple beach generation under water at flat angles.",
      uid: "28558748-ff19-4ecf-9feb-7dec9173996f",
    },
    execution: {
      isRunning: true,
      percentage: 45,
    },
  },
};
