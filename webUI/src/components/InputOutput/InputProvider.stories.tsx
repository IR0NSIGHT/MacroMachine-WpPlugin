import type { Meta, StoryObj } from '@storybook/react-vite'
import InputOutputDisplay from './InputProvider'
import { alwaysIO, forestIO, heightIO, perlinNoiseIO, slopeIO } from '@/mock/dummyIOs'

const meta: Meta<typeof InputOutputDisplay> = {
 
  component: InputOutputDisplay,
}

export default meta

type Story = StoryObj<typeof InputOutputDisplay>

export const AlwaysInput: Story = {
  args: {
    inputOutput: alwaysIO,
    type: 'input',
  },
}

export const SlopeInput: Story = {
  args: {
    inputOutput: slopeIO,
    type: 'input',
  },
}

export const PerlinNoiseInput: Story = {
  args: {
    inputOutput: perlinNoiseIO,
    type: 'input',
  },
}

export const DeciduousOutput: Story = {
  args: {
    inputOutput: forestIO,
    type: 'output',
  },
}

export const HeightOutput: Story = {
  args: {
    inputOutput: heightIO,
    type: 'output',
  },
}
