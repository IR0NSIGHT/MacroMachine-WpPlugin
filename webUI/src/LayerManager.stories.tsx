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
        description: "",
        dataSize: "NIBBLE",
        priority: 0,
        discrete: false,
        presentInProject: false,
        macrosUsingLayer: ["Macro 1", "Macro 2", "Macro 3"],
      },
      {
        id: "2",
        name: "Layer 2",
        type: "BIT",
        custom: true,
        description: "",
        dataSize: "NIBBLE",
        priority: 0,
        discrete: false,
        presentInProject: false,
        macrosUsingLayer: ["Macro 1", "Macro 2"],
      },
      {
        id: "3",
        name: "Layer 3",
        type: "BYTE",
        custom: false,
        description: "",
        dataSize: "NIBBLE",
        priority: 0,
        discrete: false,
        presentInProject: false,
        macrosUsingLayer: [],
      },
    ],
  },
};

export const Many: StoryObj<typeof LayerManager> = {
  args: {
    layers: Array.from({ length: 700 }, (_, i) => ({
      id: `${i + 1}`,
      name: `Layer ${i + 1}`,
      type: [
        "Custom Objects",
        "Plants",
        "CustomAnnotations",
        "Caves",
        "Underground Pockets",
        "Combined Layer",
        "Ground Cover",
        "Cave/Tunnel",
        "Floating Dimension",
      ][i % 9],
      custom: i % 2 === 0,
      description: "",
      dataSize: ["NIBBLE", "BIT", "BYTE"][i % 3] as "NIBBLE" | "BIT" | "BYTE",
      priority: 0,
      discrete: Math.random() < 0.5,
      presentInProject: Math.random() < 0.5,
      macrosUsingLayer: Array.from(
        { length: Math.floor(Math.random() * 5) },
        (_, j) => `Macro ${j + 1}`,
      ),
    })),
  },
};
