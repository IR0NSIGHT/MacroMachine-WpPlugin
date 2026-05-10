// src/utils/sum.test.ts
import { describe, it, expect } from "vitest";
import {
  Segments,
  splitAt,
  mergeSegments,
  areSegmentsValid,
  shiftSegment,
  buildSegmentsFromAction,
  Segment,
  getMappingPointArrayFromSegments,
} from "./Segment";
import { MappingPointDTO, MMAction } from "@/types/MMAction";
import { alwaysIO, annotationsIO, forestIO, heightIO } from "@/mock/dummyIOs";

describe("segment splitting", () => {
  it("will not split at the border of an existing segment", () => {
    const segments: Segments = [
      { start: 0, end: 10, value: { displayName: "A", numericValue: 3 } },
      { start: 11, end: 20, value: { displayName: "B", numericValue: 4 } },
    ];
    const newSegments = splitAt(segments, 10);
    expect(newSegments).toEqual(segments);
    expect(areSegmentsValid(newSegments, 0, 20)).toBe(true);
  });

  it("splits a single segment at a given position", () => {
    const segments: Segments = [
      { start: 0, end: 10, value: { displayName: "A", numericValue: 3 } },
    ];
    const newSegments = splitAt(segments, 5);
    expect(newSegments).toEqual([
      { start: 0, end: 5, value: { displayName: "A", numericValue: 3 } },
      { start: 6, end: 10, value: { displayName: "A", numericValue: 3 } },
    ]);
    expect(areSegmentsValid(newSegments, 0, 10)).toBe(true);
  });

  it("does not split at the border of a segment", () => {
    const segments: Segments = [
      { start: 0, end: 10, value: { displayName: "A", numericValue: 3 } },
    ];
    const newSegments = splitAt(segments, 0);
    expect(newSegments).toEqual(segments);
    const newSegments2 = splitAt(segments, 10);
    expect(newSegments2).toEqual(segments);
  });

  it("does not split segments that are to small", () => {
    {
      const segmentsOneWide: Segments = [
        { start: 0, end: 1, value: { displayName: "A", numericValue: 3 } },
      ];
      expect(areSegmentsValid(segmentsOneWide, 0, 1)).toBe(true);
      const newSegments = splitAt(segmentsOneWide, 0);
      expect(newSegments).toEqual(segmentsOneWide);
      const newSegments2 = splitAt(segmentsOneWide, 1);
      expect(newSegments2).toEqual(segmentsOneWide);
    }

    {
      const segmentsTwoWide: Segments = [
        { start: 0, end: 2, value: { displayName: "A", numericValue: 3 } },
      ];
      expect(areSegmentsValid(segmentsTwoWide, 0, 2)).toBe(true);
      const newSegments3 = splitAt(segmentsTwoWide, 0);
      expect(newSegments3).toEqual(segmentsTwoWide);
      const newSegments4 = splitAt(segmentsTwoWide, 1);
      expect(newSegments4).toEqual(segmentsTwoWide);
    }
  });

  it("does split minimal size segment correctly", () => {
    const segments: Segments = [{ start: 0, end: 3, value: { displayName: "A", numericValue: 3 } }];
    const newSegments = splitAt(segments, 1);
    expect(newSegments).toEqual([
      { start: 0, end: 1, value: { displayName: "A", numericValue: 3 } },
      { start: 2, end: 3, value: { displayName: "A", numericValue: 3 } },
    ]);
    expect(areSegmentsValid(newSegments, 0, 3)).toBe(true);
  });

  it("does not split if position is outside the segment", () => {
    const segments: Segments = [
      { start: 0, end: 10, value: { displayName: "A", numericValue: 3 } },
    ];
    const newSegments = splitAt(segments, 15);
    expect(newSegments).toEqual(segments);

    const newSegments2 = splitAt(segments, -5);
    expect(newSegments2).toEqual(segments);
  });

  it("splits multiple segments correctly", () => {
    const segments: Segments = [
      { start: 0, end: 10, value: { displayName: "A", numericValue: 3 } },
      { start: 11, end: 20, value: { displayName: "B", numericValue: 5 } },
      { start: 21, end: 30, value: { displayName: "C", numericValue: 7 } },
    ];
    expect(areSegmentsValid(segments, 0, 30)).toBe(true);
    const newSegments = splitAt(segments, 15);
    expect(newSegments).toEqual([
      { start: 0, end: 10, value: { displayName: "A", numericValue: 3 } },
      { start: 11, end: 15, value: { displayName: "B", numericValue: 5 } },
      { start: 16, end: 20, value: { displayName: "B", numericValue: 5 } },
      { start: 21, end: 30, value: { displayName: "C", numericValue: 7 } },
    ]);
    expect(areSegmentsValid(newSegments, 0, 30)).toBe(true);
  });
});

