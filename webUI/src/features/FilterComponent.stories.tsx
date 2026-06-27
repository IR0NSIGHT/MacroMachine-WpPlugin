import type { Meta } from "@storybook/react-vite";
import defaultFilters from "@/mocks/data/defaultFilters.json";
import { ActionDTO } from "@/types/DTO";
import { Box } from "@mui/material";
import { namedMapping } from "./Filters";
import { ChipForValue } from "./FilterComponent";

const meta: Meta = {
  title: "Features/FilterComponent",
};

export default meta;

export const AnnotationFilterChips = {
  render: () => {
    const filter = defaultFilters[0] as ActionDTO;
    const mappings = namedMapping(filter);
    return (
      <Box sx={{ display: "flex", flexDirection: "row", gap: 1, flexWrap: "wrap", p: 2 }}>
        {mappings.map((m) => (
          <Box key={m.input} sx={{ display: "flex", flexDirection: "column", alignItems: "center" }}>
            <ChipForValue mapping={m} io={filter.input} />
            <Box sx={{ fontSize: 10, color: "text.secondary", mt: 0.5 }}>
              icon: {filter.input.iconByValue[m.input] || "(none)"}
            </Box>
          </Box>
        ))}
      </Box>
    );
  },
};
