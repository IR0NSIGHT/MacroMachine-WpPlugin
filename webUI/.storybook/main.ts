// .storybook/main.ts

const config = {
  framework: {
    name: "@storybook/react-vite",
  },

  stories: ["../src/**/*.stories.tsx"],

  addons: [],

  env: (config: Record<string, string>) => ({
    ...config,
    VITE_STORYBOOK: "true",
  }),
};

export default config;
