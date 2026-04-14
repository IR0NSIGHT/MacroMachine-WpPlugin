import type { Preview } from "@storybook/react";
import React from "react";
import { ThemeProvider, CssBaseline, StyledEngineProvider } from "@mui/material";
import { theme } from "../src/theme";

const preview: Preview = {
  decorators: [
    (Story) => (
      <StyledEngineProvider injectFirst>
        <ThemeProvider theme={theme}>
          <CssBaseline />
          <Story />
        </ThemeProvider>
      </StyledEngineProvider>
    ),
  ],
};

export default preview;