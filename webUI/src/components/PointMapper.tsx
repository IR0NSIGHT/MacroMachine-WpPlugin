import Plot from 'react-plotly.js'
import Paper from '@mui/material/Paper'
import Box from '@mui/material/Box'
import Typography from '@mui/material/Typography'
import Stack from '@mui/material/Stack'
import { NamedValue, InputOutput } from '../types'

interface PointMapperProps {
  xData: number[]
  yData: number[]
  input: InputOutput
  output: InputOutput
  title: string
  interpolation: boolean
}

export default function PointMapper({
  xData,
  yData,
  input,
  output,
  title = 'Input to Output Mapping',
  interpolation = false,
}: PointMapperProps) {
  const points = xData.slice(0, Math.min(xData.length, yData.length))
  const outputSliced = yData.slice(0, points.length)

  // Create lookup maps for display names
  const inputLabelMap = new Map(input.values.map((item) => [item.value, item.displayName]))
  const outputLabelMap = new Map(output.values.map((item) => [item.value, item.displayName]))

  // Build custom text for tooltips
  const customText = points.map((x, idx) => {
    const inputLabel = inputLabelMap.get(x)
    const outputLabel = outputLabelMap.get(outputSliced[idx])
    return `${inputLabel || x} → ${outputLabel || outputSliced[idx]}`
  })

  if (points.length === 0) {
    return (
      <Paper variant="outlined" sx={{ p: 2, textAlign: 'center' }}>
        <Typography color="textSecondary">No points to display</Typography>
      </Paper>
    )
  }

  // Calculate ranges for grid determination
  const xMin = Math.min(...points)
  const xMax = Math.max(...points)
  const yMin = Math.min(...outputSliced)
  const yMax = Math.max(...outputSliced)

  const xRange = xMax - xMin
  const yRange = yMax - yMin

  // Determine grid intervals: 1 for ranges <= 16, else 10
  const xDtick = Math.max(xRange <= 16 ? 1 : 10, yRange <= 16 ? 1 : 10)
  const yDtick = xDtick
  return (
    <Paper variant="outlined" sx={{ p: 2 }}>
      <Stack spacing={1}>
        <Typography variant="caption" color="textSecondary">
          Points: {points.length}
        </Typography>
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
              title,
              xaxis: input.discrete
                ? {
                  title: 'Input',
                  tickvals: input.values.map((v) => v.value),
                  ticktext: input.values.map((v) => v.displayName),
                }
                : {
                  title: 'Input',
                  dtick: xDtick,
                },

              yaxis: output.discrete
                ? {
                  title: 'Output',
                  tickvals: output.values.map((v) => v.value),
                  ticktext: output.values.map((v) => v.displayName),
                }
                : {
                  title: 'Output',
                  dtick: yDtick,
                },
              margin: { l: 50, r: 20, t: 50, b: 50 },
              showlegend: false,
            }
            }
            style={{ width: '100%', height: '100%' }}
          />
        </Box>
      </Stack>
    </Paper>
  )
}
