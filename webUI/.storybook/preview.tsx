import type { Preview } from "@storybook/react";
import React from "react";
import { ThemeProvider, CssBaseline, StyledEngineProvider } from "@mui/material";
import { theme } from "../src/theme";
// eslint-disable-next-line import/no-unassigned-import
import '@fontsource/ubuntu';

const preview: Preview = {
  async beforeAll() {
    if (typeof window !== "undefined") {
      const { worker } = await import("../src/mocks/browser");
      await worker.start({
        onUnhandledRequest: "bypass",
      });
    }
  },
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
