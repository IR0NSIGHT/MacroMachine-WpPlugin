import type { Meta, StoryObj } from '@storybook/react-vite'
import RangeValueAxisEditor from "./RangeEditor";
import { slopeIO, terrainIO } from "@/mock/dummyIOs";

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
};

export default meta;

type Story = StoryObj<typeof RangeValueAxisEditor>;

export const Default: Story = {
  args: {
    input: slopeIO,
    output: terrainIO,
    initialSegments: [{
      "start": 0,
      "end": 30,

      "value": {
        "numericValue": 0,
        "displayName": "Air"
      }
    }, {
      "start": 31,
      "end": 45,

      "value": {
        "numericValue": 1,
        "displayName": "Stone"
      }
    }, {
      "start": 46,
      "end": 60,

      "value": {
        "numericValue": 2,
        "displayName": "Grass Block"
      }
    }, {
      "start": 61,
      "end": 90,

      "value": {
        "numericValue": 3,
        "displayName": "Dirt"
      }
    }],
  },
};
