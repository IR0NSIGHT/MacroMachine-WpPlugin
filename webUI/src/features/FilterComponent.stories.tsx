import type { Meta, StoryObj } from "@storybook/react-vite";
import defaultFilters from "@/mocks/data/defaultFilters.json";
import { ActionDTO } from "@/types/DTO";
import { Box, Typography } from "@mui/material";
import { ChipForValue } from "./FilterComponent";
import { InputOutputDTO, InputOutputDTOTypeEnum } from "@/generated/client";

const meta: Meta = {
  title: "Features/FilterComponent",
};

export default meta;

const allIos = () => {
  const seen = new Set<string>();
  const ios: InputOutputDTO[] = [];
  for (const f of defaultFilters as ActionDTO[]) {
    for (const io of [f.input, f.output]) {
      if (!seen.has(io.type)) {
        seen.add(io.type);
        ios.push(io);
      }
    }
  }
  return ios;
};

const iosByType: Record<string, InputOutputDTO> = {};
for (const io of allIos()) {
  iosByType[io.type] = io;
}

const mkStory = (type: string): StoryObj => ({
  render: () => {
    const io = iosByType[type];
    return (
      <Box sx={{ display: "flex", flexDirection: "column", gap: 3, p: 2 }}>
        <Typography variant="h6" sx={{ mb: 1 }}>
          {io.displayName} ({io.type})
        </Typography>
        <Box sx={{ display: "flex", flexDirection: "row", gap: 1, flexWrap: "wrap" }}>
          {Array.from({ length: io.max - io.min + 1 }, (_, i) => io.min + i).map((v) => (
            <Box key={v} sx={{ display: "flex", flexDirection: "column", alignItems: "center" }}>
              <ChipForValue
                mapping={{
                  input: v,
                  inputName: io.valueDisplayNames?.[v - io.min] || String(v),
                  output: 0,
                  outputName: "",
                }}
                io={io}
              />
              <Box sx={{ fontSize: 10, color: "text.secondary", mt: 0.5 }}>
                icon: {io.iconByValue?.[v - io.min] || "(none)"}
              </Box>
            </Box>
          ))}
        </Box>
      </Box>
    );
  },
});

export const Height = mkStory(InputOutputDTOTypeEnum.Height);
export const Slope = mkStory(InputOutputDTOTypeEnum.Slope);
export const Annotation = mkStory(InputOutputDTOTypeEnum.Annotation);
export const BinaryLayer = mkStory(InputOutputDTOTypeEnum.BinaryLayer);
export const BlockDirection = mkStory(InputOutputDTOTypeEnum.BlockDirection);
export const DistanceToEdge = mkStory(InputOutputDTOTypeEnum.DistanceToEdge);
export const IntermediateSelection = mkStory(InputOutputDTOTypeEnum.IntermediateSelection);
export const NibbleLayer = mkStory(InputOutputDTOTypeEnum.NibbleLayer);
export const PerlinNoise = mkStory(InputOutputDTOTypeEnum.PerlinNoise);
export const RandomNoise = mkStory(InputOutputDTOTypeEnum.RandomNoise);
export const Selection = mkStory(InputOutputDTOTypeEnum.Selection);
export const Shadow = mkStory(InputOutputDTOTypeEnum.Shadow);
export const Terrain = mkStory(InputOutputDTOTypeEnum.Terrain);
export const VanillaBiome = mkStory(InputOutputDTOTypeEnum.VanillaBiome);
export const VoronoiNoise = mkStory(InputOutputDTOTypeEnum.VoronoiNoise);
export const WaterDepth = mkStory(InputOutputDTOTypeEnum.WaterDepth);
export const WaterHeight = mkStory(InputOutputDTOTypeEnum.WaterHeight);