describe("segment merging", () => {
  it("merges two adjacent segments", () => {
    const segments: Segments = [
      { start: 0, end: 5, value: { displayName: "A", numericValue: 3 } },
      { start: 6, end: 10, value: { displayName: "B", numericValue: 4 } },
      { start: 11, end: 15, value: { displayName: "C", numericValue: 5 } },
    ];
    {
      const leftEatsRight = mergeSegments(segments, 5);
      expect(leftEatsRight).toEqual([
        { start: 0, end: 10, value: { displayName: "A", numericValue: 3 } },
        { start: 11, end: 15, value: { displayName: "C", numericValue: 5 } },
      ]);
      expect(areSegmentsValid(leftEatsRight, 0, 15)).toBe(true);
    }
    {
      const rightEatsLeft = mergeSegments(segments, 6);
      expect(rightEatsLeft).toEqual([
        { start: 0, end: 10, value: { displayName: "B", numericValue: 4 } },
        { start: 11, end: 15, value: { displayName: "C", numericValue: 5 } },
      ]);
      expect(areSegmentsValid(rightEatsLeft, 0, 15)).toBe(true);
    }
  });
  it("does not merge out-ouf-bounds positions", () => {
    const segments: Segments = [
      { start: 0, end: 5, value: { displayName: "A", numericValue: 3 } },
      { start: 6, end: 10, value: { displayName: "B", numericValue: 5 } },
    ];
    expect(areSegmentsValid(segments, 0, 10)).toBe(true);

    const newSegments = mergeSegments(segments, 400);
    expect(newSegments).toEqual(segments);
  });
  it("does not merge if there is no right neighbour", () => {
    const segments: Segments = [
      { start: 0, end: 5, value: { displayName: "A", numericValue: 3 } },
      { start: 6, end: 10, value: { displayName: "B", numericValue: 5 } },
    ];
    expect(areSegmentsValid(segments, 0, 10)).toBe(true);

    const newSegments = mergeSegments(segments, 10);
    expect(newSegments).toEqual(segments);
  });
  it("does not merge if value is not a border between segments", () => {
    const segments: Segments = [
      { start: 0, end: 5, value: { displayName: "A", numericValue: 3 } },
      { start: 6, end: 10, value: { displayName: "B", numericValue: 5 } },
    ];
    expect(areSegmentsValid(segments, 0, 10)).toBe(true);

    const newSegments = mergeSegments(segments, 4);
    expect(newSegments).toEqual(segments);
  });

  it("does not merge in a way that the whole range would shrink", () => {
    const segments: Segments = [
      { start: 0, end: 5, value: { displayName: "A", numericValue: 3 } },
      { start: 6, end: 10, value: { displayName: "B", numericValue: 5 } },
    ];
    expect(areSegmentsValid(segments, 0, 10)).toBe(true);
    // try to merge away the first segment by merging at the start of the range
    const newSegments = mergeSegments(segments, 0);
    expect(newSegments).toEqual([
      { start: 0, end: 5, value: { displayName: "A", numericValue: 3 } },
      { start: 6, end: 10, value: { displayName: "B", numericValue: 5 } },
    ]);
    expect(areSegmentsValid(newSegments, 0, 10)).toBe(true);
  });
});

