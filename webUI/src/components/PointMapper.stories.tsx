import type { Meta, StoryObj } from '@storybook/react'
import PointMapper from './PointMapper'

const meta: Meta<typeof PointMapper> = {
  title: 'PointMapper',
  component: PointMapper,
  parameters: {
    layout: 'centered',
  },
}

export default meta
type Story = StoryObj<typeof meta>

export const LinearMapping: Story = {
  args: {
    inputPoints: [0, 1, 2, 3, 4, 5],
    outputPoints: [0, 2, 4, 6, 8, 10],
    title: 'Linear Mapping: y = 2x',
  },
}

export const QuadraticMapping: Story = {
  args: {
    inputPoints: [0, 1, 2, 3, 4, 5],
    outputPoints: [0, 1, 4, 9, 16, 25],
    title: 'Quadratic Mapping: y = x²',
  },
}

export const RandomScatter: Story = {
  args: {
    inputPoints: [
      0, 5, 10, 15, 20, 25, 30, 35, 40, 45, 50, 55, 60, 65, 70, 75, 80, 85, 90, 95,
    ],
    outputPoints: [
      5, 12, 8, 22, 18, 35, 28, 45, 38, 55, 48, 68, 58, 75, 70, 88, 82, 95, 90, 105,
    ],
    title: 'Scattered Data Points',
  },
}

export const NegativeValues: Story = {
  args: {
    inputPoints: [-10, -5, 0, 5, 10],
    outputPoints: [-20, -10, 0, 10, 20],
    title: 'Negative Values: y = 2x',
  },
}

export const SineWave: Story = {
  args: {
    inputPoints: Array.from({ length: 21 }, (_, i) => (i / 20) * Math.PI * 2),
    outputPoints: Array.from({ length: 21 }, (_, i) => Math.sin((i / 20) * Math.PI * 2) * 10),
    title: 'Sine Wave: y = 10 * sin(x)',
  },
}

export const SinglePoint: Story = {
  args: {
    inputPoints: [42],
    outputPoints: [84],
    title: 'Single Point Mapping',
  },
}

export const EmptyMapping: Story = {
  args: {
    inputPoints: [],
    outputPoints: [],
    title: 'No Points',
  },
}
