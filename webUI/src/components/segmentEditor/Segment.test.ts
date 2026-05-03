// src/utils/sum.test.ts
import { describe, it, expect } from "vitest";
import { Segments, splitAt, mergeSegments, areSegmentsValid, shiftSegment, buildSegmentsFromAction, Segment, mappingsFromSegments } from "./Segment";
import { MMAction } from "@/types/MMAction";
import { alwaysIO, annotationsIO, forestIO, heightIO } from "@/mock/dummyIOs";

describe("segment splitting", () => {
    it ("will not split at the border of an existing segment", () => {
        const segments: Segments = [
            { start: 0, end: 10, value: { displayName: "A", numericValue: 3 } },
            { start: 11, end: 20, value: { displayName: "B", numericValue: 4 } },
        ];
        const newSegments = splitAt(segments, 10);
        expect(newSegments).toEqual(segments);
        expect(areSegmentsValid(newSegments, 0, 20)).toBe(true);
    })

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
        const segments: Segments = [
            { start: 0, end: 3, value: { displayName: "A", numericValue: 3 } },
        ];
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
    it("does not merge out-ouf-bounds positions",()=>{
        const segments: Segments = [
            { start: 0, end: 5, value: { displayName: "A", numericValue: 3 } },
            { start: 6, end: 10, value: { displayName: "B", numericValue: 5 } },
        ];
        expect(areSegmentsValid(segments, 0, 10)).toBe(true);

        const newSegments = mergeSegments(segments, 400);
        expect(newSegments).toEqual(segments);
    })
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
            { start: 0, end: 10, value: { displayName: "A", numericValue: 3 } },
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
            { start: 2, end: 10, value: { displayName: "B", numericValue: 5 } }
        ]); // segment 1 would have width 0, so shift is not applied
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
            mappedInputs: [/** irrelephant */],
            mappedOutputs: [/** irrelephant */],
            mappingPoints: []
        }
        const segments = buildSegmentsFromAction(action);
        expect(segments).toEqual([
            { start: forestIO.min, end: forestIO.max, value: annotationsIO.values.find(v => v.numericValue == annotationsIO.ignoreValue) },
        ])
    })

    it("can convert actions with multiple output groups/ranges", () => {
        {
            const action: MMAction = {
                name: "Test Action",
                description: "This is a test action",
                uid: "test-action",
                input: forestIO,
                output: annotationsIO,
                actionType: "increments",
                mappedInputs: forestIO.values.filter(v => v.numericValue !== forestIO.ignoreValue).map(v => v.numericValue),
                mappedOutputs: [4, 4, 4, 4, 3, 3, 3, 3, 15, 15, 15, 15, 15, 15, 15, 15],
                mappingPoints: [{ x: 3, y: 4 }, { x: 4, y: 3 }, { x: 8, y: 15 },] // FIXME

            }
            const segments = buildSegmentsFromAction(action);
            expect(segments).toEqual([
                { start: 0, end: 3, value: annotationsIO.values.find(v => v.numericValue == 4) },
                { start: 4, end: 7, value: annotationsIO.values.find(v => v.numericValue == 3) },
                { start: 8, end: 15, value: annotationsIO.values.find(v => v.numericValue == 15) },
            ])
        }

        {
            const action: MMAction = {
                name: "Test Action",
                description: "This is a test action",
                uid: "test-action",
                input: forestIO,
                output: annotationsIO,
                actionType: "increments",
                mappedInputs: forestIO.values.filter(v => v.numericValue !== forestIO.ignoreValue).map(v => v.numericValue),
                mappedOutputs: [4, 4, 4, 4, 3, 5, 4, 4, 7, 7, 7, 7, 0, 0, 0, 15],
                mappingPoints: [{ x: 0, y: 4 }, { x: 4, y: 3 }, { x: 5, y: 5 }, { x: 7, y: 4 }, { x: 8, y: 7 }, { x: 12, y: 0 }, { x: 15, y: 15 },] // FIXME

            }
            const segments = buildSegmentsFromAction(action);
            expect(segments).toEqual([
                { start: 0, end: 3, value: annotationsIO.values.find(v => v.numericValue == 4) },
                { start: 4, end: 4, value: annotationsIO.values.find(v => v.numericValue == 3) },
                { start: 5, end: 6, value: annotationsIO.values.find(v => v.numericValue == 5) },
                { start: 7, end: 7, value: annotationsIO.values.find(v => v.numericValue == 4) },
                { start: 8, end: 11, value: annotationsIO.values.find(v => v.numericValue == 7) },
                { start: 12, end: 14, value: annotationsIO.values.find(v => v.numericValue == 0) },
                { start: 15, end: 15, value: annotationsIO.values.find(v => v.numericValue == 15) },
            ])
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
                mappingPoints: [{ x: 0, y: 4 }] // FIXME

            }
            const segments = buildSegmentsFromAction(action);
            expect(segments).toEqual([
                { start: 0, end: 0, value: annotationsIO.values.find(v => v.numericValue == 4) },
            ])
        }
    })

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
                mappedInputs: [/** irrelephant */],
                mappedOutputs: [/** irrelephant */],
                mappingPoints: [{ x: 7, y: outputMagenta.numericValue }]

            }
            const segments = buildSegmentsFromAction(action);
            expect(segments).toEqual([
                { start: 0, end: 15, value: outputMagenta }
            ])
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
                mappedInputs: [/** irrelephant */],
                mappedOutputs: [/** irrelephant */],
                mappingPoints: [{ x: Math.round((heightIO.min + heightIO.max) / 2), y: outputMagenta.numericValue }]
            }
            const segments = buildSegmentsFromAction(action);
            expect(segments).toEqual([
                { start: heightIO.min, end: heightIO.max, value: outputMagenta }
            ])
        }
    })
});

describe("convert segemnts into mapping lists for use in action", () => {
    it("can convert single segment list", () => {
        const segment: Segment = { start: -4, end: 3, value: { displayName: "hello", numericValue: 72 } }
        const mappings = mappingsFromSegments([segment]);
        expect(mappings).toEqual({ inputs: [-4, -3, -2, -1, 0, 1, 2, 3], outputs: [72, 72, 72, 72, 72, 72, 72, 72] })
    })
    it("can convert multi segment list", () => {
        const mappings = mappingsFromSegments([
            { start: -4, end: -2, value: { displayName: "hello", numericValue: 72 } },
            { start: -1, end: 3, value: { displayName: "hello", numericValue: -15 } },
            { start: 4, end: 4, value: { displayName: "hello", numericValue: 73 } },
            { start: 5, end: 10, value: { displayName: "hello", numericValue: 1 } }
        ]);
        expect(mappings).toEqual({ inputs: [-4, -3, -2, -1, 0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10], outputs: [72, 72, 72, -15, -15, -15, -15, -15, 73, 1, 1, 1, 1, 1, 1] })
    })
})