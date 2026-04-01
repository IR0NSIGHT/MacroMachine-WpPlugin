import Plot from 'react-plotly.js'
import Paper from '@mui/material/Paper'
import Box from '@mui/material/Box'
import Typography from '@mui/material/Typography'
import Stack from '@mui/material/Stack'

interface PointMapperProps {
  inputPoints: number[]
  outputPoints: number[]
  title?: string
  width?: string | number
  height?: string | number
}

export default function PointMapper({
  inputPoints,
  outputPoints,
  title = 'Input to Output Mapping',
  width = '100%',
  height = 400,
}: PointMapperProps) {
  const points = inputPoints.slice(0, Math.min(inputPoints.length, outputPoints.length))
  const outputSliced = outputPoints.slice(0, points.length)

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
  const xDtick = xRange <= 16 ? 1 : 10
  const yDtick = yRange <= 16 ? 1 : 10

  return (
    <Paper variant="outlined" sx={{ width, p: 2 }}>
      <Stack spacing={1}>
        <Typography variant="caption" color="textSecondary">
          Points: {points.length}
        </Typography>
        <Box sx={{ width: '100%', height }}>
          <Plot
            data={[
              {
                x: points,
                y: outputSliced,
                type: 'scatter',
                mode: 'lines+markers',
                line: { width: 2 },
              },
            ]}
            layout={{
              title,
              xaxis: { title: 'Input', dtick: xDtick },
              yaxis: { title: 'Output', dtick: yDtick },
              margin: { l: 50, r: 20, t: 50, b: 50 },
              showlegend: false,
            }}
            style={{ width: '100%', height: '100%' }}
          />
        </Box>
      </Stack>
    </Paper>
  )
}
