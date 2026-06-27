import type { Meta } from "@storybook/react-vite";
import defaultFilters from "@/mocks/data/defaultFilters.json";
import { ActionDTO } from "@/types/DTO";
import { Chip, Box } from "@mui/material";
import { namedMapping } from "./Filters";
import { getIconForValue } from "./FilterComponent";

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
          <Chip
            key={m.input}
            label={m.inputName + " icon:" + filter.input.iconName}
            variant="outlined"
            icon={getIconForValue(filter.input, m.input)}
          />
        ))}
      </Box>
    );
  },
};
