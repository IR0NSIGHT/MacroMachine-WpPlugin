import type { Meta, StoryObj } from "@storybook/react-vite";
import { Box, Typography } from "@mui/material";
import {
  SimpleFilterInlineEditor,
  RangeFilterInlineEditor,
  ApplyActionInlineEditor,
} from "./FilterComponent";
import defaultFilters from "@/mocks/data/defaultFilters.json";
import defaultApplyActions from "@/mocks/data/defaultApplyActions.json";
import { ActionDTO } from "@/types/DTO";
import { StepItemType } from "./Execution";
import { useState } from "react";

const filters = defaultFilters as ActionDTO[];
const applyActions = defaultApplyActions as ActionDTO[];

const simpleFilter = filters.find(
  (f) => f.output.type === "INTERMEDIATE_SELECTION" && f.input.discrete,
)!;
const rangeFilter = filters.find(
  (f) => f.output.type === "INTERMEDIATE_SELECTION" && !f.input.discrete,
)!;
const applyDiscrete = applyActions.find((a) => a.output.discrete)!;
const applyNonDiscrete = applyActions.find((a) => !a.output.discrete)!;

const meta: Meta = {
  title: "Features/FilterComponent/InlineEditors",
};

export default meta;

const editorStory = (label: string, item: ActionDTO, Editor: React.ComponentType<any>) => {
  const Story = () => {
    const [stepItem, setStepItem] = useState<StepItemType>({ ...item, active: true });
    return (
      <Box sx={{ p: 2, maxWidth: 600 }}>
        <Typography variant="h6" sx={{ mb: 2 }}>
          {label}
        </Typography>
        <Typography variant="body2" sx={{ mb: 2, color: "text.secondary" }}>
          {item.name} — input: {item.input.type}, output: {item.output.type}, discrete:{" "}
          {String(item.output.discrete ?? item.input.discrete)}
        </Typography>
        <Editor item={stepItem} setItem={setStepItem} openEditorFor={(_i: StepItemType) => {}} />
        <Typography variant="caption" sx={{ mt: 2, display: "block", color: "text.secondary" }}>
          mappingPointsY: {JSON.stringify(stepItem.mappingPointsY)}
          {stepItem.actionType && `, actionType: ${stepItem.actionType}`}
        </Typography>
      </Box>
    );
  };
  return { render: Story } as StoryObj;
};

export const SimpleFilter = editorStory(
  "SimpleFilterInlineEditor (discrete input)",
  simpleFilter,
  SimpleFilterInlineEditor,
);

export const RangeFilter = editorStory(
  "RangeFilterInlineEditor (non-discrete input)",
  rangeFilter,
  RangeFilterInlineEditor,
);

export const ApplyActionDiscrete = editorStory(
  "ApplyActionInlineEditor (discrete output)",
  applyDiscrete,
  ApplyActionInlineEditor,
);

export const ApplyActionNonDiscrete = editorStory(
  "ApplyActionInlineEditor (non-discrete output — shows action type dropdown)",
  applyNonDiscrete,
  ApplyActionInlineEditor,
);
