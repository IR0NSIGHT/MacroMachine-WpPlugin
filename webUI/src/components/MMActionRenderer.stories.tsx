import type { Meta, StoryObj } from '@storybook/react-vite'
import MMActionRenderer from './MMActionRenderer'
import { raiseYonCyan, slopeToForest } from '../mock/dummyActions'

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
