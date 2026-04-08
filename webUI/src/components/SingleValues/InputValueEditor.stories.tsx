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

export const Annotations: Story = {
  render: () => {
    const [value, setValue] = useState<NamedValue>({
      numericValue: 1,
      displayName: 'One',
    })

    return (
      <InputValueEditor
        label="Input"
        value={value.numericValue}
        input={annotationsIO}
        onChange={setValue}
      />
    )
  },
}

export const Biomes: Story = {
  render: () => {
    const [value, setValue] = useState<NamedValue>({
      numericValue: 1,
      displayName: 'One',
    })

    return (
      <InputValueEditor
        label="Input"
        value={value.numericValue}
        input={biomesIO}
        onChange={setValue}
      />
    )
  },
}