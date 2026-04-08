import type { Meta, StoryObj } from '@storybook/react-vite'
import { useState } from 'react'
import { InputValueEditor } from './InputValueEditor'
import { NamedValue } from '../../types/InputOutput'
import { annotationsIO, biomesIO } from '../../mock/dummyIOs'

const meta: Meta<typeof InputValueEditor> = {
  component: InputValueEditor,
}

export default meta

type Story = StoryObj<typeof InputValueEditor>

export const InputAnnotations: Story = {
  render: () => {
    const [value, setValue] = useState<NamedValue>({
      numericValue: 1,
      displayName: 'One',
    })

    return (
      <InputValueEditor
        includeIgnore={false}
        label="Input"
        value={value.numericValue}
        input={annotationsIO}
        onChange={setValue}
         open={true}
      />
    )
  },
}

export const InputBiome: Story = {
  render: () => {
    const [value, setValue] = useState<NamedValue>({
      numericValue: 1,
      displayName: 'One',
    })

    return (
      <InputValueEditor
        includeIgnore={false}
        label="Input"
        value={value.numericValue}
        input={biomesIO}
        onChange={setValue}
         open={true}
      />
    )
  },
}


export const OutputBiome: Story = {
  render: () => {
    const [value, setValue] = useState<NamedValue>(biomesIO.values.find(v => v.numericValue === biomesIO.ignoreValue)!)

    return (
      <InputValueEditor
        includeIgnore={true}
        label="Output"
        value={value.numericValue}
        input={biomesIO}
        onChange={setValue}
        open={true}
      />
    )
  },
}



