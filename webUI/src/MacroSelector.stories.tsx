import type { Meta, StoryObj } from "@storybook/react-vite";
import { MacroSelector } from "./MacroSelector";

const meta: Meta<typeof MacroSelector> = {
  title: "Components/MacroSelector",
  component: MacroSelector,
};

export default meta;

type Story = StoryObj<typeof MacroSelector>;

const baseMacro = (id: number) => ({
  uid: `macro-${id}`,
  name: `Macro ${id}`,
  description: `Description for Macro ${id}`,
  executionUUIDs: [],
  activeActions: [],
});

export const Empty: Story = {
  args: {
    macros: [],
    selectedMacroId: "",
    onChange: (uid) => console.log("selected:", uid),
  },
};

export const OneMacro: Story = {
  args: {
    macros: [baseMacro(1)],
    selectedMacroId: "macro-1",
    onChange: (uid) => console.log("selected:", uid),
  },
};

export const ThreeMacros: Story = {
  args: {
    macros: [baseMacro(1), baseMacro(2), baseMacro(3)],
    selectedMacroId: "macro-2",
    onChange: (uid) => console.log("selected:", uid),
  },
};

export const ManyMacros: Story = {
  args: {
    macros: Array.from({ length: 75 }, (_, i) => baseMacro(i + 1)),
    selectedMacroId: "macro-10",
    onChange: (uid) => console.log("selected:", uid),
  },
};
