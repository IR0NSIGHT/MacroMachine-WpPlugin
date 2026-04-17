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
import { ActionType } from '@/types/MMAction'
import { useTheme } from '@mui/material/styles'



export interface PointScatterPlotProps {
  xData: number[]
  yData: number[]
  input: InputOutput
  output: InputOutput
  title: string
  interpolation: boolean
  type: ActionType,
  changePoint: (oldP: MappingPoint, newP: MappingPoint) => void
  addPoint: (newP: MappingPoint) => void
}

export default function PointScatterPlot({
  xData,
  yData,
  input,
  output,
  title = 'Input to Output Mapping',
  interpolation = false,
  type,
  changePoint,
  addPoint,
}: PointScatterPlotProps) {
  const theme = useTheme()

  const points = xData.slice(0, Math.min(xData.length, yData.length))
  const outputSliced = yData.slice(0, points.length)

  const [editingPoint, setEditingPoint] = useState<boolean>(false)
  const [selectedPoint, setSelectedPoint] = useState<MappingPoint | null>(null)

  const inputLabelMap = new Map(input.values.map((item) => [item.numericValue, item.displayName]))
  const outputLabelMap = new Map(output.values.map((item) => [item.numericValue, item.displayName]))

  const customText = points.map((x, idx) => {
    const inputLabel = inputLabelMap.get(x)
    const outputLabel = outputLabelMap.get(outputSliced[idx])
    return `${inputLabel || x} → ${outputLabel || outputSliced[idx]}`
  })

  if (points.length === 0) {
    return (
      <Paper variant="outlined" sx={{ p: 2, textAlign: 'center' }}>
        <Typography color="textSecondary">No points to display</Typography>
        <Button
          onClick={() => {
            setSelectedPoint(null)
            setEditingPoint(true)
          }}
        >
          Add Point
        </Button>
      </Paper>
    )
  }

  const xMin = Math.min(...points)
  const xMax = Math.max(...points)
  const yMin = Math.min(...outputSliced)
  const yMax = Math.max(...outputSliced)

  const xRange = xMax - xMin
  const yRange = yMax - yMin

  const xDtick = Math.max(xRange <= 16 ? 1 : 10, yRange <= 16 ? 1 : 10)
  const yDtick = xDtick

  return (
    <Paper variant="outlined" sx={{ p: 2 }}>
      <Stack spacing={1}>
        <Typography variant="caption" color="textSecondary">
          Points: {points.length}
        </Typography>

        <Button
          size="small"
          onClick={() => {
            setSelectedPoint(null)
            setEditingPoint(true)
          }}
        >
          Add Point
        </Button>

        <Box sx={{ width: '100%' }}>
          <Plot
            data={[
              {
                x: points,
                y: outputSliced,
                text: customText,
                type: 'scatter',
                mode: interpolation ? 'lines+markers' : 'markers',
                line: { width: 2 },
                hovertemplate: '%{text}<extra></extra>',
              },
            ]}
            layout={{
              title: {
                text: title,
                font: {
                  family: theme.typography.fontFamily,
                  color: theme.palette.text.primary,
                },
              },
              paper_bgcolor: 'transparent',
              plot_bgcolor: 'transparent',

              font: {
                family: theme.typography.fontFamily,
                color: theme.palette.text.primary,
              },

              xaxis: input.discrete
                ? {
                  title: {
                    text: 'Input',
                    font: { color: theme.palette.text.secondary },
                  },
                  tickvals: input.values.map((v) => v.numericValue),
                  ticktext: input.values.map((v) => v.displayName),
                }
                : {
                  title: {
                    text: 'Input',
                    font: { color: theme.palette.text.secondary },
                  },
                  dtick: xDtick,
                  gridcolor: theme.palette.divider,
                },

              yaxis: output.discrete
                ? {
                  title: {
                    text: 'Output',
                    font: { color: theme.palette.text.secondary },
                  },
                  tickvals: output.values.map((v) => v.numericValue),
                  ticktext: output.values.map((v) => v.displayName),
                }
                : {
                  title: {
                    text: 'Output',
                    font: { color: theme.palette.text.secondary },
                  },
                  dtick: yDtick,
                  gridcolor: theme.palette.divider,
                },

              margin: { l: 50, r: 20, t: 50, b: 50 },
              showlegend: false,
            }}
            style={{ width: '100%', height: '100%' }}
            onClick={(event: any) => {
              const pt = event.points?.[0]
              if (!pt) return

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