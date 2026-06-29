import type { Meta, StoryObj } from "@storybook/react-vite";
import { FilterValueDialog } from "./FilterValueDialog";
import filters from "../mocks/data/defaultFilters.json";
import { StepItemType } from "@/features/Execution";
import { filterValuePass } from "@/features/Filters";
import { ActionDTO } from "@/types/DTO";

const meta: Meta<typeof FilterValueDialog> = {
  title: "Components/FilterValueDialog",
  component: FilterValueDialog,
};

export default meta;

type Story = StoryObj<typeof FilterValueDialog>;

export const Default: Story = {
  args: {
    open: true,
    action: { ...filters[0], active: true } as StepItemType,
    onClose: () => {},
    setAction: () => {},
    onViewItem: () => {},
  },
};

function preSelectValues(action: ActionDTO, indices: number[]): StepItemType {
  const mappedOutputs = [...action.mappedOutputs];
  for (const i of indices) {
    if (i >= 0 && i < mappedOutputs.length) {
      mappedOutputs[i] = filterValuePass;
    }
  }
  return { ...action, mappedOutputs, active: true } as StepItemType;
}

function preSelectRange(action: ActionDTO, start: number, end: number): StepItemType {
  const mappedOutputs = action.mappedOutputs.map((v, i) =>
    i >= start && i <= end ? filterValuePass : v,
  );
  return { ...action, mappedOutputs, active: true } as StepItemType;
}

const terrainFilter = (filters as ActionDTO[]).find((a) => a.name === "Filter by: Terrain")!;

export const Terrain: Story = {
  args: {
    open: true,
    action: preSelectValues(terrainFilter, [0, 1, 4, 5, 8]),
    onClose: () => {},
    setAction: () => {},
    onViewItem: () => {},
  },
};

const heightFilter = (filters as ActionDTO[]).find((a) => a.name === "Filter by: Height")!;

export const Height: Story = {
  args: {
    open: true,
    action: preSelectRange(heightFilter, 64, 191),
    onClose: () => {},
    setAction: () => {},
    onViewItem: () => {},
  },
};