describe("segment validation", () => {
  it("validates that segments fill the complete range", () => {
    const segmentsOkay: Segments = [
      { start: 0, end: 5, value: { displayName: "A", numericValue: 3 } },
      { start: 6, end: 10, value: { displayName: "B", numericValue: 5 } },
    ];
    expect(areSegmentsValid(segmentsOkay, 0, 10)).toBe(true);

    const segmentsWrong1: Segments = [
      { start: 0, end: 5, value: { displayName: "A", numericValue: 3 } },
      { start: 6, end: 8, value: { displayName: "B", numericValue: 5 } },
    ];
    expect(areSegmentsValid(segmentsWrong1, 0, 10)).toBe(false);

    const noSegments: Segments = [];
    expect(areSegmentsValid(noSegments, 0, 10)).toBe(false);

    const singleSegment: Segments = [
      { start: 0, end: 10, value: { displayName: "A", numericValue: 3 } },
    ];
    expect(areSegmentsValid(singleSegment, 0, 10)).toBe(true);
  });

  it("validates that segments do not overlap", () => {
    const segments: Segments = [
      { start: 0, end: 5, value: { displayName: "A", numericValue: 3 } },
      { start: 4, end: 10, value: { displayName: "B", numericValue: 5 } },
      { start: 11, end: 15, value: { displayName: "C", numericValue: 7 } },
    ];
    expect(areSegmentsValid(segments, 0, 10)).toBe(false);

    const segments2: Segments = [
      { start: 0, end: 5, value: { displayName: "A", numericValue: 3 } },
      { start: 5, end: 10, value: { displayName: "B", numericValue: 5 } },
    ];
    expect(areSegmentsValid(segments2, 0, 10)).toBe(false);
  });

  it("validates that segments have start <= end", () => {
    const segments: Segments = [
      { start: 0, end: 5, value: { displayName: "A", numericValue: 3 } },
      { start: 10, end: 6, value: { displayName: "B", numericValue: 5 } },
    ];
    expect(areSegmentsValid(segments, 0, 10)).toBe(false);

    const segmentsWrongOrder: Segments = [
      { start: 6, end: 10, value: { displayName: "B", numericValue: 5 } },
      { start: 0, end: 5, value: { displayName: "A", numericValue: 3 } },
    ];
    expect(areSegmentsValid(segmentsWrongOrder, 0, 10)).toBe(false);
  });

  it("validates correct segments", () => {
    const segments: Segments = [
      { start: 0, end: 5, value: { displayName: "A", numericValue: 3 } },
      { start: 6, end: 10, value: { displayName: "B", numericValue: 5 } },
    ];
    expect(areSegmentsValid(segments, 0, 10)).toBe(true);
  });
});

