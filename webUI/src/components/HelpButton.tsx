import {  Popover, Typography } from "@mui/material";
import { useState } from "react";
import { MMIconButton } from "./IconButton";

export default function HelpButton({ explanation }: { explanation: String}) {
  const [anchorEl, setAnchorEl] = useState(null);

  const handleClick = (event: any) => {
    setAnchorEl(anchorEl ? null : event.currentTarget);
  };

  const open = Boolean(anchorEl);

  return (
    <>
      <MMIconButton
              onClick={handleClick}
              tooltip="Show help"
              title="?" icon={undefined}      />

      <Popover
        open={open}
        anchorEl={anchorEl}
        onClose={() => setAnchorEl(null)}
        anchorOrigin={{
          vertical: "bottom",
          horizontal: "left",
        }}
      >
        <Typography sx={{ p: 2, maxWidth: 300 }}>
          {explanation}
        </Typography>
      </Popover>
    </>
  )
}