import { describe, expect, it } from "vitest";
import { allowedValues, invertFilter } from "./Filters";
import { ActionDTO } from "@/types/DTO";
import { SKIP_DISPLAY_NAME } from "./InputOutput";

describe("invert filters", () => {
  it("can invert a selection binary filter", () => {
    const insideSelectionFilter: ActionDTO = {
      input: {
        displayName: "Selection",
        description: "Worldpainter Default Selection Layer (yellow)",
        min: 0,
        max: 1,
        ignoreValue: 2147483647,
        valueDisplayNames: ["OFF", "ON"],
        discrete: true,
        type: "SELECTION",
      },
      output: {
        displayName: "Action Filter",
        description: "only blocks that pass this filter will be used in following actions.",
        min: 0,
        max: 1,
        ignoreValue: 2147483647,
        valueDisplayNames: ["BLOCK (0)", "PASS (1)"],
        discrete: true,
        type: "INTERMEDIATE_SELECTION",
      },
      actionType: "SET",
      name: "Filter: Inside Selection",
      description: "Default filter: block all blocks that are not in selection.",
      uid: "fcd62c5d-a279-4296-9b7e-2d7a678ccf70",
      mappingPointsX: [0, 1],
      mappingPointsY: [0, 2147483647],
      mappedOutputs: [0, 2147483647],
    };
    const outsideSelection = invertFilter({ ...insideSelectionFilter, active: true });
    const pass = allowedValues(outsideSelection);
    expect(pass).toStrictEqual([
      { input: 0, output: 2147483647, inputName: "OFF", outputName: SKIP_DISPLAY_NAME },
    ]);
  });
});
