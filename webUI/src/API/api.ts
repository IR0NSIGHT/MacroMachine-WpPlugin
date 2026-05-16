export const API_BASE =
  import.meta.env.DEV && !import.meta.env.VITE_STORYBOOK ? "http://localhost:8080" : "";
