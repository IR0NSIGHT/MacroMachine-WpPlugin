import type { Meta, StoryObj } from '@storybook/react-vite'
import { MappingPointTable } from './MappingPointTable'
import { alwaysIO, annotationsIO, filterIO, forestIO, heightIO } from '@/mock/dummyIOs'
import { InputOutput, NamedValue } from '@/types/InputOutput'
import { MappingPoint } from '@/types/MappingPoint'
import { useState } from 'react'

const meta: Meta<typeof MappingPointTable> = {
    component: MappingPointTable,
}

export default meta

type Story = StoryObj<typeof MappingPointTable>

const toMappingPoint = (value: NamedValue, input: InputOutput, output: InputOutput): MappingPoint => {
    return {
        x: value.numericValue,
        input: input,
        output: output,
        y: (value.numericValue % 3) + 1 == 0 ? output.ignoreValue :  output.values[Math.abs((value.numericValue + 5)) % output.values.length].numericValue,
    }
}

export const Default: Story = {
    render: (args) => {
        const [points, setPoints] = useState(args.points);
        console.log("points in table:",points);
        return (
            <MappingPointTable
                {...args}
                points={points}
                setPoints={setPoints}
            />
        );
    },
    args: {
        points: annotationsIO.values.filter(v => v.numericValue != annotationsIO.ignoreValue).map(val => toMappingPoint(val, annotationsIO, forestIO)),
        setPoints: newPoints => { console.log(" set points to: ", newPoints) }
    },
}

export const Always: Story = {
    ...Default,
    args: {
        points: alwaysIO.values.map(val => toMappingPoint(val, alwaysIO, annotationsIO)),
        setPoints: newPoints => { console.log(" set points to: ", newPoints) }
    },
}

export const Filter: Story = {
    ...Default,
    args: {
        points: annotationsIO.values.filter(v => v.numericValue != annotationsIO.ignoreValue).map(val => toMappingPoint(val, annotationsIO, filterIO)),
        setPoints: newPoints => { console.log(" set points to: ", newPoints) }
    },
}

export const ManyPoints: Story = {
    ...Default,
    args: {
        points: heightIO.values.filter(v => v.numericValue != annotationsIO.ignoreValue).map(val => toMappingPoint(val, heightIO, annotationsIO)),
        setPoints: newPoints => { console.log(" set points to: ", newPoints) }
    },
}