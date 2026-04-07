import type { Meta, StoryObj } from '@storybook/react'
import PointMapper from './PointMapper'
import { InputOutput } from '../types'
import { heightIO, annotationsIO, waterdepthIO, terrainIO } from '../mock/dummyIOs'

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

// Helper to create a simple InputOutput from min/max
const createSimpleIO = (min: number, max: number, unitName: string): InputOutput => ({
    displayName: "SimpleIO",
    min,
    max,
    ignoreValue: -999,
    values: Array.from({ length: max - min + 1 }, (_, i) => ({
        value: min + i,
        displayName: `${unitName} ${min + i}`,
    })),
    description: '',
    discrete: false,
    uid: '',
    parameters: []
})

const meta: Meta<typeof PointMapper> = {
    title: 'Components/PointMapper',
    component: PointMapper,
}

export default meta

type Story = StoryObj<typeof PointMapper>

const countLinear = getRandomPointCount()
const inputLinear = generateRandomValues(countLinear, 0, 256).sort((a, b) => a - b)
const outputLinear = inputLinear
const linear0to256IO: InputOutput = createSimpleIO(0, 256, 'Level')

const singleInput = generateRandomValues(1, 0, 100)[0]
const singleOutput = generateRandomValues(1, 0, 100)[0]
export const SinglePoint: Story = {
    args: {
        xData: [singleInput],
        yData: [singleOutput],
        input: createSimpleIO(0, 100, 'Input'),
        output: createSimpleIO(0, 100, 'Output'),
        title: 'Single Point',
        interpolation: false, // Added explicitly
    },
}

export const Empty: Story = {
    args: {
        xData: [],
        yData: [],
        input: createSimpleIO(0, 256, 'Input'),
        output: createSimpleIO(0, 256, 'Output'),
        title: 'Empty Mapping',
        interpolation: false, // Added explicitly
    },
}


export const Annotation_Height: Story = {
    args: {
        xData: [0, 3, 5, 7, 9, 12],
        yData: [0, 1, 2, 3, 4, 5],
        input: annotationsIO,
        output: heightIO,
        title: 'annotations to height mapping',
        interpolation: false, // Added explicitly
    },
}

const input30 = generateRandomValues(7, heightIO.min, heightIO.max).sort((a, b) => a - b)
const output30 = generateRandomValues(7, annotationsIO.min, annotationsIO.max)
export const Height_Annotations: Story = {
    args: {
        xData: input30,
        yData: output30,
        input: heightIO,
        output: annotationsIO,
        title: '30 Data Points',
         interpolation: false,
    },
}

export const heightToTerrain: Story = {
    args: {
        xData: generateRandomValues(256, heightIO.min, heightIO.max).sort((a, b) => a - b),
        yData:  generateRandomValues(256, terrainIO.min, terrainIO.max),
        input: heightIO,
        output: terrainIO,
        title: '256 Data Points',
        interpolation: false,
    },
}

const input256 = generateRandomValues(30, heightIO.min, heightIO.max).sort((a, b) => a - b)
const output256 = generateRandomValues(30, waterdepthIO.min, waterdepthIO.max)
export const heightToWaterdepth: Story = {
    args: {
        xData: input256,
        yData: output256,
        input: heightIO,
        output: waterdepthIO,
        title: '256 Data Points',
        interpolation: true,
    },
}