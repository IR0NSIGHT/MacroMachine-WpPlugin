import { createTheme } from "@mui/material/styles";

export const theme = createTheme({
  palette: {
    mode: "dark",
    primary: {
      main: "rgb(0, 173, 141)",
      light: "rgb(1, 144, 118)",
      dark: "#016a57",
      contrastText: "#ffffff",
    },
    secondary: {
      main: "#c823c9",
      light: "#d34fd3",
      dark: "#8c188c",
      contrastText: "#ffffff",
    },
    divider: "rgba(223, 223, 223, 1)",
    text: {
      primary: "#e2e2e2",
      secondary: "rgba(255,255,255,0.6)",
      disabled: "rgba(255,255,255,0.3)",
      //hint: 'rgba(255,255,255,0.5)',
    },
    background: {
      paper: "#424242",
      default: "#303030",
    },
    error: {
      main: "#f44336",
      light: "#f6685e",
      dark: "#aa2e25",
      contrastText: "#ffffff",
    },
    warning: {
      main: "#ff9800",
      light: "#ffac33",
      dark: "#b26a00",
      contrastText: "rgba(0,0,0,0.87)",
    },
    info: {
      main: "#2196f3",
      light: "#4dabf5",
      dark: "#1769aa",
      contrastText: "#ffffff",
    },
    success: {
      main: "#4caf50",
      light: "#6fbf73",
      dark: "#357a38",
      contrastText: "rgba(0,0,0,0.87)",
    },
  },
  typography: {
    fontFamily: "Ubuntu",
  },
  spacing: 8,
  shape: {
    borderRadius: 4,
  },
});
