import type { Meta, StoryObj } from '@storybook/react'
import ItemRenderer, { RenderItem } from './ItemRenderer'

const item: RenderItem = {
  uid: '1',
  label: 'Apple',
  icon: <path d="M12 2C10.3 2 8.7 2.7 7.5 3.9a7.5 7.5 0 0 0 0 10.6c.1.1.2.2.3.3l4.9-4.9 4.8 4.8c.9-1 1.4-2.3 1.4-3.7 0-2.1-1.1-4-2.8-5.1C15.2 6 13.6 5 12 5c-.1 0-.3 0-.4.0C12.8 4.2 13.4 3.1 13.4 2c0-.1 0-.1 0-.2A4.9 4.9 0 0 0 12 2z" />,
}

const meta: Meta<typeof ItemRenderer> = {
  title: 'Components/ItemRenderer',
  component: ItemRenderer,
}

export default meta

type Story = StoryObj<typeof ItemRenderer>

export const Default: Story = {
  args: {
    item,
  },
}
