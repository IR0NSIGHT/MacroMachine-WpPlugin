import Card from "@mui/material/Card";
import CardActions from "@mui/material/CardActions";
import CardContent from "@mui/material/CardContent";
import CardMedia from "@mui/material/CardMedia";
import Typography from "@mui/material/Typography";
import { CardActionArea, Box, CircularProgress, alpha, IconButton } from "@mui/material";
import { components } from "@/generated/api-types";
import { theme } from "@/theme";
import PlayArrowIcon from "@mui/icons-material/PlayArrow";
import ShareIcon from "@mui/icons-material/Share";
import EditIcon from "@mui/icons-material/Edit";
import VisibilityIcon from "@mui/icons-material/Visibility";

type MacroDTO = components["schemas"]["MacroDTO"];
type executionState = components["schemas"]["ExecutionStateDTO"];
export default function MacroCard(props: {
  macro: MacroDTO;
  execution?: executionState;
  onRequestExecution: (isDebug: boolean) => void;
  imageURL?: string;
  onView: () => void;
  onEdit: () => void;
  onShare: () => void;
}) {
  const { macro, execution, imageURL } = props;
  const isMacroRunning = execution?.executionId == macro.uid;
  const percentage =
    execution?.currentStepIndex !== undefined && execution?.steps && execution.steps.length !== 0
      ? ((execution.currentStepIndex +
          execution.steps[execution.currentStepIndex].percentComplete / 100) /
          execution.steps.length) *
        100
      : 0;
  return (
    <Card
      sx={{
        maxWidth: 345,
        position: "relative",

        "&:hover .play-overlay": {
          opacity: isMacroRunning ? 0 : 1,
        },

        "&:hover": {
          cursor: isMacroRunning ? "default" : "pointer",
        },
      }}
    >
      <CardActionArea
        disabled={isMacroRunning}
        onClick={() => props.onRequestExecution(false)}
        sx={{
          filter: isMacroRunning ? "blur(1.5px)" : "none",
        }}
        title="run macro"
      >
        <CardMedia
          sx={{ height: 140 }}
          image={
            imageURL ||
            "https://worldpainter-blog.com/wp-content/uploads/2024/11/dannypan_2024-10-13-16-53_15stars_2.png"
          }
        />

        <CardContent>
          <Typography gutterBottom variant="h5">
            {macro.name}
          </Typography>

          <Typography variant="body2" sx={{ color: "text.secondary" }}>
            {macro.description}
          </Typography>
        </CardContent>
      </CardActionArea>

      <CardActions>
        <IconButton size="small" disabled={isMacroRunning} onClick={props.onShare}>
          <ShareIcon />
        </IconButton>
        <IconButton size="small" disabled={isMacroRunning} onClick={props.onEdit}>
          <EditIcon />
        </IconButton>
        <IconButton size="small" disabled={isMacroRunning} onClick={props.onView}>
          <VisibilityIcon />
        </IconButton>
      </CardActions>

      {/* PLAY OVERLAY (hover hint) */}
      <Box
        className="play-overlay"
        sx={{
          position: "absolute",
          top: 0,
          left: 0,
          right: 0,
          bottom: 56,
          display: "flex",
          alignItems: "center",
          justifyContent: "center",
          backgroundColor: "rgba(0,0,0,0.25)",
          opacity: 0,
          transition: "opacity 150ms ease-in-out",
          pointerEvents: "none",
          zIndex: 1,
        }}
      >
        <PlayArrowIcon sx={{ fontSize: 64, color: "white" }} />
      </Box>

      {/* RUNNING OVERLAY (blocks everything) */}
      {isMacroRunning && (
        <Box
          sx={{
            position: "absolute",
            top: 0,
            left: 0,
            right: 0,
            bottom: 56,
            display: "flex",
            alignItems: "center",
            justifyContent: "center",
            backgroundColor: alpha(theme.palette.background.default, 0.7),
            zIndex: 2,
          }}
        >
          <Box sx={{ position: "relative", display: "inline-flex" }}>
            <CircularProgress size={64} color="secondary" />

            <Box
              sx={{
                position: "absolute",
                display: "flex",
                alignItems: "center",
                justifyContent: "center",
                width: "100%",
                height: "100%",
              }}
            >
              <Typography variant="caption">{Math.round(percentage)}%</Typography>
            </Box>
          </Box>
        </Box>
      )}
    </Card>
  );
}
