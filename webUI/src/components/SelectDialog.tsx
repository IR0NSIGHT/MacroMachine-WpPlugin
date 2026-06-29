import {
  Dialog,
  DialogTitle,
  DialogContent,
  Box,
  ListItem,
  ListItemButton,
  ListItemText,
  Avatar,
  ListItemAvatar,
  Typography,
} from "@mui/material";
import CloseIcon from "@mui/icons-material/Close";
import CheckIcon from "@mui/icons-material/Check";
import { useEffect, useMemo, useState } from "react";
import InboxIcon from "@mui/icons-material/Inbox";
import { List } from "react-window";
import { Search, SearchIconWrapper, StyledInputBase } from "@/MacroGrid";
import SearchIcon from "@mui/icons-material/Search";
import { MMIconButton } from "./IconButton";

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
      sx={{
        bgcolor: "background.paper",
      }}
      PaperProps={{
        sx: {
          position: "relative",
          overflow: "visible",
        },
      }}
    >
      {/* Floating abort */}
      {onAbort && (
        <Box sx={{ position: "absolute", top: 12, right: 12, zIndex: 10 }}>
          <MMIconButton disabled={false} onClick={onAbort} icon={<CloseIcon />} tooltip={"Abort"} />
        </Box>
      )}
      <DialogTitle>{title}</DialogTitle>
      <DialogContent sx={{ pb: 10 }}> {children} </DialogContent>
      {/* Floating confirm */}
      {onConfirm && (
        <Box sx={{ position: "absolute", bottom: 24, right: 24, zIndex: 10 }}>
          <MMIconButton
            disabled={false}
            onClick={onConfirm}
            icon={<CheckIcon />}
            tooltip={"Confirm"}
          />
        </Box>
      )}
    </Dialog>
  );
}

type RowProps<T> = {
  index: number;
  style: React.CSSProperties;

  items: T[];
  getId: (item: T) => string;
  getLabel: (item: T) => string;
  isSelected: (id: string) => boolean;
  toggleItemSelected: (item: T) => void;
  getSecondaryText?: (item: T) => string;
  renderIcon?: (item: T) => React.ReactNode | string;
};

function renderRow<T>({
  index,
  style,
  items,
  getId,
  getLabel,
  isSelected,
  toggleItemSelected,
  getSecondaryText,
  renderIcon,
}: RowProps<T>) {
  const item = items[index];
  const id = getId(item);
  const selected = isSelected(id);

  const icon = renderIcon?.(item);
  return (
    <Box style={style}>
      <ListItem disablePadding color={selected ? "primary" : "default"}>
        <ListItemButton onClick={() => toggleItemSelected(item)} selected={selected}>
          {icon && (
            <ListItemAvatar>
              <Avatar src={typeof icon === "string" ? icon : undefined}>
                {typeof icon === "string" ? null : (icon ?? <InboxIcon />)}
              </Avatar>
            </ListItemAvatar>
          )}
          <ListItemText
            primary={getLabel(item)}
            secondary={getSecondaryText ? getSecondaryText(item) : undefined}
          />
        </ListItemButton>
      </ListItem>
    </Box>
  );
}

type IconImageProps = {
  src: string;
  alt?: string;
};

export function IconImage({ src, alt = "" }: IconImageProps) {
  return (
    <img
      src={src}
      alt={alt}
      width={24}
      height={24}
      style={{
        width: 24,
        height: 24,
        objectFit: "contain",
        display: "block",
      }}
    />
  );
}

export type SelectDialogProps<T> = {
  open: boolean;
  items?: T[];
  getId: (item: T) => string;
  getLabel: (item: T) => string;
  getSecondaryText?: (item: T) => string;
  onClose: (selected: T[], confirmed: boolean) => void;
  isSingleSelect: boolean;
  title: string;
  renderIcon?: (item: T) => React.ReactNode | string;
  selectedItems?: T[];
  toolbar?: React.ReactNode;
  compare?: (a: T, b: T) => number;
};

export function SelectDialog<T>({
  open,
  items,
  getId,
  getLabel,
  getSecondaryText,
  renderIcon,
  onClose,
  isSingleSelect = false,
  title,
  selectedItems,
  toolbar,
  compare,
}: SelectDialogProps<T>) {
  const [selected, setSelected] = useState<T[]>([]);

  useEffect(() => {
    if (open) {
      setSelected(selectedItems ?? []);
    }
  }, [open, selectedItems]);
  const [search, setSearch] = useState("");

  const isSelected = (id: string) => selected.some((s) => getId(s) === id);

  const toggleItem = (item: T) => {
    const id = getId(item);
    const selected = isSelected(id);

    if (isSingleSelect) {
      setSelected(selected ? [] : [item]);
    } else {
      setSelected((prev) => (selected ? prev.filter((s) => getId(s) !== id) : [...prev, item]));
    }
  };

  const abort = () => {
    onClose(selected, false);
  };

  const confirm = () => {
    onClose(selected, true);
  };

  const sortedFilteredItems = useMemo(() => {
    return items
      ?.filter(
        (item) => search === "" || getLabel(item).toLowerCase().includes(search.toLowerCase()),
      )
      .sort(
        compare ?? ((a, b) => getLabel(a).toLowerCase().localeCompare(getLabel(b).toLowerCase())),
      );
  }, [items, search, compare]);

  console.log(items?.map(getId));
  return (
    <PopupDialog open={open} onAbort={abort} onConfirm={confirm} title={title}>
      <Box display="flex" flexDirection="column" gap={2}>
        {toolbar}
        <Search>
          <SearchIconWrapper>
            <SearchIcon />
          </SearchIconWrapper>
          <StyledInputBase
            placeholder="Search…"
            value={search}
            onChange={(e) => setSearch(e.target.value)}
            inputProps={{ "aria-label": "search" }}
          />
        </Search>
        {!sortedFilteredItems || sortedFilteredItems.length === 0 ? (
          <Box>No items available</Box>
        ) : null}

        {sortedFilteredItems && sortedFilteredItems.length !== 0 && (
          <Box sx={{ width: "100%", height: 400, bgcolor: "background.paper" }}>
            <List
              rowHeight={getSecondaryText ? 72 : 56}
              rowCount={sortedFilteredItems.length}
              style={{}}
              rowProps={{
                items: sortedFilteredItems,
                getId,
                getLabel,
                isSelected,
                toggleItemSelected: toggleItem,
                getSecondaryText,
                renderIcon,
              }}
              overscanCount={5}
              rowComponent={renderRow}
            />
          </Box>
        )}
        <Typography>{selected.length} selected</Typography>
      </Box>
    </PopupDialog>
  );
}
