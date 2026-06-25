import { Button, Tooltip } from "@mui/material";

export type ButtonProps = {
  disabled?: boolean;
  onClick: () => void;
  icon: React.ReactNode;
  tooltip?: string;
  title?: string;
};
export const MMIconButton = ({ disabled, onClick, icon, tooltip, title }: ButtonProps) => {
  return (
    <Tooltip title={tooltip}>
      <Button
        size="large"
        variant="outlined"
        disabled={disabled}
        onClick={onClick}
        startIcon={icon}
        sx={{
          // center the content instead of "text button" alignment
          justifyContent: "center",

          // kill the startIcon layout offset
          "& .MuiButton-startIcon": {
            margin: 0,
          },
        }}
      >
        {title}
      </Button>
    </Tooltip>
  );
};
