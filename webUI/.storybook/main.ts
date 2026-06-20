import type { StorybookConfig } from "@storybook/react-vite";

const config: StorybookConfig = {
  framework: {
    name: "@storybook/react-vite",
    options: {},
  },

  stories: ["../src/**/*.stories.tsx"],

  addons: [],

  staticDirs: ["../public"],

  env: (config) => ({
    ...config,
    VITE_STORYBOOK: "true",
  }),

  async viteFinal(config) {
    const isProd = process.env.STORYBOOK_BUILD === "true";

    return {
      ...config,
      base: isProd ? "/MacroMachine-WpPlugin/" : "/",
    };
  },
};

export default config;