describe("segment shifting", () => {
  it("can not shift a single-segment-range", () => {
    const segments: Segments = [
      { start: 10, end: 15, value: { displayName: "A", numericValue: 3 } },
    ];
    const expandRight = shiftSegment(segments, 10, 10, 16);
    expect(expandRight).toEqual(segments);

    const expandLeft = shiftSegment(segments, 10, 9, 15);
    expect(expandLeft).toEqual(segments);

    const expandBoth = shiftSegment(segments, 10, 9, 16);
    expect(expandBoth).toEqual(segments);

    const shrinkLeft = shiftSegment(segments, 10, 11, 15);
    expect(shrinkLeft).toEqual(segments);

    const shrinkRight = shiftSegment(segments, 10, 10, 14);
    expect(shrinkRight).toEqual(segments);

    const shrinkBoth = shiftSegment(segments, 10, 11, 14);
    expect(shrinkBoth).toEqual(segments);
  });

  it("does not create holes in the range, adjusts neighbours", () => {
    const segments: Segments = [
      { start: 0, end: 5, value: { displayName: "A", numericValue: 3 } },
      { start: 6, end: 10, value: { displayName: "B", numericValue: 5 } },
    ];
    const shift = shiftSegment(segments, 0, 0, 2);
    expect(shift).toEqual([
      { start: 0, end: 2, value: { displayName: "A", numericValue: 3 } },
      { start: 3, end: 10, value: { displayName: "B", numericValue: 5 } },
    ]);
  });

  it("does not allow shifting so that a segment has a width of 0 or less", () => {
    const segments: Segments = [
      { start: 0, end: 5, value: { displayName: "A", numericValue: 3 } },
      { start: 6, end: 10, value: { displayName: "B", numericValue: 5 } },
    ];
    const shift = shiftSegment(segments, 0, 0, 0);
    expect(shift).toEqual([
      { start: 0, end: 1, value: { displayName: "A", numericValue: 3 } },
      { start: 2, end: 10, value: { displayName: "B", numericValue: 5 } },
    ]); // segment 1 would have width 0, so shift is not applied
  });

  it("does not shift if value is not a segment border", () => {
    const segments: Segments = [
      { start: 0, end: 5, value: { displayName: "A", numericValue: 3 } },
      { start: 6, end: 10, value: { displayName: "B", numericValue: 5 } },
    ];
    const shift = shiftSegment(segments, 3, 0, 0);
    expect(shift).toEqual([
      { start: 0, end: 5, value: { displayName: "A", numericValue: 3 } },
      { start: 6, end: 10, value: { displayName: "B", numericValue: 5 } },
    ]); // segment 1 would have width 0, so shift is not applied
  });

  it("can shift a simple filteraction segment range", () => {
    const segments = [
      { start: 0, end: 0, value: { displayName: "BLOCK (0)", numericValue: 0 } },
      { start: 1, end: 100, value: { displayName: "PASS (1)", numericValue: 1 } },
    ];
    const newSegments = shiftSegment(segments, 0, 0, 5);
    expect(newSegments).toEqual([
      { start: 0, end: 5, value: { displayName: "BLOCK (0)", numericValue: 0 } },
      { start: 6, end: 100, value: { displayName: "PASS (1)", numericValue: 1 } },
    ]);
  });
});

