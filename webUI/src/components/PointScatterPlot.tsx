import Plot from 'react-plotly.js'
import Paper from '@mui/material/Paper'
import Box from '@mui/material/Box'
import Typography from '@mui/material/Typography'
import Stack from '@mui/material/Stack'
import { MappingPointEditor } from './SingleValues/MappingPointEditor'
import Button from '@mui/material/Button'
import { useState } from 'react'
import { InputOutput } from '../types/InputOutput'
import { MappingPoint } from '../types/MappingPoint'
import { ActionType, MappingPointDTO } from '@/types/MMAction'
import { useTheme } from '@mui/material/styles'
import { Checkbox, FormControlLabel } from '@mui/material'



export interface PointScatterPlotProps {
  xData: number[]
  yData: number[]
  mappingPoints: MappingPointDTO[]
  input: InputOutput
  output: InputOutput
  title: string
  interpolation: boolean
  type: ActionType,
  changePoint: (oldP: MappingPoint, newP: MappingPoint) => void
  addPoint: (newP: MappingPoint) => void
}

const getGridSpacingForRange = (range: number) => {
  if (range <= 16) return 1
  if (range <= 100) return 10
  if (range <= 700) return 50
  if (range <= 1600) return 100
  return Math.pow(10, Math.floor(Math.log10(range)) - 1)
}

