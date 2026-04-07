import type { Meta, StoryObj } from '@storybook/react'
import { useState } from 'react'
import { InputDropdownSelector } from './InputSelector'
import { InputOutput, NamedValue } from '../types'
import { annotationsIO, biomesIO } from '../mock/dummyIOs'

const meta: Meta<typeof InputDropdownSelector> = {
  title: 'Components/InputDropdownSelector',
  component: InputDropdownSelector,
}

export default meta

type Story = StoryObj<typeof InputDropdownSelector>

export const Annotations: Story = {
  render: () => {
    const [value, setValue] = useState<NamedValue>({
      numericValue: 1,
      displayName: 'One',
    })

    return (
      <InputDropdownSelector
        value={value}
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
      <InputDropdownSelector
        value={value}
        input={biomesIO}
        onChange={setValue}
      />
    )
  },
}