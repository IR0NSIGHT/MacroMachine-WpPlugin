import { createTheme } from "@mui/material/styles";

export const theme = createTheme({
  palette: {
    mode: "dark",
    primary: {
      main: "#8ec28d",
    },
    background: {
      default: "#0b0f17",
      paper: "#111827",
    },
  },
  shape: {
    borderRadius: 8,
  },
});