import type { Meta, StoryObj } from '@storybook/react-vite'
import RangeValueAxisEditor from "./RangeEditor";
import { forestIO, heightIO, slopeIO, terrainIO } from "@/mock/dummyIOs";
import { Segment } from './Segment';
import { InputOutput } from '@/types/InputOutput';
import { useState } from 'react';

const WrappeEditor = (props: { input: InputOutput, output: InputOutput, initialSegments: Segment[] }) => {
  const [segments, setSegments] = useState<Segment[]>(props.initialSegments);
  return (
    <RangeValueAxisEditor input={props.input} output={props.output} segments={segments} setSegments={setSegments} ></RangeValueAxisEditor>
  )
}

const meta: Meta<typeof WrappeEditor> = {
  title: "Components/RangeValueAxisEditor",
  component: WrappeEditor,
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

type Story = StoryObj<typeof WrappeEditor>;

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

export const SingleSegment: Story = {
  args: {
    input: slopeIO,
    output: terrainIO,
    initialSegments: [{
      "start": 0,
      "end": 90,

      "value": {
        "numericValue": 1,
        "displayName": "Stone"
      }
    }],
  },
};

export const ManyXValues: Story = {
  args: {
    input: heightIO,
    output: terrainIO,
    initialSegments: [{
      "start": heightIO.min,
      "end": 125,

      "value": {
        "numericValue": 1,
        "displayName": "Stone"
      }
    }, {
      "start": 126,
      "end": 127,

      "value": {
        "numericValue": 0,
        "displayName": "Air"
      }
    }, {
      "start": 128,
      "end": heightIO.max,

      "value": {
        "numericValue": 2,
        "displayName": "Grass Block"
      }
    }],
  },
};

export const FewXValues: Story = {
  args: {
    input: forestIO,
    output: terrainIO,
    initialSegments: [{
      "start": 0,
      "end": 8,

      "value": {
        "numericValue": 1,
        "displayName": "Stone"
      }
    },
    {
      "start": 9,
      "end": 15,

      "value": {
        "numericValue": 1,
        "displayName": "Stone"
      }
    }
    ],
  },
};