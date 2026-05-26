import { Dialog, DialogTitle, DialogContent, Stack, Box, Chip, Fab } from "@mui/material";
import CloseIcon from "@mui/icons-material/Close";
import CheckIcon from "@mui/icons-material/Check";
import { useState } from "react";

export type SelectDialogProps<T> = {
  open: boolean;
  items: T[];
  getId: (item: T) => string;
  getLabel: (item: T) => string;
  onClose: (selected: T[]) => void;
  isSingleSelect: boolean;
  title: string;
};

export function SelectDialog<T>({
  open,
  items,
  getId,
  getLabel,
  onClose,
  isSingleSelect = false,
  title,
}: SelectDialogProps<T>) {
  const [selected, setSelected] = useState<T[]>([]);

  return (
    <Dialog
      open={open}
      maxWidth="md"
      fullWidth
      PaperProps={{
        sx: {
          position: "relative",
          overflow: "visible",
        },
      }}
    >
      {/* Floating abort */}
      <Fab
        size="small"
        color="default"
        onClick={() => {
          setSelected([]);
          onClose([]);
        }}
        sx={{
          position: "absolute",
          top: 12,
          right: 12,
          zIndex: 10,
        }}
      >
        <CloseIcon />
      </Fab>

      <DialogTitle>{title}</DialogTitle>

      <DialogContent sx={{ pb: 10 }}>
        <Stack spacing={0}>
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
                      if (isSingleSelect) {
                        setSelected(isSelected ? [] : [item]);
                      } else {
                        setSelected((prev) =>
                          isSelected ? prev.filter((s) => getId(s) !== id) : [...prev, item],
                        );
                      }
                    }}
                  />
                </Box>
              );
            })}
        </Stack>
      </DialogContent>

      {/* Floating confirm */}
      <Fab
        color="primary"
        onClick={() => onClose(selected)}
        sx={{
          position: "absolute",
          bottom: 24,
          right: 24,
          zIndex: 10,
        }}
      >
        <CheckIcon />
      </Fab>
    </Dialog>
  );
}
