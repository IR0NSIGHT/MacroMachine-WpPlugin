import type { Meta, StoryObj } from '@storybook/react'
import OutputProvider from './OutputProvider'

const meta: Meta<typeof OutputProvider> = {
  title: 'Components/OutputProvider',
  component: OutputProvider,
}

export default meta

type Story = StoryObj<typeof OutputProvider>

export const Terrain: Story = {
  args: {
    outputId: 'TERRAIN',
    outputData: [],
  },
}

export const StonePalette: Story = {
  args: {
    outputId: 'STONE_PALETTE',
    outputData: [0, 10, 4, 12, 13, 72, 73, 74, 150, 75, 5],
  },
}

export const BinarySpraypaint: Story = {
  args: {
    outputId: 'BINARY_SPRAYPAINT',
    outputData: ['Plants.5a395d0a77e053bd', 'Waterlilly', true],
  },
}

export const NibbleLayer: Story = {
  args: {
    outputId: 'NIBBLE_LAYER',
    outputData: [
      'Heatmap',
      'org.ironsight.wpplugin.macropainter.heatmaplayer',
      false,
      [8191, 24575, 39679, 56063, 65515, 65444, 65384, 65317],
    ],
  },
}