describe("convert action to segments", () => {
  it("can handle empty mappingpoint array", () => {
    const action: MMAction = {
      name: "Test Action",
      description: "This is a test action",
      uid: "test-action",
      input: forestIO,
      output: annotationsIO,
      actionType: "increments",
      mappedInputs: [
        /** irrelephant */
      ],
      mappedOutputs: [
        /** irrelephant */
      ],
      mappingPoints: [],
    };
    const segments = buildSegmentsFromAction(action);
    expect(segments).toEqual([
      {
        start: forestIO.min,
        end: forestIO.max,
        value: annotationsIO.values.find((v) => v.numericValue == annotationsIO.ignoreValue),
      },
    ]);
  });

  it("can convert actions with multiple output groups/ranges", () => {
    {
      const action: MMAction = {
        name: "Test Action",
        description: "This is a test action",
        uid: "test-action",
        input: forestIO,
        output: annotationsIO,
        actionType: "increments",
        mappedInputs: forestIO.values
          .filter((v) => v.numericValue !== forestIO.ignoreValue)
          .map((v) => v.numericValue),
        mappedOutputs: [4, 4, 4, 4, 3, 3, 3, 3, 15, 15, 15, 15, 15, 15, 15, 15],
        mappingPoints: [
          { x: 3, y: 4 },
          { x: 4, y: 3 },
          { x: 8, y: 15 },
        ], // FIXME
      };
      const segments = buildSegmentsFromAction(action);
      expect(segments).toEqual([
        { start: 0, end: 3, value: annotationsIO.values.find((v) => v.numericValue == 4) },
        { start: 4, end: 7, value: annotationsIO.values.find((v) => v.numericValue == 3) },
        { start: 8, end: 15, value: annotationsIO.values.find((v) => v.numericValue == 15) },
      ]);
    }

    {
      const action: MMAction = {
        name: "Test Action",
        description: "This is a test action",
        uid: "test-action",
        input: forestIO,
        output: annotationsIO,
        actionType: "increments",
        mappedInputs: forestIO.values
          .filter((v) => v.numericValue !== forestIO.ignoreValue)
          .map((v) => v.numericValue),
        mappedOutputs: [4, 4, 4, 4, 3, 5, 4, 4, 7, 7, 7, 7, 0, 0, 0, 15],
        mappingPoints: [
          { x: 0, y: 4 },
          { x: 4, y: 3 },
          { x: 5, y: 5 },
          { x: 7, y: 4 },
          { x: 8, y: 7 },
          { x: 12, y: 0 },
          { x: 15, y: 15 },
        ], // FIXME
      };
      const segments = buildSegmentsFromAction(action);
      expect(segments).toEqual([
        { start: 0, end: 3, value: annotationsIO.values.find((v) => v.numericValue == 4) },
        { start: 4, end: 4, value: annotationsIO.values.find((v) => v.numericValue == 3) },
        { start: 5, end: 6, value: annotationsIO.values.find((v) => v.numericValue == 5) },
        { start: 7, end: 7, value: annotationsIO.values.find((v) => v.numericValue == 4) },
        { start: 8, end: 11, value: annotationsIO.values.find((v) => v.numericValue == 7) },
        { start: 12, end: 14, value: annotationsIO.values.find((v) => v.numericValue == 0) },
        { start: 15, end: 15, value: annotationsIO.values.find((v) => v.numericValue == 15) },
      ]);
    }

    {
      const action: MMAction = {
        name: "Test Action",
        description: "This is a test action",
        uid: "test-action",
        input: alwaysIO,
        output: annotationsIO,
        actionType: "increments",
        mappedInputs: [0],
        mappedOutputs: [4],
        mappingPoints: [{ x: 0, y: 4 }], // FIXME
      };
      const segments = buildSegmentsFromAction(action);
      expect(segments).toEqual([
        { start: 0, end: 0, value: annotationsIO.values.find((v) => v.numericValue == 4) },
      ]);
    }
  });

  it("can convert fully define input points that have a single output", () => {
    {
      const outputMagenta = annotationsIO.values[3];
      const action: MMAction = {
        name: "Test Action",
        description: "This is a test action",
        uid: "test-action",
        input: forestIO,
        output: annotationsIO,
        actionType: "increments",
        mappedInputs: [
          /** irrelephant */
        ],
        mappedOutputs: [
          /** irrelephant */
        ],
        mappingPoints: [{ x: 7, y: outputMagenta.numericValue }],
      };
      const segments = buildSegmentsFromAction(action);
      expect(segments).toEqual([{ start: 0, end: 15, value: outputMagenta }]);
    }
    {
      const outputMagenta = annotationsIO.values[3];
      const action: MMAction = {
        name: "Test Action",
        description: "This is a test action xx",
        uid: "test-action",
        input: heightIO,
        output: annotationsIO,
        actionType: "increments",
        mappedInputs: [
          /** irrelephant */
        ],
        mappedOutputs: [
          /** irrelephant */
        ],
        mappingPoints: [
          { x: Math.round((heightIO.min + heightIO.max) / 2), y: outputMagenta.numericValue },
        ],
      };
      const segments = buildSegmentsFromAction(action);
      expect(segments).toEqual([{ start: heightIO.min, end: heightIO.max, value: outputMagenta }]);
    }
  });

  it("correctly converts a real action (regression test)", () => {
    const action: MMAction = {
      output: {
        discrete: true,
        uid: "2130196964",
        ignoreValue: 2147483647,
        min: 0,
        max: 1,
        displayName: "Action Filter",
        values: [
          {
            displayName: "Skip",
            numericValue: 2147483647,
          },
          {
            displayName: "BLOCK (0)",
            numericValue: 0,
          },
          {
            displayName: "PASS (1)",
            numericValue: 1,
          },
        ],
        description: "only blocks that pass this filter will be used in following actions.",
        parameters: [],
      },
      uid: "168093c4-3b5d-49f9-9fc6-8941a434e681",
      actionType: "sets",
      input: {
        discrete: false,
        uid: "1927540175",
        ignoreValue: 2147483647,
        min: 0,
        max: 100,
        displayName: "Water Depth",
        values: [
          {
            displayName: "Land (0)",
            numericValue: 0,
          },
          {
            displayName: "1 deep",
            numericValue: 1,
          },
          {
            displayName: "2 deep",
            numericValue: 2,
          },
          {
            displayName: "3 deep",
            numericValue: 3,
          },
          {
            displayName: "4 deep",
            numericValue: 4,
          },
          {
            displayName: "5 deep",
            numericValue: 5,
          },
          {
            displayName: "6 deep",
            numericValue: 6,
          },
          {
            displayName: "7 deep",
            numericValue: 7,
          },
          {
            displayName: "8 deep",
            numericValue: 8,
          },
          {
            displayName: "9 deep",
            numericValue: 9,
          },
          {
            displayName: "10 deep",
            numericValue: 10,
          },
          {
            displayName: "11 deep",
            numericValue: 11,
          },
          {
            displayName: "12 deep",
            numericValue: 12,
          },
          {
            displayName: "13 deep",
            numericValue: 13,
          },
          {
            displayName: "14 deep",
            numericValue: 14,
          },
          {
            displayName: "15 deep",
            numericValue: 15,
          },
          {
            displayName: "16 deep",
            numericValue: 16,
          },
          {
            displayName: "17 deep",
            numericValue: 17,
          },
          {
            displayName: "18 deep",
            numericValue: 18,
          },
          {
            displayName: "19 deep",
            numericValue: 19,
          },
          {
            displayName: "20 deep",
            numericValue: 20,
          },
          {
            displayName: "21 deep",
            numericValue: 21,
          },
          {
            displayName: "22 deep",
            numericValue: 22,
          },
          {
            displayName: "23 deep",
            numericValue: 23,
          },
          {
            displayName: "24 deep",
            numericValue: 24,
          },
          {
            displayName: "25 deep",
            numericValue: 25,
          },
          {
            displayName: "26 deep",
            numericValue: 26,
          },
          {
            displayName: "27 deep",
            numericValue: 27,
          },
          {
            displayName: "28 deep",
            numericValue: 28,
          },
          {
            displayName: "29 deep",
            numericValue: 29,
          },
          {
            displayName: "30 deep",
            numericValue: 30,
          },
          {
            displayName: "31 deep",
            numericValue: 31,
          },
          {
            displayName: "32 deep",
            numericValue: 32,
          },
          {
            displayName: "33 deep",
            numericValue: 33,
          },
          {
            displayName: "34 deep",
            numericValue: 34,
          },
          {
            displayName: "35 deep",
            numericValue: 35,
          },
          {
            displayName: "36 deep",
            numericValue: 36,
          },
          {
            displayName: "37 deep",
            numericValue: 37,
          },
          {
            displayName: "38 deep",
            numericValue: 38,
          },
          {
            displayName: "39 deep",
            numericValue: 39,
          },
          {
            displayName: "40 deep",
            numericValue: 40,
          },
          {
            displayName: "41 deep",
            numericValue: 41,
          },
          {
            displayName: "42 deep",
            numericValue: 42,
          },
          {
            displayName: "43 deep",
            numericValue: 43,
          },
          {
            displayName: "44 deep",
            numericValue: 44,
          },
          {
            displayName: "45 deep",
            numericValue: 45,
          },
          {
            displayName: "46 deep",
            numericValue: 46,
          },
          {
            displayName: "47 deep",
            numericValue: 47,
          },
          {
            displayName: "48 deep",
            numericValue: 48,
          },
          {
            displayName: "49 deep",
            numericValue: 49,
          },
          {
            displayName: "50 deep",
            numericValue: 50,
          },
          {
            displayName: "51 deep",
            numericValue: 51,
          },
          {
            displayName: "52 deep",
            numericValue: 52,
          },
          {
            displayName: "53 deep",
            numericValue: 53,
          },
          {
            displayName: "54 deep",
            numericValue: 54,
          },
          {
            displayName: "55 deep",
            numericValue: 55,
          },
          {
            displayName: "56 deep",
            numericValue: 56,
          },
          {
            displayName: "57 deep",
            numericValue: 57,
          },
          {
            displayName: "58 deep",
            numericValue: 58,
          },
          {
            displayName: "59 deep",
            numericValue: 59,
          },
          {
            displayName: "60 deep",
            numericValue: 60,
          },
          {
            displayName: "61 deep",
            numericValue: 61,
          },
          {
            displayName: "62 deep",
            numericValue: 62,
          },
          {
            displayName: "63 deep",
            numericValue: 63,
          },
          {
            displayName: "64 deep",
            numericValue: 64,
          },
          {
            displayName: "65 deep",
            numericValue: 65,
          },
          {
            displayName: "66 deep",
            numericValue: 66,
          },
          {
            displayName: "67 deep",
            numericValue: 67,
          },
          {
            displayName: "68 deep",
            numericValue: 68,
          },
          {
            displayName: "69 deep",
            numericValue: 69,
          },
          {
            displayName: "70 deep",
            numericValue: 70,
          },
          {
            displayName: "71 deep",
            numericValue: 71,
          },
          {
            displayName: "72 deep",
            numericValue: 72,
          },
          {
            displayName: "73 deep",
            numericValue: 73,
          },
          {
            displayName: "74 deep",
            numericValue: 74,
          },
          {
            displayName: "75 deep",
            numericValue: 75,
          },
          {
            displayName: "76 deep",
            numericValue: 76,
          },
          {
            displayName: "77 deep",
            numericValue: 77,
          },
          {
            displayName: "78 deep",
            numericValue: 78,
          },
          {
            displayName: "79 deep",
            numericValue: 79,
          },
          {
            displayName: "80 deep",
            numericValue: 80,
          },
          {
            displayName: "81 deep",
            numericValue: 81,
          },
          {
            displayName: "82 deep",
            numericValue: 82,
          },
          {
            displayName: "83 deep",
            numericValue: 83,
          },
          {
            displayName: "84 deep",
            numericValue: 84,
          },
          {
            displayName: "85 deep",
            numericValue: 85,
          },
          {
            displayName: "86 deep",
            numericValue: 86,
          },
          {
            displayName: "87 deep",
            numericValue: 87,
          },
          {
            displayName: "88 deep",
            numericValue: 88,
          },
          {
            displayName: "89 deep",
            numericValue: 89,
          },
          {
            displayName: "90 deep",
            numericValue: 90,
          },
          {
            displayName: "91 deep",
            numericValue: 91,
          },
          {
            displayName: "92 deep",
            numericValue: 92,
          },
          {
            displayName: "93 deep",
            numericValue: 93,
          },
          {
            displayName: "94 deep",
            numericValue: 94,
          },
          {
            displayName: "95 deep",
            numericValue: 95,
          },
          {
            displayName: "96 deep",
            numericValue: 96,
          },
          {
            displayName: "97 deep",
            numericValue: 97,
          },
          {
            displayName: "98 deep",
            numericValue: 98,
          },
          {
            displayName: "99 deep",
            numericValue: 99,
          },
          {
            displayName: "100 deep",
            numericValue: 100,
          },
        ],
        description:
          "depth of water. if used as output, it only changes terrain height, not water level!",
        parameters: [],
      },
      mappedOutputs: [
        0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
        0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1,
        1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1,
        1, 1, 1, 1, 1, 1, 1, 1,
      ],
      name: "Filter: Only On Water",
      description: "Default filter: block all blocks that are not below waterlevel",
      mappedInputs: [
        0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 20, 21, 22, 23, 24,
        25, 26, 27, 28, 29, 30, 31, 32, 33, 34, 35, 36, 37, 38, 39, 40, 41, 42, 43, 44, 45, 46, 47,
        48, 49, 50, 51, 52, 53, 54, 55, 56, 57, 58, 59, 60, 61, 62, 63, 64, 65, 66, 67, 68, 69, 70,
        71, 72, 73, 74, 75, 76, 77, 78, 79, 80, 81, 82, 83, 84, 85, 86, 87, 88, 89, 90, 91, 92, 93,
        94, 95, 96, 97, 98, 99, 100,
      ],
      mappingPoints: [
        {
          x: 0,
          y: 0,
        },
        {
          x: 50,
          y: 0,
        },
        {
          x: 51,
          y: 1,
        },
        {
          x: 100,
          y: 1,
        },
      ],
    };
    const segments = buildSegmentsFromAction(action);
    expect(segments).toEqual([
      { start: 0, end: 50, value: { displayName: "BLOCK (0)", numericValue: 0 } },
      { start: 51, end: 100, value: { displayName: "PASS (1)", numericValue: 1 } },
    ]);
  });
});

