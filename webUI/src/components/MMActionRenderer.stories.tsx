import type { Meta, StoryObj } from '@storybook/react-vite'
import MMActionRenderer from './MMActionRenderer'
import { raiseYonCyan, slopeToForest } from '../mock/dummyActions'
import { forestIO, annotationsIO, heightIO } from '@/mock/dummyIOs'
import { MMAction } from '@/types/MMAction'
import { expect } from 'vitest'
import { buildSegmentsFromAction } from './segmentEditor/Segment'

const meta: Meta<typeof MMActionRenderer> = {

  component: MMActionRenderer,
}

export default meta

type Story = StoryObj<typeof MMActionRenderer>

export const Default: Story = {
  args: {
    action: raiseYonCyan,
  },
}

export const SlopeToForest: Story = {
  args: {
    action: slopeToForest
  }
}

export const Increment: Story = {
  args: {
    action: {
      ...raiseYonCyan,
      actionType: "increment",
      inputPoints: [0, 13],
      outputPoints: [8, 0],
      name: 'Set: prefer shallow water',
      description: 'Default filter: block all blocks that are steeper than this angle',
      uid: '8865142f-ab9d-4d27-9afd-197fa5cb214e',
    },
  },
}

const outputMagenta = annotationsIO.values[3];
export const SimpleContinuousToDiscrete: Story = {
  args: {
    action: {
      name: "Test Action",
      description: "This is a test action",
      uid: "test-action",
      input: forestIO,
      output: annotationsIO,
      actionType: "increment",
      inputPoints: forestIO.values.filter(v => v.numericValue !== forestIO.ignoreValue).map(v => v.numericValue),
      outputPoints: forestIO.values.map(_ => outputMagenta.numericValue),
    },
  },
}

export const HeightToAnnotation: Story = {
  args: {
    action: {
      name: "Test Action",
      description: "This is a test action",
      uid: "test-action",
      input: heightIO,
      output: annotationsIO,
      actionType: "increment",
      inputPoints: heightIO.values.filter(v => v.numericValue !== heightIO.ignoreValue).map(v => v.numericValue),
      outputPoints: heightIO.values.map(v => Math.round(Math.abs(v.numericValue) / 20) % 15),
    },
  },
}
