import type { Meta, StoryObj } from '@storybook/react'
import MMActionRenderer from './MMActionRenderer'
import { MMAction } from '../types/MMAction'

const sampleAction: MMAction = {
  inputId: 'ALWAYS',
  inputData: [],
  outputId: 'TERRAIN',
  outputData: [],
  actionType: 'SET',
  inputPoints: [0],
  outputPoints: [9],
  name: 'apply snow',
  description: 'description of the action',
  uid: 'f5e02009-97ae-4955-a521-92639642c71b',
}

const meta: Meta<typeof MMActionRenderer> = {
  title: 'Components/MMActionRenderer',
  component: MMActionRenderer,
}

export default meta

type Story = StoryObj<typeof MMActionRenderer>

export const Default: Story = {
  args: {
    action: sampleAction,
  },
}

export const Increment: Story = {
  args: {
    action: {
      ...sampleAction,
      actionType: 'INCREMENT',
      inputPoints: [0, 13],
      outputPoints: [8, 0],
      name: 'Set: prefer shallow water',
      description: 'Default filter: block all blocks that are steeper than this angle',
      uid: '8865142f-ab9d-4d27-9afd-197fa5cb214e',
    },
  },
}
