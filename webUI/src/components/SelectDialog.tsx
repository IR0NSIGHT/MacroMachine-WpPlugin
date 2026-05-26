import { Dialog, DialogTitle, DialogContent, Stack, Box, Chip } from "@mui/material";
import { useState } from "react";

type SelectDialogProps<T> = {
  open: boolean;
  items: T[];
  getId: (item: T) => string;
  getLabel: (item: T) => string;
  onClose: (selected: T[]) => void;
};

export function SelectDialog<T>({ open, items, getId, getLabel, onClose }: SelectDialogProps<T>) {
  const [selected, setSelected] = useState<T[]>([]);

  return (
    <Dialog open={open} onClose={() => onClose(selected)} maxWidth="md" fullWidth>
      <DialogTitle>Select Items</DialogTitle>

      <DialogContent>
        <Stack direction="column" spacing={1}>
          {[...items]
            .sort((a, b) => getLabel(a).localeCompare(getLabel(b)))
            .map((item) => {
              const id = getId(item);
              const isSelected = selected.some((s) => getId(s) === id);

              return (
                <Box key={id}>
                  <Chip
                    label={getLabel(item)}
                    size="small"
                    color={isSelected ? "primary" : "default"}
                    variant={isSelected ? "filled" : "outlined"}
                    onClick={() => {
                      setSelected((prev) =>
                        isSelected ? prev.filter((s) => getId(s) !== id) : [...prev, item],
                      );
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
