import { useState } from 'react'
import Button from '@mui/material/Button'
import Typography from '@mui/material/Typography'
import Container from '@mui/material/Container'
import Stack from '@mui/material/Stack'
import ItemDropdown from './components/ItemDropdown'
import { RenderItem } from './components/ItemRenderer'
import './App.css'

function App() {
  const [count, setCount] = useState(0)
  const [selected, setSelected] = useState('')
  const items: RenderItem[] = [
    { uid: '1', label: 'Apple', icon: <path d="M12 2C10.3 2 8.7 2.7 7.5 3.9a7.5 7.5 0 0 0 0 10.6c.1.1.2.2.3.3l4.9-4.9 4.8 4.8c.9-1 1.4-2.3 1.4-3.7 0-2.1-1.1-4-2.8-5.1C15.2 6 13.6 5 12 5c-.1 0-.3 0-.4.0C12.8 4.2 13.4 3.1 13.4 2c0-.1 0-.1 0-.2A4.9 4.9 0 0 0 12 2z" />,
    },
    { uid: '2', label: 'Banana', icon: <path d="M6 10c-2.8 0-5.3 1.5-6 4 1.5-.5 3.4-1 4.9-.7 2.6.5 4.7 2.6 5.2 5.2.3 1.5 0 3.4-.1 4.9 2.5-.7 4-3.2 4-6 0-4.4-3.6-8-8-8z" /> },
    { uid: '3', label: 'Orange', icon: <path d="M12 2A10 10 0 1 0 22 12 10 10 0 0 0 12 2zm0 18a8 8 0 0 1-1-.1l-4.6 1.8 1.8-4.6A8 8 0 0 1 12 20zm1-17v5.2l4.3 4.3a8 8 0 0 1-4.3-9.5z" /> },
  ]

  return (
    <Container sx={{ py: 4 }}>
      <Stack spacing={3} alignItems="center">
        <Typography variant="h4" component="h1" gutterBottom>
          Vite + React + MUI
        </Typography>
        <ItemDropdown items={items} label="Fruit" onSelect={setSelected} />
        <Typography>Selected: {selected || 'None'}</Typography>
        <Typography>Count: {count}</Typography>
        <Button variant="contained" onClick={() => setCount((c) => c + 1)}>
          Increase
        </Button>
        <Typography variant="body2" color="text.secondary">
          Dropdown component is at src/components/ItemDropdown.tsx
        </Typography>
      </Stack>
    </Container>
  )
}

export default App
