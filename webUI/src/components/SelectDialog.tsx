import { Dialog, DialogTitle, DialogContent, Stack, Box, Chip, Fab } from "@mui/material";
import CloseIcon from "@mui/icons-material/Close";
import CheckIcon from "@mui/icons-material/Check";
import { useState } from "react";

export function PopupDialog({
  open,
  onAbort,
  onConfirm,
  title,
  children,
}: {
  open: boolean;
  onAbort?: () => void | undefined;
  onConfirm?: () => void | undefined;
  title: React.ReactNode;
  children: React.ReactNode;
}) {
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
      {onAbort && (
        <Fab
          size="small"
          color="default"
          onClick={onAbort}
          sx={{
            position: "absolute",
            top: 12,
            right: 12,
            zIndex: 10,
          }}
        >
          <CloseIcon />
        </Fab>
      )}
      <DialogTitle>{title}</DialogTitle>
      <DialogContent sx={{ pb: 10 }}> {children} </DialogContent>
      {/* Floating confirm */}
      {onConfirm && (
        <Fab
          color="primary"
          onClick={onConfirm}
          sx={{
            position: "absolute",
            bottom: 24,
            right: 24,
            zIndex: 10,
          }}
        >
          <CheckIcon />
        </Fab>
      )}
    </Dialog>
  );
}

export type SelectDialogProps<T> = {
  open: boolean;
  items?: T[];
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

  const toggleItem = (item: T) => {
    const id = getId(item);
    const isSelected = selected.some((s) => getId(s) === id);

    if (isSingleSelect) {
      setSelected(isSelected ? [] : [item]);
    } else {
      setSelected((prev) => (isSelected ? prev.filter((s) => getId(s) !== id) : [...prev, item]));
    }
  };

  const abort = () => {
    setSelected([]);
    onClose([]);
  };

  const confirm = () => {
    onClose(selected);
  };

  return (
    <PopupDialog open={open} onAbort={abort} onConfirm={confirm} title={title}>
      {!items || items.length === 0 ? <Box>No items available</Box> : null}

      {items && (
        <Stack spacing={0}>
          {items
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
                    onClick={() => toggleItem(item)}
                  />
                </Box>
              );
            })}
        </Stack>
      )}
    </PopupDialog>
  );
}
