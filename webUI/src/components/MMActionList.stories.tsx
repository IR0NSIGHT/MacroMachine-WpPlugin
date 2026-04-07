import type { Meta, StoryObj } from '@storybook/react'
import MMActionList from './MMActionList'
import { MMAction } from '../types/MMAction'
import { alwaysIO, annotationsIO, slopeIO, terrainIO, waterdepthIO } from '../mock/dummyIOs'

const sampleActions: MMAction[] = [
  {
    input: alwaysIO,
    output: terrainIO,
    actionType: 'SET',
    inputPoints: [0],
    outputPoints: [9],
    name: 'apply snow',
    description: 'Apply snow to all terrain',
    uid: 'f5e02009-97ae-4955-a521-92639642c71b',
  },
  {
    input: slopeIO,
    output: terrainIO,
    actionType: 'SET',
    inputPoints: [30, 90],
    outputPoints: [21, 75],
    name: 'Paint by slope',
    description: 'Paint terrain based on slope angle',
    uid: '4d006c6d-93f6-4326-81fe-60446dad53eb',
  },
  {
    input: waterdepthIO,
    output: annotationsIO,
    actionType: 'INCREMENT',
    inputPoints: [0, 13],
    outputPoints: [8, 0],
    name: 'Set: prefer shallow water',
    description: 'Increment heatmap based on water depth',
    uid: '8865142f-ab9d-4d27-9afd-197fa5cb214e',
  },
]

const meta: Meta<typeof MMActionList> = {
  title: 'Components/MMActionList',
  component: MMActionList,
}

export default meta

type Story = StoryObj<typeof MMActionList>

export const Default: Story = {
  args: {
    actions: sampleActions,
    title: 'Sample Actions',
  },
}

export const Empty: Story = {
  args: {
    actions: [],
    title: 'No Actions',
  },
}
