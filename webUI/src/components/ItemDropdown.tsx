import { useState } from 'react'
import Box from '@mui/material/Box'
import FormControl from '@mui/material/FormControl'
import InputLabel from '@mui/material/InputLabel'
import Select, { SelectChangeEvent } from '@mui/material/Select'
import ItemRenderer, { RenderItem } from './ItemRenderer'

interface ItemDropdownProps {
  items: RenderItem[]
  label?: string
  onSelect?: (uid: string) => void
}

export default function ItemDropdown({ items, label = 'Item', onSelect }: ItemDropdownProps) {
  const [value, setValue] = useState<string>(items[0]?.uid ?? '')

  const handleChange = (event: SelectChangeEvent<string>) => {
    const selected = event.target.value
    setValue(selected)
    if (onSelect) onSelect(selected)
  }

  return (
    <Box sx={{ minWidth: 200 }}>
      <FormControl fullWidth>
        <InputLabel id="item-dropdown-label">{label}</InputLabel>
        <Select
          labelId="item-dropdown-label"
          id="item-dropdown"
          value={value}
          label={label}
          onChange={handleChange}
        >
          {items.map((item) => (
            <ItemRenderer key={item.uid} item={item} />
          ))}
        </Select>
      </FormControl>
    </Box>
  )
}
