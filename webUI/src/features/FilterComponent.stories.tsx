import type { Meta } from "@storybook/react-vite";
import defaultFilters from "@/mocks/data/defaultFilters.json";
import { ActionDTO } from "@/types/DTO";
import { Box, Typography } from "@mui/material";
import { namedMapping } from "./Filters";
import { ChipForValue } from "./FilterComponent";
import { InputOutputDTO } from "@/generated/client";

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

export const AllProviderTypeChips = {
  render: () => {
    const ios = allIos();
    return (
      <Box sx={{ display: "flex", flexDirection: "column", gap: 3, p: 2 }}>
        {ios.map((io) => (
          <Box key={io.type}>
            <Typography variant="h6" sx={{ mb: 1 }}>
              {io.displayName} ({io.type})
            </Typography>
            <Box sx={{ display: "flex", flexDirection: "row", gap: 1, flexWrap: "wrap" }}>
              {Array.from({ length: io.max - io.min + 1 }, (_, i) => io.min + i).map((v) => (
                <Box key={v} sx={{ display: "flex", flexDirection: "column", alignItems: "center" }}>
                  <ChipForValue
                    mapping={{ input: v, inputName: io.valueDisplayNames[v - io.min] || String(v) }}
                    io={io}
                  />
                  <Box sx={{ fontSize: 10, color: "text.secondary", mt: 0.5 }}>
                    icon: {io.iconByValue[v - io.min] || "(none)"}
                  </Box>
                </Box>
              ))}
            </Box>
          </Box>
        ))}
      </Box>
    );
  },
};
