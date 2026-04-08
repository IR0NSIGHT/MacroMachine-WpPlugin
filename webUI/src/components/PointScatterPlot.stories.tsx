import type { Meta, StoryObj } from '@storybook/react-vite';
import { useState } from 'react';
import PointScatterPlot, { PointScatterPlotProps } from './PointScatterPlot';
import { heightIO, annotationsIO, waterdepthIO, terrainIO } from '../mock/dummyIOs';
import { MappingPoint } from '@/types/MappingPoint';

const meta: Meta<typeof PointScatterPlot> = {
  component: PointScatterPlot,
}

export default meta;

// Utility to generate random values
const generateRandomValues = (count: number, min = 0, max = 100): number[] => {
  const values = [];
  for (let i = 0; i < count; i++) {
    values.push(Math.floor(Math.random() * (max - min + 1)) + min);
  }
  return values;
};

// Wrapper template for stateful stories
const Template = (args: PointScatterPlotProps) => {
  const initialPoints: MappingPoint[] = args.xData.map((x, i) => ({
    x,
    y: args.yData[i],
    input: args.input,
    output: args.output,
  }));

  const [points, setPoints] = useState<MappingPoint[]>(initialPoints);

  return (
    <PointScatterPlot
      {...args}
      xData={points.map((p) => p.x)}
      yData={points.map((p) => p.y)}
      changePoint={(oldP, newP) => {
        setPoints(points.map((p) => (p.x === oldP.x && p.y === oldP.y ? newP : p)));
      }}
      addPoint={(pNew) => setPoints([...points, pNew])}
    />
  );
};

type Story = StoryObj<typeof PointScatterPlot>;

// Stories
export const Empty: Story = {
  render: Template,
  args: {
    xData: [],
    yData: [],
    input: annotationsIO,
    output: heightIO,
    title: 'Empty Mapping',
    interpolation: false,
  },
};

export const Annotation_Height: Story = {
  render: Template,
  args: {
    xData: [0, 3, 5, 7, 9, 12],
    yData: [0, 1, 2, 3, 4, 5],
    input: annotationsIO,
    output: heightIO,
    title: 'Annotations → Height',
    interpolation: false,
  },
};

const input30 = generateRandomValues(7, heightIO.min, heightIO.max).sort((a, b) => a - b);
const output30 = generateRandomValues(7, annotationsIO.min, annotationsIO.max);
export const Height_Annotations: Story = {
  render: Template,
  args: {
    xData: input30,
    yData: output30,
    input: heightIO,
    output: annotationsIO,
    title: '7 Random Data Points',
    interpolation: false,
  },
};

export const HeightToTerrain: Story = {
  render: Template,
  args: {
    xData: generateRandomValues(256, heightIO.min, heightIO.max).sort((a, b) => a - b),
    yData: generateRandomValues(256, terrainIO.min, terrainIO.max),
    input: heightIO,
    output: terrainIO,
    title: '256 Random Data Points',
    interpolation: false,
  },
};

const input256 = generateRandomValues(30, heightIO.min, heightIO.max).sort((a, b) => a - b);
const output256 = generateRandomValues(30, waterdepthIO.min, waterdepthIO.max);
export const HeightToWaterdepth: Story = {
  render: Template,
  args: {
    xData: input256,
    yData: output256,
    input: heightIO,
    output: waterdepthIO,
    title: '30 Random Points → Waterdepth',
    interpolation: true,
  },
};