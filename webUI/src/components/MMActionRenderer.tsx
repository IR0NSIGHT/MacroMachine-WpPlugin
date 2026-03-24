import { useState } from 'react'
import Card from '@mui/material/Card'
import CardContent from '@mui/material/CardContent'
import CardHeader from '@mui/material/CardHeader'
import Typography from '@mui/material/Typography'
import Stack from '@mui/material/Stack'
import Box from '@mui/material/Box'
import Chip from '@mui/material/Chip'
import TextField from '@mui/material/TextField'
import IconButton from '@mui/material/IconButton'
import EditIcon from '@mui/icons-material/Edit'
import SaveIcon from '@mui/icons-material/Save'
import CancelIcon from '@mui/icons-material/Cancel'
import { MMAction } from '../types/MMAction'
import InputProvider from './InputProvider'
import OutputProvider from './OutputProvider'
import PointMapper from './PointMapper'

interface MMActionRendererProps {
  action: MMAction
  onUpdate?: (updated: MMAction) => void
}

export default function MMActionRenderer({ action, onUpdate }: MMActionRendererProps) {
  const [isEditing, setIsEditing] = useState(false)
  const [name, setName] = useState(action.name)
  const [description, setDescription] = useState(action.description)

  const handleEdit = () => {
    setIsEditing(true)
  }

  const handleSave = () => {
    const updated = { ...action, name, description }
    if (onUpdate) onUpdate(updated)
    setIsEditing(false)
  }

  const handleCancel = () => {
    setName(action.name)
    setDescription(action.description)
    setIsEditing(false)
  }

  const action_title = isEditing ? (
    <TextField
      size="small"
      value={name}
      onChange={(e) => setName(e.target.value)}
      fullWidth
    />
  ) : (
    action.name
  )

  const action_subheader = isEditing ? (
    <TextField
      size="small"
      value={description}
      onChange={(e) => setDescription(e.target.value)}
      fullWidth
      multiline
      rows={2}
    />
  ) : (
    action.description
  )

  return (
    <Card sx={{ mb: 2 }}>
      <CardHeader
        title={action_title}
        subheader={action_subheader}
        action={
          isEditing ? (
            <Stack direction="row" spacing={0.5}>
              <IconButton size="small" onClick={handleSave} color="success">
                <SaveIcon />
              </IconButton>
              <IconButton size="small" onClick={handleCancel} color="error">
                <CancelIcon />
              </IconButton>
            </Stack>
          ) : (
            <IconButton size="small" onClick={handleEdit}>
              <EditIcon />
            </IconButton>
          )
        }
        sx={{ pb: 1 }}
      />
      <CardContent>
        <Stack spacing={2}>
          <Box>
            <Typography variant="subtitle2">Action Type</Typography>
            <Chip label={action.actionType} size="small" color="primary" />
          </Box>

          <Stack direction="row" spacing={2}>
            <Box sx={{ flex: 1 }}>
              <InputProvider inputId={action.inputId} inputData={action.inputData} />
            </Box>

            <Box sx={{ flex: 1 }}>
              <OutputProvider outputId={action.outputId} outputData={action.outputData} />
            </Box>
          </Stack>

          <Box>
            <PointMapper
              inputPoints={action.inputPoints}
              outputPoints={action.outputPoints}
              title="Input → Output Mapping"
              height={350}
            />
          </Box>
        </Stack>
      </CardContent>
    </Card>
  )
}

