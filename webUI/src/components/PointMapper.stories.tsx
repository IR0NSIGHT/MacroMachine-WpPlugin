import type { Meta, StoryObj } from '@storybook/react'
import PointMapper from './PointMapper'

const generateRandomValues = (count: number, min: number = 0, max: number = 100): number[] => {
  const values = []
  for (let i = 0; i < count; i++) {
    values.push(Math.floor(Math.random() * (max - min + 1)) + min)
  }
  return values
}

const getRandomPointCount = (): number => {
  return Math.floor(Math.random() * (300 - 15 + 1)) + 15
}

const meta: Meta<typeof PointMapper> = {
  title: 'Components/PointMapper',
  component: PointMapper,
}

export default meta

type Story = StoryObj<typeof PointMapper>

const countLinear = getRandomPointCount()
const inputLinear = generateRandomValues(countLinear, 0, 256).sort((a, b) => a - b)
export const Linear: Story = {
  args: {
    inputPoints: inputLinear,
    outputPoints: inputLinear,
    title: `Linear Mapping (1:1) - ${countLinear} points`,
  },
}

const countNonLinear = getRandomPointCount()
const inputNonLinear = generateRandomValues(countNonLinear, 0, 256).sort((a, b) => a - b)
const outputNonLinear = generateRandomValues(countNonLinear, 0, 256)
export const NonLinear: Story = {
  args: {
    inputPoints: inputNonLinear,
    outputPoints: outputNonLinear,
    title: `Non-Linear Mapping - ${countNonLinear} points`,
  },
}

const countInverted = getRandomPointCount()
const inputInverted = generateRandomValues(countInverted, 0, 256).sort((a, b) => a - b)
const outputInverted = inputInverted.slice().reverse()
export const Inverted: Story = {
  args: {
    inputPoints: inputInverted,
    outputPoints: outputInverted,
    title: `Inverted Mapping - ${countInverted} points`,
  },
}

export const SinglePoint: Story = {
  args: {
    inputPoints: [generateRandomValues(1, 0, 256)[0]],
    outputPoints: [generateRandomValues(1, 0, 256)[0]],
    title: 'Single Point',
  },
}

const countWide = getRandomPointCount()
const inputWide = generateRandomValues(countWide, 0, 256).sort((a, b) => a - b)
const outputWide = generateRandomValues(countWide, 0, 256)
export const WideRange: Story = {
  args: {
    inputPoints: inputWide,
    outputPoints: outputWide,
    title: `Wide Range Mapping - ${countWide} points`,
  },
}

export const Empty: Story = {
  args: {
    inputPoints: [],
    outputPoints: [],
    title: 'Empty Mapping',
  },
}

const input15 = generateRandomValues(15, 0, 256).sort((a, b) => a - b)
export const Points15: Story = {
  args: {
    inputPoints: input15,
    outputPoints: generateRandomValues(15, 0, 256),
    title: '15 Data Points',
  },
}

const input30 = generateRandomValues(30, 0, 256).sort((a, b) => a - b)
export const Points30: Story = {
  args: {
    inputPoints: input30,
    outputPoints: generateRandomValues(30, 0, 256),
    title: '30 Data Points',
  },
}

const input256 = generateRandomValues(256, 0, 256).sort((a, b) => a - b)
export const Points256: Story = {
  args: {
    inputPoints: input256,
    outputPoints: generateRandomValues(256, 0, 256),
    title: '256 Data Points',
  },
}
