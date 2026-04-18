import { useEffect, useState } from 'react'
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
import { isValidAction, MMAction } from '../types/MMAction'
import InputOutputDisplay from './InputOutput/InputProvider'
import PointScatterPlot from './PointScatterPlot'
import { MappingPoint } from '@/types/MappingPoint'
import RangeEditor from './segmentEditor/RangeEditor'
import { buildSegmentsFromAction, mappingsFromSegments, Segment } from './segmentEditor/Segment'
import EditOutlinedIcon from '@mui/icons-material/EditOutlined';
import isEqual from 'lodash/isEqual';
import { MappingPointTable } from './MappingPointTable'

interface MMActionRendererProps {
  action: MMAction
  onUpdate?: (updated: MMAction) => void
}

export default function MMActionRenderer({ action, onUpdate }: MMActionRendererProps) {
  const dataValidation = isValidAction(action)
  if (!dataValidation.valid) {
    throw new Error("Invalid action" + (action.name ?? "unknown action") + " , can not render: " + JSON.stringify(dataValidation, null, 3));
  }

  const [draftAction, setDraftAction] = useState<MMAction>(action)
  const [draftSegments, setDrafSegments] = useState<Segment[]>(buildSegmentsFromAction(action))
  const [isEditing, setIsEditing] = useState(false)
  const [name, setName] = useState(draftAction.name)
  const [description, setDescription] = useState(draftAction.description)
  useEffect(() => {
    console.log("draftAction State changed:", draftAction);
  }, [draftAction]);

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
      };
    })

  }
  const isTableEditor = draftAction.input.discrete;
  const isRangeEditor = !isTableEditor && draftAction.output.discrete;
  const isGridEditor = !isRangeEditor && !isTableEditor;
  
  const updateActionFromSegments = (segments: Segment[]): void => {
    console.log("update action from segmetns:", segments);
    const { inputs, outputs } = mappingsFromSegments(segments);
    setDraftAction((prev) => ({
      ...prev,
      inputPoints: inputs,
      outputPoints: outputs
    }));
    setDrafSegments(segments); // we need to keep segments separately, because transforming to action and back might merge adjacent segmetns with same output value
  };



  const segmentsDiffer =
    isRangeEditor &&
    !isEqual(draftSegments, buildSegmentsFromAction(action));

  const actionDiffers = !isEqual(action, draftAction);


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
              <InputOutputDisplay inputOutput={draftAction.input} type='input' />
            </Box>

            <Box sx={{ flex: 1 }}>
              <InputOutputDisplay inputOutput={draftAction.output} type='output' />
            </Box>
          </Stack>
          {(segmentsDiffer || actionDiffers) && <EditOutlinedIcon color="warning" />}

          <Box>
            {
              (isGridEditor) && <PointScatterPlot
                xData={draftAction.inputPoints}
                yData={draftAction.outputPoints}
                input={draftAction.input}
                output={draftAction.output}
                title={draftAction.name}
                interpolation={!draftAction.input.discrete && !draftAction.output.discrete}
                type={draftAction.actionType}
                changePoint={updatePoint}
                addPoint={addPoint} />
            }
            {
              (isRangeEditor) && <RangeEditor input={draftAction.input} output={draftAction.output} segments={draftSegments} setSegments={updateActionFromSegments} />
            }
            {
              isTableEditor && <MappingPointTable
                points={toMappingPointList(draftAction)}
                setPoints={points => { const { inputPoints, outputPoints } = toNumericValueList(points); setDraftAction(prev => ({ ...prev, inputPoints: inputPoints, outputPoints: outputPoints })) }} />
            }

          </Box>
        </Stack>
      </CardContent>
    </Card>
  )
}

const toMappingPointList = (action: MMAction): MappingPoint[] => {
  // action.inputPoints and outputPoints is a complete set of mappings, bijektiv
  return action.inputPoints.map((inputX, i) => ({ x: inputX, y: action.outputPoints[i], input: action.input, output: action.output }))
}

const toNumericValueList = (mappingpoints: MappingPoint[]): { inputPoints: number[], outputPoints: number[] } => {
  return ({
    inputPoints: mappingpoints.map(p => p.x),
    outputPoints: mappingpoints.map(p => p.y)
  })
}