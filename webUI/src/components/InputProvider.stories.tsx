import type { Meta, StoryObj } from '@storybook/react'
import InputOutputDisplay from './InputProvider'

const meta: Meta<typeof InputOutputDisplay> = {
  title: 'Components/InputProvider',
  component: InputOutputDisplay,
}

export default meta

type Story = StoryObj<typeof InputOutputDisplay>

export const Always: Story = {
  args: {
    inputId: 'ALWAYS',
    inputData: [],
  },
}

export const Slope: Story = {
  args: {
    inputId: 'SLOPE',
    inputData: [],
  },
}

export const PerlinNoise: Story = {
  args: {
    inputId: 'PERLIN_NOISE',
    inputData: [25, 15, 10, 123123],
  },
}

export const NibbleLayer: Story = {
  args: {
    inputId: 'NIBBLE_LAYER',
    inputData: [
      'Heatmap',
      'org.ironsight.wpplugin.macropainter.heatmaplayer',
      false,
      [8191, 24575, 39679, 56063, 65515, 65444, 65384, 65317],
    ],
  },
}
