import type { Meta, StoryObj } from "@storybook/react";
import RangeValueAxisEditor from "./RangeEditor";

const meta: Meta<typeof RangeValueAxisEditor> = {
  title: "Components/RangeValueAxisEditor",
  component: RangeValueAxisEditor,
  parameters: {
    layout: "padded",
    backgrounds: {
      default: "dark",
      values: [
        { name: "dark", value: "#0b0f17" },
        { name: "light", value: "#ffffff" },
      ],
    },
  },
  argTypes: {
    min: { control: "number" },
    max: { control: "number" },
    allowedValues: { control: "object" },
    height: { control: "number" },
  },
};

export default meta;

type Story = StoryObj<typeof RangeValueAxisEditor>;

export const Default: Story = {
  args: {
    min: 0,
    max: 100,
    height: 90,
    allowedValues: ["low", "medium", "high"],
  },
};

export const TemperatureScale: Story = {
  args: {
    min: -20,
    max: 40,
    height: 90,
    allowedValues: ["cold", "cool", "warm", "hot"],
  },
};

export const FinancialRiskBands: Story = {
  args: {
    min: 0,
    max: 1,
    height: 90,
    allowedValues: ["safe", "moderate", "risky", "critical"],
    initialSegments: [
      { id: "1", start: 0, end: 0.25, value: "safe" },
      { id: "2", start: 0.25, end: 0.5, value: "moderate" },
      { id: "3", start: 0.5, end: 0.75, value: "risky" },
      { id: "4", start: 0.75, end: 1, value: "critical" },
    ],
  },
};

export const DenseRange: Story = {
  args: {
    min: 0,
    max: 1000,
    height: 90,
    allowedValues: ["A", "B", "C", "D", "E", "F"],
  },
};

export const WideInteractiveDemo: Story = {
  args: {
    min: 0,
    max: 100,
    height: 120,
    allowedValues: ["low", "medium", "high"],
  },
  parameters: {
    docs: {
      description: {
        story:
          "Click anywhere on the axis to split segments. Click a segment to change its value. Drag handles to resize segments.",
      },
    },
  },
};