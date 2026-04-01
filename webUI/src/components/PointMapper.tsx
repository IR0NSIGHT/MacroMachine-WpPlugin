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

  if (points.length === 0) {
    return (
      <Paper variant="outlined" sx={{ p: 2, textAlign: 'center' }}>
        <Typography color="textSecondary">No points to display</Typography>
      </Paper>
    )
  }

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
                y: outputPoints.slice(0, points.length),
                type: 'scatter',
                mode: 'markers',
                marker: { size: 8 },
              },
            ]}
            layout={{
              title,
              xaxis: { title: 'Input' },
              yaxis: { title: 'Output' },
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
