import { Typography, Stack, Chip } from "@mui/material";

import { ActionDTO, ActionType, InputDTO, isMacroDTO, OutputDTO, MacroDTO } from "@/types/DTO";
import { StepMacroType } from "@/features/Execution";
import { StepItemType } from "@/features/Execution";
import { runnableMacro } from "@/features/Execution";
import { PopupDialog } from "../SelectDialog";

type Props = {
  open: boolean;
  macro: runnableMacro | undefined;
  onClose: () => void;
  onViewItem: (item: MacroDTO | ActionDTO) => void;
};
const toSentence = (input: InputDTO, type: ActionType, output: OutputDTO) => {
  const does = (() => {
    switch (type as string) {
      case "DIVIDE":
        return "divides";
      case "INCREMENT":
        return "increments";
      case "LIMIT_TO":
        return "limits";
      case "MULTIPLY":
        return "multiplies";
      case "SET":
        return "sets";
      case "AT_LEAST":
        return "sets minimum";
      case "DECREMENT":
        return "subtracts";
      default:
        return type;
    }
  })();
  return input.displayName + " " + does + " " + output.displayName;
};

const DetailsItem = ({
  item,
  key,
  onClick,
}: {
  item: StepMacroType | StepItemType;
  key: number;
  onClick: () => void;
}) => {
  const details = isMacroDTO(item) ? "Macro" : toSentence(item.input, item.actionType, item.output);
  return (
    <Chip
      key={key}
      color={item.active ? (isMacroDTO(item) ? "primary" : "secondary") : "default"}
      label={item.name + ": " + details}
      size="small"
      onClick={onClick}
    />
  );
};

export function MacroDetailsDialog({ open, macro, onClose, onViewItem }: Props) {
  if (!macro) return null;
  return (
    <PopupDialog open={open} onAbort={onClose} title={macro.name}>
      <Stack spacing={2}>
        <Typography color="text.secondary">{macro.description}</Typography>

        <Typography variant="subtitle2">Macro ID</Typography>

        <Typography variant="body2" sx={{ fontFamily: "monospace" }}>
          {macro.uid}
        </Typography>

        <Typography variant="subtitle2">Steps</Typography>

        <Stack direction="column" spacing={1} flexWrap="wrap">
          {macro.steps.map((stepDTO, idx) => (
            <DetailsItem item={stepDTO} key={idx} onClick={() => onViewItem(stepDTO)} />
          ))}
        </Stack>
      </Stack>
    </PopupDialog>
  );
}
