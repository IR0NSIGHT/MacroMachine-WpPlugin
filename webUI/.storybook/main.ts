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

  async viteFinal(config, { configType }) {
    return {
      ...config,
      base: configType === "PRODUCTION" ? "/MacroMachine-WpPlugin/" : "/",
    };
  },
};

export default config;