describe("convert segments into mappingpoint[]", () => {
  it("can handle empty list", () => {
    const mappings = getMappingPointArrayFromSegments([]);
    expect(mappings).toEqual([]);
  });
  it("can convert single segment list", () => {
    const segment: Segment = {
      start: -4,
      end: 3,
      value: { displayName: "hello", numericValue: 72 },
    };
    const mappings = getMappingPointArrayFromSegments([segment]);
    const expectedMappingPoints: MappingPointDTO[] = [{ x: 3, y: 72 }];
    expect(mappings).toEqual(expectedMappingPoints);
  });
  it("can convert multi segment list", () => {
    const mappings = getMappingPointArrayFromSegments([
      { start: -4, end: -2, value: { displayName: "hello", numericValue: 72 } },
      { start: -1, end: 3, value: { displayName: "hello", numericValue: -15 } },
      { start: 4, end: 4, value: { displayName: "hello", numericValue: 73 } },
      { start: 5, end: 10, value: { displayName: "hello", numericValue: 1 } },
    ]);
    const expectedMappingPoints: MappingPointDTO[] = [
      { x: -2, y: 72 },
      { x: 3, y: -15 },
      { x: 4, y: 73 },
      { x: 10, y: 1 },
    ];
    expect(mappings).toEqual(expectedMappingPoints);
  });
});

describe("test if segment[] is valid", () => {
  it("will reject empty list", () => {
    expect(areSegmentsValid([], 0, 10)).toBeFalsy();
  });
  it("will accept list with single legal segment", () => {
    expect(
      areSegmentsValid(
        [{ start: 0, end: 10, value: { displayName: "hello", numericValue: 72 } }],
        0,
        10,
      ),
    ).toBeTruthy();
  });
  it("will reject list with single illegal segment", () => {
    expect(
      areSegmentsValid(
        [
          { start: 0, end: 5, value: { displayName: "hello", numericValue: 72 } },
          { start: 7, end: 6, value: { displayName: "hello", numericValue: 72 } }, // illegal start/end
          { start: 8, end: 10, value: { displayName: "hello", numericValue: 72 } },
        ],
        0,
        10,
      ),
    ).toBeFalsy();
  });
});
