import { Box, Typography, Paper } from '@mui/material'

type DebugPanelProps = {
    title?: string
    data?: any
}

export default function DebugPanel({ title = 'DEBUG PANEL', data }: DebugPanelProps) {
    return (
        <Paper
            elevation={0}
            sx={{
                border: '3px dashed red',
                backgroundColor: 'rgba(255,0,0,0.05)',
                p: 2,
                my: 2,
            }}
        >
            <Typography
                variant="h5"
                sx={{
                    fontWeight: 'bold',
                    color: 'red',
                    mb: 2,
                }}
            >
                🚨 {title}
            </Typography>

            <Box
                sx={{
                    border: '2px solid orange',
                    p: 2,
                    mb: 2,
                }}
            >
                <Typography variant="subtitle1" sx={{ color: 'orange' }}>
                    Layout Box
                </Typography>
            </Box>

            <Box
                sx={{
                    border: '2px solid blue',
                    p: 2,
                    mb: 2,
                }}
            >
                <Typography variant="subtitle1" sx={{ color: 'blue' }}>
                    Content Box
                </Typography>

                {data && (
                    <Box
                        component="pre"
                        sx={{
                            mt: 1,
                            p: 1,
                            backgroundColor: '#111',
                            color: '#0f0',
                            fontSize: '12px',
                            overflow: 'auto',
                            maxHeight: 300,
                        }}
                    >
                        {JSON.stringify(data, null, 2)}
                    </Box>
                )}
            </Box>

            <Box
                sx={{
                    border: '2px solid green',
                    p: 2,
                }}
            >
                <Typography variant="subtitle1" sx={{ color: 'green' }}>
                    Footer Box
                </Typography>
            </Box>
        </Paper>
    )
}