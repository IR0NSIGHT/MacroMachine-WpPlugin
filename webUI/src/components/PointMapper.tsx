import EChartsReact from 'echarts-for-react'
import { useTheme } from '@mui/material/styles'
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
  const theme = useTheme()

  // Validate and create point pairs
  const points = inputPoints
    .slice(0, Math.min(inputPoints.length, outputPoints.length))
    .map((input, index) => [input, outputPoints[index]])

  if (points.length === 0) {
    return (
      <Paper variant="outlined" sx={{ p: 2, textAlign: 'center' }}>
        <Typography color="textSecondary">No points to display</Typography>
      </Paper>
    )
  }

  // Calculate axis ranges with padding
  const xValues = points.map((p) => p[0])
  const yValues = points.map((p) => p[1])

  const xMin = Math.min(...xValues)
  const xMax = Math.max(...xValues)
  const yMin = Math.min(...yValues)
  const yMax = Math.max(...yValues)

  const xPadding = (xMax - xMin) * 0.1 || 5
  const yPadding = (yMax - yMin) * 0.1 || 5

  const option = {
    title: {
      text: title,
      left: 'center',
      textStyle: {
        color: theme.palette.text.primary,
        fontSize: 14,
        fontWeight: 500,
      },
    },
    tooltip: {
      trigger: 'item',
      backgroundColor: theme.palette.background.paper,
      borderColor: theme.palette.divider,
      textStyle: {
        color: theme.palette.text.primary,
      },
      formatter: (params: any) => {
        if (params.componentSubType === 'scatter') {
          return `Input: ${params.value[0]}<br/>Output: ${params.value[1]}`
        }
        return ''
      },
    },
    grid: {
      left: 60,
      right: 20,
      top: 60,
      bottom: 60,
      containLabel: true,
    },
    xAxis: {
      type: 'value',
      name: 'Input',
      nameLocation: 'middle',
      nameGap: 30,
      nameTextStyle: {
        color: theme.palette.text.secondary,
        fontSize: 12,
      },
      axisLabel: {
        color: theme.palette.text.secondary,
      },
      axisLine: {
        lineStyle: {
          color: theme.palette.divider,
        },
      },
      splitLine: {
        lineStyle: {
          color: theme.palette.divider,
          type: 'dashed',
        },
      },
      min: xMin - xPadding,
      max: xMax + xPadding,
    },
    yAxis: {
      type: 'value',
      name: 'Output',
      nameLocation: 'middle',
      nameGap: 40,
      nameTextStyle: {
        color: theme.palette.text.secondary,
        fontSize: 12,
      },
      axisLabel: {
        color: theme.palette.text.secondary,
      },
      axisLine: {
        lineStyle: {
          color: theme.palette.divider,
        },
      },
      splitLine: {
        lineStyle: {
          color: theme.palette.divider,
          type: 'dashed',
        },
      },
      min: yMin - yPadding,
      max: yMax + yPadding,
    },
    series: [
      {
        data: points,
        type: 'scatter',
        symbolSize: 8,
        itemStyle: {
          color: theme.palette.primary.main,
          opacity: 0.8,
          borderColor: theme.palette.primary.dark,
          borderWidth: 1,
        },
        emphasis: {
          itemStyle: {
            color: theme.palette.primary.light,
            borderColor: theme.palette.primary.main,
            borderWidth: 2,
            shadowColor: theme.palette.action.hover,
            shadowBlur: 10,
          },
        },
      },
    ],
    toolbox: {
      feature: {
        dataZoom: {
          yAxisIndex: 'none',
          title: {
            zoom: 'Zoom',
            back: 'Reset Zoom',
          },
        },
        restore: {
          title: 'Restore',
        },
        saveAsImage: {
          title: 'Save as Image',
        },
      },
      right: 10,
      top: 10,
      iconStyle: {
        borderColor: theme.palette.action.active,
      },
    },
    dataZoom: [
      {
        type: 'inside',
        xAxisIndex: 0,
        yAxisIndex: 0,
        start: 0,
        end: 100,
      },
    ],
  }

  return (
    <Paper variant="outlined" sx={{ width, p: 2 }}>
      <Stack spacing={1}>
        <Typography variant="caption" color="textSecondary">
          Points: {points.length} | X Range: [{xMin.toFixed(2)}, {xMax.toFixed(2)}] | Y Range: [{yMin.toFixed(2)}, {yMax.toFixed(2)}]
        </Typography>
        <Box sx={{ width: '100%', height }}>
          <EChartsReact option={option} style={{ width: '100%', height: '100%' }} />
        </Box>
      </Stack>
    </Paper>
  )
}
