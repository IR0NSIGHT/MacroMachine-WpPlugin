import { Meta, StoryObj } from "@storybook/react-vite";
import { LayerManager } from "./LayerManager";

const meta: Meta<typeof LayerManager> = {
  title: "Components/LayerManager",
  component: LayerManager,
};

export default meta;

export const None: StoryObj<typeof LayerManager> = {
  args: {
    layers: [],
  },
};

export const Some: StoryObj<typeof LayerManager> = {
  args: {
    layers: [
      {
        id: "1",
        name: "Layer 1",
        type: "NIBBLE",
        custom: false,
        usedMacros: [],
        existsInProject: true,
      },
      {
        id: "2",
        name: "Layer 2",
        type: "BIT",
        custom: true,
        usedMacros: ["Macro1", "Macro2"],
        existsInProject: false,
      },
      {
        id: "3",
        name: "Layer 3",
        type: "BYTE",
        custom: false,
        usedMacros: [],
        existsInProject: true,
      },
    ],
  },
};

export const Many: StoryObj<typeof LayerManager> = {
  args: {
    layers: Array.from({ length: 20 }, (_, i) => ({
      id: `${i + 1}`,
      name: `Layer ${i + 1}`,
      type: ["NIBBLE", "BIT", "BYTE"][i % 3] as "NIBBLE" | "BIT" | "BYTE",
      custom: i % 2 === 0,
      usedMacros: i % 4 === 0 ? [`Macro${i + 1}`] : [],
      existsInProject: i % 3 !== 0,
    })),
  },
};
