import Card from '@mui/material/Card';
import CardActions from '@mui/material/CardActions';
import CardContent from '@mui/material/CardContent';
import CardMedia from '@mui/material/CardMedia';
import Button from '@mui/material/Button';
import Typography from '@mui/material/Typography';
import { CardActionArea, Box, CircularProgress, alpha } from '@mui/material';
import { components } from '@/generated/api-types';
import { theme } from '@/theme';

type MacroDTO = components["schemas"]["MacroDTO"];
type executionState = {
    isRunning: boolean;
    percentage: number;
};
export default function MacroCard(props: {
    macro: MacroDTO;
    execution?: executionState;
}) {
    const { macro, execution } = props;

    return (
        <Card sx={{ maxWidth: 345, position: 'relative' }}>
            <CardActionArea disabled={execution?.isRunning} sx={{ filter: execution?.isRunning ? 'blur(1.5px)' : 'none' }}>
                <CardMedia
                    sx={{ height: 140 }}
                    image="https://media.istockphoto.com/id/1772777335/de/foto/moraine-lake-trail.webp?s=2048x2048&w=is&k=20&c=OSa46vyZC-c8zs_px8f9NL8mw5LJcyGih4DupS-w_tY="
                    title="macro image"
                />

                <CardContent>
                    <Typography gutterBottom variant="h5" component="div">
                        {macro.name}
                    </Typography>

                    <Typography variant="body2" sx={{ color: 'text.secondary' }}>
                        {macro.description}
                    </Typography>
                </CardContent>
            </CardActionArea>

            <CardActions>
                <Button size="small" disabled={execution?.isRunning}>
                    Share
                </Button>
                <Button size="small" disabled={execution?.isRunning}>
                    Edit
                </Button>
            </CardActions>

            {execution?.isRunning && (
                <Box
                    sx={{
                        position: 'absolute',
                        top: 0,
                        left: 0,
                        right: 0,
                        bottom: 56,
                        display: 'flex',
                        alignItems: 'center',
                        justifyContent: 'center',
                        backgroundColor: alpha(theme.palette.background.default, 0.7),
                        zIndex: 2,
                    }}
                >
                    <Box sx={{ position: 'relative', display: 'inline-flex' }}>
                        <CircularProgress
                            variant="indeterminate"
                            size={64}
                            color="secondary"
                        />

                        <Box
                            sx={{
                                top: 0,
                                left: 0,
                                bottom: 0,
                                right: 0,
                                position: 'absolute',
                                display: 'flex',
                                alignItems: 'center',
                                justifyContent: 'center',
                            }}
                        >
                            <Typography
                                variant="caption"
                                component="div"
                                color="text.primary"
                            >
                                {execution?.percentage}%
                            </Typography>
                        </Box>
                    </Box>
                </Box>
            )}
        </Card>
    );
}