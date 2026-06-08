import { describe, expect, it } from "vitest";
import {
  allowedValues,
  forbiddenValues,
  invertFilter,
  invertFilterSinglePosition,
} from "./Filters";
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
        ioParameters: [],
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
        ioParameters: [],
      },
      actionType: "SET",
      name: "Filter: Inside Selection",
      description: "Default filter: block all blocks that are not in selection.",
      uid: "fcd62c5d-a279-4296-9b7e-2d7a678ccf70",
      mappingPointsX: [0, 1],
      mappingPointsY: [0, 2147483647],
      mappedInputs: [0, 1],
      mappedOutputs: [0, 2147483647],
    };
    const outsideSelection = invertFilter({
      ...insideSelectionFilter,
      active: true,
    });
    const pass = allowedValues(outsideSelection);
    expect(pass).toStrictEqual([
      {
        input: 0,
        output: 2147483647,
        inputName: "OFF",
        outputName: SKIP_DISPLAY_NAME,
      },
    ]);
  });

  it("can invert a single value in a filter", () => {
    const annotationFilter: ActionDTO = {
      input: {
        displayName: "Annotations",
        description: "Annotations",
        min: 0,
        max: 15,
        ignoreValue: 2147483647,
        valueDisplayNames: [
          "No annotation",
          "White",
          "Orange",
          "Magenta",
          "Light Blue",
          "Yellow",
          "Lime",
          "Pink",
          "Light Grey",
          "Cyan",
          "Purple",
          "Blue",
          "Brown",
          "Green",
          "Red",
          "Black",
        ],
        discrete: true,
        type: "ANNOTATION",
        ioParameters: [],
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
        ioParameters: [],
      },
      actionType: "SET",
      name: "Filter: Only On Annotations Cyan",
      description: "Default filter: block all blocks that are not cyan annotated.",
      uid: "3532c357-c74e-41b1-82ab-95a92c9f721e",
      mappingPointsX: [0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15],
      mappingPointsY: [0, 0, 0, 0, 0, 0, 0, 0, 0, 2147483647, 0, 0, 0, 0, 0, 0],
      mappedOutputs: [0, 0, 0, 0, 0, 0, 0, 0, 0, 2147483647, 0, 0, 0, 0, 0, 0],
      mappedInputs: [0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15],
    };

    expect(allowedValues(annotationFilter)).toStrictEqual([
      {
        input: 9,
        inputName: "Cyan",
        output: 2147483647,
        outputName: "Ignore",
      },
    ]);
    expect(forbiddenValues(annotationFilter).map((item) => item.inputName)).toStrictEqual([
      "No annotation",
      "White",
      "Orange",
      "Magenta",
      "Light Blue",
      "Yellow",
      "Lime",
      "Pink",
      "Light Grey",
      "Purple",
      "Blue",
      "Brown",
      "Green",
      "Red",
      "Black",
    ]);

    const invertAnnotationRed = invertFilterSinglePosition(annotationFilter, 14);
    expect(allowedValues(invertAnnotationRed)).toStrictEqual([
      {
        input: 9,
        inputName: "Cyan",
        output: 2147483647,
        outputName: "Ignore",
      },
      {
        input: 14,
        inputName: "Red",
        output: 2147483647,
        outputName: "Ignore",
      },
    ]);
    expect(forbiddenValues(invertAnnotationRed).map((item) => item.inputName)).toStrictEqual([
      "No annotation",
      "White",
      "Orange",
      "Magenta",
      "Light Blue",
      "Yellow",
      "Lime",
      "Pink",
      "Light Grey",
      "Purple",
      "Blue",
      "Brown",
      "Green",
      "Black",
    ]);

    const doubleInverted = invertFilterSinglePosition(invertAnnotationRed, 14);
    expect(doubleInverted).toStrictEqual(annotationFilter);
  });
});
