import Card from "@mui/material/Card";
import CardActions from "@mui/material/CardActions";
import CardContent from "@mui/material/CardContent";
import CardMedia from "@mui/material/CardMedia";
import Typography from "@mui/material/Typography";
import { CardActionArea, Box, CircularProgress, alpha, ButtonGroup } from "@mui/material";
import { MMIconButton } from "../IconButton";
import type {
  MacroDTO as GeneratedMacroDTO,
  ExecutionStateDTO as GeneratedExecutionStateDTO,
} from "@/generated/client";
import { theme } from "@/theme";
import PlayArrowIcon from "@mui/icons-material/PlayArrow";
import ShareIcon from "@mui/icons-material/Share";
import EditIcon from "@mui/icons-material/Edit";
import VisibilityIcon from "@mui/icons-material/Visibility";
import { executionProgress } from "../AppBar";
import DeleteIcon from "@mui/icons-material/Delete";
import StarIcon from "@mui/icons-material/Star";

type MacroDTO = GeneratedMacroDTO;
type executionState = GeneratedExecutionStateDTO;
export default function MacroCard(props: {
  macro: MacroDTO;
  execution?: executionState;
  onRequestExecution: (isDebug: boolean) => void;
  imageURL?: string;
  onView: () => void;
  onEdit: () => void;
  onShare: () => void;
  onDelete: () => void;
  onSetFavorite: () => void;
}) {
  const { macro, execution, imageURL } = props;
  const isMacroRunning = execution?.executionId == macro.uid;
  const percentage = executionProgress(props.execution);
  return (
    <Card
      sx={{
        position: "relative",

        "&:hover .play-overlay": {
          opacity: isMacroRunning ? 0 : 1,
        },

        "&:hover": {
          cursor: isMacroRunning ? "default" : "pointer",
        },
        width: "100%",
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
          <Typography variant="body1" sx={{ color: "text.primary" }}>
            {macro.executionUUIDs.length} step{macro.executionUUIDs.length != 1 && "s"}
          </Typography>
          <Typography variant="body2" sx={{ color: "text.secondary" }}>
            {macro.description}
          </Typography>
        </CardContent>
      </CardActionArea>

      <CardActions>
        <ButtonGroup>
          <MMIconButton
            disabled={true}
            onClick={props.onShare}
            icon={<ShareIcon />}
            tooltip={"Share this macro"}
          />
          <MMIconButton
            disabled={true}
            onClick={props.onEdit}
            icon={<EditIcon />}
            tooltip={"Edit this macro"}
          />
          <MMIconButton
            onClick={props.onView}
            icon={<VisibilityIcon />}
            tooltip={"Inspect this macro"}
          />
          <MMIconButton
            disabled={isMacroRunning}
            onClick={props.onDelete}
            icon={<DeleteIcon />}
            tooltip={"Delete this macro permanently"}
          />
          <MMIconButton
            disabled={true}
            onClick={props.onSetFavorite}
            icon={<StarIcon />}
            tooltip={"Pin this macro as favorite"}
          />
        </ButtonGroup>
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
            <CircularProgress
              key={-1}
              enableTrackSlot
              variant="determinate"
              color="secondary"
              size={65}
              value={100 - percentage}
              aria-label="Upload photos"
            />
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
