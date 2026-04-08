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
import InputOutputDisplay from './InputOutput/InputProvider'
import PointScatterPlot from './PointScatterPlot'
import { MappingPoint } from '@/types/MappingPoint'

interface MMActionRendererProps {
  action: MMAction
  onUpdate?: (updated: MMAction) => void
}

export default function MMActionRenderer({ action, onUpdate }: MMActionRendererProps) {
  const [draftAction, setDraftAction] = useState<MMAction>(action)
  const [isEditing, setIsEditing] = useState(false)
  const [name, setName] = useState(draftAction.name)
  const [description, setDescription] = useState(draftAction.description)

  const handleEdit = () => {
    setIsEditing(true)
  }

  const handleSave = () => {
    const updated = { ...action, name, description }
    if (onUpdate) onUpdate(updated)
    setIsEditing(false)
  }

  const handleCancel = () => {
    setName(draftAction.name)
    setDescription(draftAction.description)
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
    draftAction.name
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
    draftAction.description
  )

  const updatePoint = (oldP: MappingPoint, newP: MappingPoint): void => {
    // implement mutation to action, no submit yet
    setDraftAction((prev) => {
      const inputPoints = prev.inputPoints.map((p) => (p === oldP.x ? newP.x : p))
      const outputPoints = prev.outputPoints.map((p) => (p === oldP.y ? newP.y : p))
      return { ...prev, inputPoints, outputPoints }
    })
  }

  const addPoint = (newP: MappingPoint): void => {
    // implement mutation to action, no submit yet
    setDraftAction((prev) => {
      return {
        ...prev,
        inputPoints: [...prev.inputPoints, newP.x],
        outputPoints: [...prev.outputPoints, newP.y],
      }
    })

  }


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
            <Chip label={draftAction.actionType} size="small" color="primary" />
          </Box>

          <Stack direction="row" spacing={2}>
            <Box sx={{ flex: 1 }}>
              <InputOutputDisplay input={draftAction.input} />
            </Box>

            <Box sx={{ flex: 1 }}>
              <InputOutputDisplay input={draftAction.output} />
            </Box>
          </Stack>

          <Box>
            <PointScatterPlot
              xData={draftAction.inputPoints}
              yData={draftAction.outputPoints}
              input={draftAction.input}
              output={draftAction.output}
              title={draftAction.name}
              interpolation={!draftAction.input.discrete && !draftAction.output.discrete}
              type={draftAction.actionType}
              changePoint={updatePoint}
              addPoint={addPoint} />
          </Box>
        </Stack>
      </CardContent>
    </Card>
  )
}

