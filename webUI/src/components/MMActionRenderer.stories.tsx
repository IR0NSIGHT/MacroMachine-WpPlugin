import type { Meta, StoryObj } from '@storybook/react'
import MMActionRenderer from './MMActionRenderer'
import { MMAction } from '../types/MMAction'

const sampleAction: MMAction = {
  inputId: 'ALWAYS',
  inputData: [],
  outputId: 'TERRAIN',
  outputData: [],
  actionType: 'SET',
  inputPoints: [0, 5, 10, 15],
  outputPoints: [10, 25, 40, 55],
  name: 'apply snow',
  description: 'Gradually increase terrain height based on input',
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
      inputPoints: [0, 2, 4, 6, 8, 10, 12, 14],
      outputPoints: [128, 120, 100, 80, 60, 40, 20, 0],
      name: 'Height-based water depth mapping',
      description: 'Maps terrain height to water depth - higher terrain = shallower water',
      uid: '8865142f-ab9d-4d27-9afd-197fa5cb214e',
    },
  },
}

export const ComplexMapping: Story = {
  args: {
    action: {
      ...sampleAction,
      actionType: 'SET',
      inputPoints: [0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10],
      outputPoints: [0, 3, 8, 15, 24, 35, 48, 63, 80, 99, 120],
      name: 'Quadratic terrain amplification',
      description: 'Amplifies terrain variations using a quadratic mapping formula',
      uid: 'abc12345-def6-7890-ghij-klmnopqrstuv',
    },
  },
}
