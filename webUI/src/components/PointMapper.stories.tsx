import type { Meta, StoryObj } from '@storybook/react'
import PointMapper from './PointMapper'
import { heightIO, annotationsIO, waterdepthIO, terrainIO } from '../mock/dummyIOs'

const generateRandomValues = (count: number, min: number = 0, max: number = 100): number[] => {
    const values = []
    for (let i = 0; i < count; i++) {
        values.push(Math.floor(Math.random() * (max - min + 1)) + min)
    }
    return values
}

const meta: Meta<typeof PointMapper> = {
    title: 'Components/PointMapper',
    component: PointMapper,
}

export default meta

type Story = StoryObj<typeof PointMapper>

export const Empty: Story = {
    args: {
        xData: [],
        yData: [],
        input: annotationsIO,
        output: heightIO,
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