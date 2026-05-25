import { ActionDTO } from "@/types/DTO";
import { Dialog, DialogTitle, DialogContent, Stack, Box, Chip } from "@mui/material";
import { useState } from "react";

type FilterEditorProps = {
  open: boolean;
  actions: ActionDTO[];
  onClose: (item: ActionDTO[]) => void;
};

export function ActionSelectDialog({ open, actions, onClose }: FilterEditorProps) {
  const [selected, setSelected] = useState<ActionDTO[]>([]);

  return (
    <Dialog
      open={open}
      onClose={() => {
        onClose(selected);
      }}
      maxWidth="md"
      fullWidth
    >
      <DialogTitle>Select Items</DialogTitle>

      <DialogContent>
        <Stack direction="column" spacing={1} flexWrap="wrap">
          {actions
            .sort((a, b) => a.name.localeCompare(b.name))
            .map((action) => {
              const isSelected = selected.some((s) => s.uid === action.uid);
              return (
                <Box key={action.uid}>
                  <Chip
                    label={action.name}
                    size="small"
                    color={isSelected ? "primary" : "default"}
                    variant={isSelected ? "filled" : "outlined"}
                    onClick={() => {
                      if (isSelected) setSelected(selected.filter((s) => s.uid !== action.uid));
                      else setSelected([...selected, action]);
                    }}
                  />
                </Box>
              );
            })}
        </Stack>
      </DialogContent>
    </Dialog>
  );
}
