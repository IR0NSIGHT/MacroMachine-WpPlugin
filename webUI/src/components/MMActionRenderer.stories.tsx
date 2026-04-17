import type { Meta, StoryObj } from '@storybook/react-vite'
import MMActionRenderer from './MMActionRenderer'
import { raiseYonCyan, slopeToForest } from '../mock/dummyActions'
import { forestIO, annotationsIO, heightIO, slopeIO, terrainIO } from '@/mock/dummyIOs'

const meta: Meta<typeof MMActionRenderer> = {

  component: MMActionRenderer,
}

export default meta

type Story = StoryObj<typeof MMActionRenderer>

export const DefaultRaiseOnCyan: Story = {
  args: {
    action: raiseYonCyan,
  },
}

export const SlopeToForest: Story = {
  args: {
    action: slopeToForest
  }
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
      actionType: "increments",
      inputPoints: forestIO.values.filter(v => v.numericValue !== forestIO.ignoreValue).map(v => v.numericValue),
      outputPoints: forestIO.values.map(_ => outputMagenta.numericValue),
    },
  },
}

export const ManyRanges: Story = {
  args: {
    action: {
      name: "Test Action",
      description: "This is a test action",
      uid: "test-action",
      input: heightIO,
      output: annotationsIO,
      actionType: "increments",
      inputPoints: heightIO.values.filter(v => v.numericValue !== heightIO.ignoreValue).map(v => v.numericValue),
      outputPoints: heightIO.values.map(v => Math.round(Math.abs(v.numericValue) / 20) % 15),
    },
  },
}


export const SomeRanges: Story = {
  args: {
    action: {
      name: "Test Action",
      description: "This is a test action",
      uid: "test-action",
      input: slopeIO,
      output: terrainIO,
      actionType: "increments",
      inputPoints: slopeIO.values.filter(v => v.numericValue !== slopeIO.ignoreValue).map(v => v.numericValue),
      outputPoints: slopeIO.values.map(v => Math.round(Math.abs(v.numericValue) / 30) % 15),
    },
  },
}
