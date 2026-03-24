import ListItemIcon from '@mui/material/ListItemIcon'
import ListItemText from '@mui/material/ListItemText'
import MenuItem from '@mui/material/MenuItem'
import SvgIcon from '@mui/material/SvgIcon'

export interface RenderItem {
  uid: string
  label: string
  icon: React.ReactNode
}

interface ItemRendererProps {
  item: RenderItem
}

export default function ItemRenderer({ item }: ItemRendererProps) {
  return (
    <MenuItem key={item.uid} value={item.uid}>
      <ListItemIcon>
        <SvgIcon fontSize="small">{item.icon}</SvgIcon>
      </ListItemIcon>
      <ListItemText primary={item.label} />
    </MenuItem>
  )
}
