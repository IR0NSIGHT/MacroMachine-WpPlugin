import { Dialog, DialogTitle, DialogContent, Typography, Stack, Chip } from "@mui/material";

import { components } from "@/generated/api-types";
import { ActionDTO, ActionType, InputDTO, isMacroDTO, OutputDTO } from "@/types/DTO";

type MacroDTO = components["schemas"]["MacroDTO"];

type Props = {
  open: boolean;
  macro: (MacroDTO & { steps: (MacroDTO | ActionDTO)[] }) | null;
  onClose: () => void;
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

const DetailsItem = ({ item, key }: { item: MacroDTO | ActionDTO; key: number }) => {
  const details = isMacroDTO(item) ? "Macro" : toSentence(item.input, item.actionType, item.output);
  return <Chip key={key} label={item.name + ": " + details} size="small" />;
};

export function MacroDetailsDialog({ open, macro, onClose }: Props) {
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
            {macro.steps.map((actionDTP, idx) => (
              <DetailsItem item={actionDTP} key={idx} />
            ))}
          </Stack>
        </Stack>
      </DialogContent>
    </Dialog>
  );
}
