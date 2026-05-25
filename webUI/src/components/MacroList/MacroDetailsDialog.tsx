import { Dialog, DialogTitle, DialogContent, Typography, Stack, Chip } from "@mui/material";

import { ActionDTO, ActionType, InputDTO, isMacroDTO, OutputDTO, MacroDTO } from "@/types/DTO";

type Props = {
  open: boolean;
  macro: (MacroDTO & { steps: (MacroDTO | ActionDTO)[] }) | null;
  onClose: () => void;
  onViewItem: (item: MacroDTO | ActionDTO) => void;
};
//FIXME: active state of each step must be displayed
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
  item: MacroDTO | ActionDTO;
  key: number;
  onClick: () => void;
}) => {
  const details = isMacroDTO(item) ? "Macro" : toSentence(item.input, item.actionType, item.output);
  return <Chip key={key} label={item.name + ": " + details} size="small" onClick={onClick} />;
};

export function MacroDetailsDialog({ open, macro, onClose, onViewItem }: Props) {
  if (!macro) return null;

  return (
    <Dialog open={open} onClose={onClose} maxWidth="md" fullWidth>
      <DialogTitle>{macro.name}</DialogTitle>

      <DialogContent>
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
      </DialogContent>
    </Dialog>
  );
}