export default function PointScatterPlot({
  xData,
  yData,
  mappingPoints,
  input,
  output,
  title = 'Input to Output Mapping',
  interpolation = false,
  type,
  changePoint,
  addPoint,
}: PointScatterPlotProps) {
  const theme = useTheme()

  const mappingPointXs = mappingPoints.map(p => p.x)
  const outputSliced = mappingPoints.map(p => p.y)

  const [editingPoint, setEditingPoint] = useState<boolean>(false)
  const [selectedPoint, setSelectedPoint] = useState<MappingPoint | null>(null)

  const [showInterpolationLine, setShowInterpolationLine] = useState<boolean>(true)

  const inputLabelMap = new Map(input.values.map((item) => [item.numericValue, item.displayName]))
  const outputLabelMap = new Map(output.values.map((item) => [item.numericValue, item.displayName]))

  const getDisplayLabel = ({x,y}:{x: number, y: number}) => {
    const inputLabel = inputLabelMap.get(x)
    const outputLabel = outputLabelMap.get(y)
    return `${input.displayName}=${inputLabel || x} ${type} ${output.displayName}=${outputLabel || y}`
  }

  console.log(xData.length + yData.length)  // FIXME: only so eslint wont complain
  const xMin = input.min
  const xMax = input.max
  const yMin = output.min
  const yMax = output.max
  const xRange = xMax - xMin
  const yRange = yMax - yMin
  const rangePaddingPercent = 0.05;

  const xDtick = getGridSpacingForRange(input.max - input.min)
  const yDtick = getGridSpacingForRange(output.max - output.min)

  console.log("xDtick:", xDtick, "yDtick:", yDtick);

  const yLabels = output.values.filter(y => y.numericValue % yDtick === 0);
  const xLabels = input.values.filter(x => x.numericValue % xDtick === 0);

  const zip = (xs: number[], ys: number[]): {x:number, y:number}[] => {
    return xs.map((x, i) => ({ x, y: ys[i] }));
  };
  const controlPointData = {
    name: "ControlPoints",
    x: mappingPointXs,
    y: outputSliced,
    text: zip(mappingPointXs, outputSliced).map(getDisplayLabel),
    hovertemplate: '%{text}<extra></extra>',

    type: 'scatter',
    mode: showInterpolationLine ? 'lines+markers' : 'markers',

    line: {
      color: theme.palette.primary.light,
      width: 3,
      shape: "linear",
    },

    marker: {
      size: 16,
      color: theme.palette.primary.main,
    }
  };


  const mappingPointXsSet = new Set(mappingPointXs);
  const mappingPointIndices = xData.map((v, idx) => ({ x: v, idx: idx })).filter(input => !mappingPointXsSet.has(input.x)).map(p => p.idx);

  const nonMappingPointIdxSet = new Set(mappingPointIndices);
  const filterForNonControlPoints = (_v: any, idx: number) => nonMappingPointIdxSet.has(idx);
  const xDataFiltered = xData.filter(filterForNonControlPoints);
  const yDataFiltered = yData.filter(filterForNonControlPoints);
  const completeMappingData = {
    x: xDataFiltered,
    y: yDataFiltered,
    text: zip(xDataFiltered,yDataFiltered).map(getDisplayLabel),
    hovertemplate: '%{text}<extra></extra>',

    type: 'scatter',
    mode: 'markers',

    marker: {
      size: 6,
      color: theme.palette.text.disabled,
    }
  };

  return (
    <Paper variant="outlined" sx={{ p: 2 }}>
      <Stack spacing={1}>
        <Button
          size="small"
          onClick={() => {
            setSelectedPoint(null)
            setEditingPoint(true)
          }}
        >
          Add Point
        </Button>
        <FormControlLabel
          control={
            <Checkbox
              checked={showInterpolationLine}
              onChange={(e) => setShowInterpolationLine(e.target.checked)}
            />
          }
          label="Show interpolation line"
        />
        <Box sx={{ width: '100%' }}>
          <Plot
            config={{
              doubleClick: "reset",
            }}
            data={[
              completeMappingData,
              controlPointData
            ]}
            layout={{
              paper_bgcolor: 'transparent',
              plot_bgcolor: 'transparent',

              font: {
                family: theme.typography.fontFamily,
                color: theme.palette.text.primary,
              },

              uirevision: "static", // preserve zoom and pan on data update
              xaxis: {
                title: {
                  text: 'Input: ' + input.displayName,
                  font: { color: theme.palette.text.secondary },
                },
                tickvals: xLabels.map(v => v.numericValue),
                ticktext: xLabels.map(v => v.displayName),

                showgrid: true,
                gridcolor: theme.palette.text.disabled,
                gridwidth: 1,
                zeroline: true,
                zerolinecolor: theme.palette.text.secondary,

                range: [xMin - rangePaddingPercent * xRange, xMax + rangePaddingPercent * xRange],
                constrain: "range",
                autorange: false,
              },

              yaxis: {
                title: {
                  text: 'Output: ' + output.displayName,
                  font: { color: theme.palette.text.secondary },
                },
                tickvals: yLabels.map(v => v.numericValue),
                ticktext: yLabels.map(v => v.displayName),

                showgrid: true,
                gridcolor: theme.palette.text.disabled,
                gridwidth: 1,
                zeroline: true,
                zerolinecolor: theme.palette.text.secondary,

                range: [yMin - rangePaddingPercent * yRange, yMax + rangePaddingPercent * yRange],
                constrain: "range",
                autorange: false,
              },

              margin: { l: 50, r: 20, t: 50, b: 50 },
              showlegend: false,
            }}
            style={{ width: '100%', height: '100%' }}
            onClick={(event: any) => {
              const pt = event.points?.[0]
              if (!pt) return

              if (pt.data.name !== controlPointData.name) return;

              setSelectedPoint(pt);


              const oldP = { x: pt.x, input: input, y: pt.y, output: output }
              setSelectedPoint(oldP)
              setEditingPoint(true)
            }}
          />
        </Box>
      </Stack>
      <MappingPointEditor
        key={selectedPoint ? `${selectedPoint.x}-${selectedPoint.y}` : 'new-point-editor'}
        isNew={selectedPoint === null}
        editorActive={editingPoint}
        onClose={() => {
          setSelectedPoint(null)
          setEditingPoint(false)
        }}
        oldPoint={selectedPoint !== null ? selectedPoint : { x: input.min, input: input, y: output.min, output: output }}
        updatePoint={(oldP, newP) => {
          changePoint(oldP, newP)
          setSelectedPoint(null)
          setEditingPoint(false)
        }}
        addPoint={(newP) => {
          addPoint(newP)
          setEditingPoint(false)
        }}
        type={type}
      />

    </Paper>
  )
}