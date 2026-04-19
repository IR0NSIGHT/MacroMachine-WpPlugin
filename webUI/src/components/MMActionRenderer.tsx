import { useState } from 'react'
import Card from '@mui/material/Card'
import CardContent from '@mui/material/CardContent'
import CardHeader from '@mui/material/CardHeader'
import Stack from '@mui/material/Stack'
import Box from '@mui/material/Box'
import TextField from '@mui/material/TextField'
import IconButton from '@mui/material/IconButton'
import SaveIcon from '@mui/icons-material/Save'
import { ACTION_TYPES, isValidAction, MMAction } from '../types/MMAction'
import PointScatterPlot from './PointScatterPlot'
import { MappingPoint } from '@/types/MappingPoint'
import RangeEditor from './segmentEditor/RangeEditor'
import { buildSegmentsFromAction, mappingsFromSegments, Segment } from './segmentEditor/Segment'
import isEqual from 'lodash/isEqual';
import { MappingPointTable } from './MappingPointTable'
import { FormControl, InputLabel, Select, MenuItem, Divider, Paper, ButtonGroup } from '@mui/material'
import RestartAltIcon from '@mui/icons-material/RestartAlt';
import ExpandMoreIcon from '@mui/icons-material/ExpandMore';
import ExpandLessIcon from "@mui/icons-material/ExpandLess";
import TimelineIcon from '@mui/icons-material/Timeline';
import TableChartIcon from '@mui/icons-material/TableChart';
import ViewColumnIcon from '@mui/icons-material/ViewColumn';
import { EditableText } from './EditableText'

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
  const [showValues, setShowValues] = useState<boolean>(false);
  const [showTable, setShowTable] = useState<boolean>(false);

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

  const allInputs = [draftAction.input];
  const allOutputs = [draftAction.output];
  const allActionTypes = ACTION_TYPES;

  const handleResetAction = () => { setDraftAction(action); setDrafSegments(buildSegmentsFromAction(action)); };
  const handleSaveAction = () => { if (onUpdate) onUpdate(draftAction) };

  const actionTitleComp = (<EditableText value={draftAction.name} onChange={(val) => setDraftAction((prev) => ({ ...prev, name: val }))} variant='h5' placeholder='Name' label="Name" />);
  const actionDescriptionComp =
    <EditableText value={draftAction.description} onChange={(val) => setDraftAction((prev) => ({ ...prev, description: val }))} variant="body1" placeholder='Description' label="Description" />

  const switchViewModeButton =
    (!isTableEditor && <IconButton size="small" onClick={() => { setShowTable(prev => !prev); }} color="primary">
      {!showTable && <TableChartIcon /> /** switch to table view */}
      {showTable && isGridEditor && <TimelineIcon /> /** switch to grid view */}
      {showTable && isRangeEditor && <ViewColumnIcon /> /** switch to grid view */}
    </IconButton>)


  const dataViewComponent = (
    <>
      {
        (!showTable && isGridEditor) && <PointScatterPlot
          xData={draftAction.inputPoints}
          yData={draftAction.outputPoints}
          input={draftAction.input}
          output={draftAction.output}
          title={draftAction.name}
          interpolation={!draftAction.input.discrete && !draftAction.output.discrete}
          type={draftAction.actionType}
          changePoint={updatePoint}
          addPoint={addPoint} />
      }{
        (!showTable && isRangeEditor) && <RangeEditor input={draftAction.input} output={draftAction.output} segments={draftSegments} setSegments={updateActionFromSegments} />
      }
      {
        (showTable || isTableEditor) && <MappingPointTable
          points={toMappingPointList(draftAction)}
          setPoints={points => { const { inputPoints, outputPoints } = toNumericValueList(points); setDraftAction(prev => ({ ...prev, inputPoints: inputPoints, outputPoints: outputPoints })) }} />
      }
    </>
  )

  const showHideValuesButton = (
    <IconButton size="small" onClick={() => { setShowValues(prev => !prev); }} color="primary">
      {!showValues && <ExpandMoreIcon />}
      {showValues && <ExpandLessIcon />}
    </IconButton>
  )

  const inputControl = (<FormControl size="small" fullWidth>
    <InputLabel>Input</InputLabel>
    <Select
      value={draftAction.input.uid}
      onChange={e => setDraftAction({ ...draftAction, input: allInputs.find(io => io.uid == e.target.value) ?? draftAction.input })}>
      <MenuItem key={draftAction.input.uid} value={draftAction.input.uid}>
        <em>{draftAction.input.displayName}</em>
      </MenuItem>
    </Select>
  </FormControl>);

  const actionTypeControl = (<FormControl size="small" fullWidth>
    <InputLabel>Type</InputLabel>
    <Select
      value={draftAction.actionType}
      onChange={e => setDraftAction({ ...draftAction, actionType: allActionTypes.find(type => type == e.target.value) ?? draftAction.actionType })}>
      {allActionTypes.map(t => (<MenuItem key={t} value={t}>
        <em>{t}</em>
      </MenuItem>))}
    </Select>
  </FormControl>)

  const outputControl = (<FormControl size="small" fullWidth>
    <InputLabel>Output</InputLabel>
    <Select
      value={draftAction.output.uid}
      onChange={e => setDraftAction({ ...draftAction, output: allOutputs.find(io => io.uid == e.target.value) ?? draftAction.output })}>
      <MenuItem value={draftAction.output.uid}>
        <em>{draftAction.output.displayName}</em>
      </MenuItem>
    </Select>
  </FormControl>)

  return (
    <Paper sx={{ mb: 2, p: 2 }} variant="outlined">
      <Stack direction="row" spacing={2} alignItems="center" sx={{ mb: 2 }}>
        {actionTitleComp}
        {actionDescriptionComp}

        <ButtonGroup sx={{ ml: 'auto' }}>
          {(segmentsDiffer || actionDiffers) && (
            <IconButton size="small" onClick={handleResetAction} color="primary">
              <RestartAltIcon />
            </IconButton>
          )}
          {(segmentsDiffer || actionDiffers) && (
            <IconButton size="small" onClick={handleSaveAction} color="primary">
              <SaveIcon />
            </IconButton>
          )}
        </ButtonGroup>
      </Stack>

      <Stack spacing={2}>
        <Stack direction="row" spacing={2} alignItems="center" sx={{ py: 1 }}>
          {inputControl}
          {actionTypeControl}
          {outputControl}
        </Stack>

        <Stack direction="row" alignItems="center" sx={{ py: 1 }}>
          {showValues && switchViewModeButton}

          <Box sx={{ ml: "auto" }}>
            {showHideValuesButton}
          </Box>
        </Stack>
        {showValues && dataViewComponent}
      </Stack>
    </Paper>
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