import type { Meta, StoryObj } from '@storybook/react-vite'
import InputOutputDisplay from './InputProvider'
import { alwaysIO, forestIO, perlinNoiseIO, slopeIO } from '@/mock/dummyIOs'

const meta: Meta<typeof InputOutputDisplay> = {
 
  component: InputOutputDisplay,
}

export default meta

type Story = StoryObj<typeof InputOutputDisplay>

export const Always: Story = {
  args: {
    input: alwaysIO
  },
}

export const Slope: Story = {
  args: {
    input: slopeIO
  },
}

export const PerlinNoise: Story = {
  args: {
    input: perlinNoiseIO
  },
}

export const NibbleLayer: Story = {
  args: {
    input: forestIO,
  },
}
