import { useState } from 'react'
import Stack from '@mui/material/Stack'
import Box from '@mui/material/Box'
import IconButton from '@mui/material/IconButton'
import SaveIcon from '@mui/icons-material/Save'
import { ACTION_TYPES, ActionType, MMAction } from '../types/MMAction'
import PointScatterPlot from './PointScatterPlot'
import { MappingPoint } from '@/types/MappingPoint'
import RangeEditor from './segmentEditor/RangeEditor'
import { buildSegmentsFromAction, getMappingPointArrayFromSegments, Segment } from './segmentEditor/Segment'
import isEqual from 'lodash/isEqual';
import { MappingPointTable } from './MappingPointTable'
import { Paper, ButtonGroup } from '@mui/material'
import RestartAltIcon from '@mui/icons-material/RestartAlt';
import ExpandMoreIcon from '@mui/icons-material/ExpandMore';
import ExpandLessIcon from "@mui/icons-material/ExpandLess";
import TimelineIcon from '@mui/icons-material/Timeline';
import TableChartIcon from '@mui/icons-material/TableChart';
import ViewColumnIcon from '@mui/icons-material/ViewColumn';
import { EditableSelect, EditableText } from './EditableText'
import { InputOutput, isIgnoreValue } from '@/types/InputOutput'
import { fetchActionWithPoints } from '@/API/api'

interface MMActionRendererProps {
  action: MMAction
  onUpdate?: (updated: MMAction) => void
}

export default function MMActionRenderer({ action, onUpdate }: MMActionRendererProps) {
  const [draftAction, setDraftAction] = useState<MMAction>(action)
  const [showValues, setShowValues] = useState<boolean>(true);
  const [showTable, setShowTable] = useState<boolean>(false);

  const updatePoint = (oldP: MappingPoint, newP: MappingPoint): void => {
    const updatedPoints = draftAction.mappingPoints.map(p => (p.x === oldP.x && p.y === oldP.y) ? { x: newP.x, y: newP.y } : p).filter(p => !isIgnoreValue(action.output, p.y));;
    fetchActionWithPoints(draftAction.uid, updatedPoints).then(updatedAction => {
      console.log("got updated action from backend:", updatedAction);
      setDraftAction((prev) => ({
        ...prev,
        mappingPoints: updatedAction.mappingPoints,
        mappedInputs: updatedAction.mappedInputs,
        mappedOutputs: updatedAction.mappedOutputs
      }));
    });
  }

  const addPoint = (newP: MappingPoint): void => {
    // implement mutation to action, no submit yet
    setDraftAction((prev) => {
      return {
        ...prev,
        mappedInputs: [...prev.mappedInputs, newP.x],
        mappedOutputs: [...prev.mappedOutputs, newP.y],
      };
    })

  }
  const isTableEditor = draftAction.input.discrete;
  const isRangeEditor = !isTableEditor && draftAction.output.discrete;
  const isGridEditor = !isRangeEditor && !isTableEditor;

  const updateActionFromSegments = (segments: Segment[]): void => {
    console.log("update action from segmetns:", segments);
    const mappingPoints = getMappingPointArrayFromSegments(segments);
    console.log("got mapping points from segments:", mappingPoints);
    fetchActionWithPoints(draftAction.uid, mappingPoints).then(updatedAction => {
      console.log("got updated action from backend:", updatedAction);
      setDraftAction((prev) => ({
        ...prev,
        mappingPoints: updatedAction.mappingPoints,
        mappedInputs: updatedAction.mappedInputs,
        mappedOutputs: updatedAction.mappedOutputs
      }));
    });
  };

  const actionDiffers = !isEqual(action, draftAction);

  const allInputs = [draftAction.input];
  const allOutputs = [draftAction.output];

  const handleResetAction = () => { setDraftAction(action); };
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
          xData={draftAction.mappedInputs}
          yData={draftAction.mappedOutputs}
          mappingPoints={draftAction.mappingPoints}
          input={draftAction.input}
          output={draftAction.output}
          title={draftAction.name}
          interpolation={!draftAction.input.discrete && !draftAction.output.discrete}
          type={draftAction.actionType}
          changePoint={updatePoint}
          addPoint={addPoint} />
      }{
        (!showTable && isRangeEditor) && <RangeEditor input={draftAction.input} output={draftAction.output} segments={buildSegmentsFromAction(draftAction)} setSegments={updateActionFromSegments} />
      }
      {
        (showTable || isTableEditor) && <MappingPointTable
          points={toFullSetMappingPointList(draftAction)}
          setPoints={points => { const { inputPoints, outputPoints } = toNumericValueList(points); setDraftAction(prev => ({ ...prev, mappedInputs: inputPoints, mappedOutputs: outputPoints })) }} />
      }
    </>
  )

  const showHideValuesButton = (
    <IconButton size="small" onClick={() => { setShowValues(prev => !prev); }} color="primary">
      {!showValues && <ExpandMoreIcon />}
      {showValues && <ExpandLessIcon />}
    </IconButton>
  )

  const inputControl = (<EditableSelect<InputOutput> value={draftAction.input} setValue={io => setDraftAction(prev => ({ ...prev, input: io }))} options={allInputs} label="Input" />);
  const ACTION_TYPE_ITEMS = ACTION_TYPES.map((t) => ({
    uid: t,
    displayName: t,
  }));
  const actionTypeControl = (
    <EditableSelect
      value={{ uid: draftAction.actionType, displayName: draftAction.actionType }}
      setValue={(item) =>
        setDraftAction((prev) => ({
          ...prev,
          actionType: item.uid as ActionType,
        }))
      }
      options={ACTION_TYPE_ITEMS}
      label="Type"
    />
  );

  const outputControl = (<EditableSelect value={draftAction.output} setValue={io => setDraftAction(prev => ({ ...prev, output: io }))} options={allOutputs} label="Output" />)

  return (
    <Paper sx={{ mb: 2, p: 2 }} variant="outlined">
      <Stack direction="row" spacing={2} alignItems="center" sx={{ mb: 2 }}>
        {actionTitleComp}
        {actionDescriptionComp}

        <ButtonGroup sx={{ ml: 'auto' }}>
          {(actionDiffers) && (
            <IconButton size="small" onClick={handleResetAction} color="primary">
              <RestartAltIcon />
            </IconButton>
          )}
          {(actionDiffers) && (
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

const toFullSetMappingPointList = (action: MMAction): MappingPoint[] => {
  // as a base, create INGORE for all possible inputs
  const allValuesAsIgnore: MappingPoint[] = action.mappedInputs.map(inputV => ({
    x: inputV,
    y: action.output.ignoreValue,
    input: action.input,
    output: action.output
  }))

  // then replace with actual mappings where they exist
  return allValuesAsIgnore.map(p => { 
    const mappingPoint = action.mappingPoints.find((mappingPoint) => mappingPoint.x === p.x);
    if (mappingPoint === undefined) {
      return p; // no mapping for this input value, treat as ignore
    }
    return {
      x: p.x,
      y: mappingPoint.y,
      input: action.input,
      output: action.output
    }
  });

  
}

const toNumericValueList = (mappingpoints: MappingPoint[]): { inputPoints: number[], outputPoints: number[] } => {
  console.log("converting mapping points to numeric value lists:", mappingpoints);
  return ({
    inputPoints: mappingpoints.map(p => p.x),
    outputPoints: mappingpoints.map(p => p.y)
  })
}