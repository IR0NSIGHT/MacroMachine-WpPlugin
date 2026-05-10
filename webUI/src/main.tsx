import React from "react";
import ReactDOM from "react-dom/client";
import { MacroGrid } from "./App";
import "./index.css";
import { ThemeProvider } from "@emotion/react";
import { theme } from "./theme";
import { CssBaseline } from "@mui/material";
import AppShell from "./App";

ReactDOM.createRoot(document.getElementById("root") as HTMLElement).render(
  <React.StrictMode>
    <ThemeProvider theme={theme}>
      <CssBaseline />
      <AppShell>
        <MacroGrid />
      </AppShell>
    </ThemeProvider>
  </React.StrictMode>,
);
