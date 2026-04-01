import type { Meta, StoryObj } from '@storybook/react'
import PointMapper from './PointMapper'

const meta: Meta<typeof PointMapper> = {
  title: 'Components/PointMapper',
  component: PointMapper,
}

export default meta

type Story = StoryObj<typeof PointMapper>

export const Linear: Story = {
  args: {
    inputPoints: [0, 5, 10],
    outputPoints: [0, 5, 10],
    title: 'Linear Mapping (1:1)',
  },
}

export const NonLinear: Story = {
  args: {
    inputPoints: [0, 2, 5, 10],
    outputPoints: [0, 1, 8, 15],
    title: 'Non-Linear Mapping',
  },
}

export const Inverted: Story = {
  args: {
    inputPoints: [0, 50, 100],
    outputPoints: [100, 50, 0],
    title: 'Inverted Mapping',
  },
}

export const SinglePoint: Story = {
  args: {
    inputPoints: [5],
    outputPoints: [5],
    title: 'Single Point',
  },
}

export const WideRange: Story = {
  args: {
    inputPoints: [0, 128, 255],
    outputPoints: [0, 64, 255],
    title: 'Wide Range Mapping',
  },
}

export const Empty: Story = {
  args: {
    inputPoints: [],
    outputPoints: [],
    title: 'Empty Mapping',
  },
}
