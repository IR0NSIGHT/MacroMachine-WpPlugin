// .storybook/main.ts

const config = {
  framework: {
    name: "@storybook/react-vite",
  },
  staticDirs: ["../public"],

  stories: ["../src/**/*.stories.tsx"],

  addons: [],

  env: (config: Record<string, string>) => ({
    ...config,
    VITE_STORYBOOK: "true",
  }),
};

export default config;
