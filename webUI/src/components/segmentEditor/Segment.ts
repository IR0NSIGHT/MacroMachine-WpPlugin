import { NamedValue } from "@/types/InputOutput";
import { assert } from "vitest";

export type Segment = {
    id: string;
    start: number;
    end: number;
    value: NamedValue;
};

export type Range = {
    start: number;
    end: number;
};

export type Segments = Segment[];

export const splitAt = (segments: Segments, position: number): Segments => {
    const newSegments: Segments = [];
    for (const segment of segments) {
        if (position > segment.start && position < segment.end) {
            newSegments.push(
                { ...segment, end: position },
                { ...segment, id: `${segment.id}-2`, start: position },
            );
        } else {
            newSegments.push(segment);
        }
    }
    assert(areSegmentsValid(newSegments, segments[0].start, segments[segments.length - 1].end), "Invalid segments after splitting");
    return newSegments;
};

export const mergeSegments = (segments: Segments, position: number): Segments => {
    const segment1 = segments.find(s => s.start === position || s.end === position);
    const segment2 = segments.find(s => s.start === position || s.end === position);
    if (!segment1 || !segment2) {
        return segments;
    }
    const mergedSegment: Segment = {
        id: segment1.id,
        start: Math.min(segment1.start, segment2.start),
        end: Math.max(segment1.end, segment2.end),
        value: segment1.value,
    };
    const newSegments = segments.filter(s => s.id !== segment1.id && s.id !== segment2.id).concat(mergedSegment).sort((a, b) => a.start - b.start);
    assert(areSegmentsValid(newSegments, segments[0].start, segments[segments.length - 1].end), "Invalid segments after merging");  
    return newSegments;
};

export const shiftSegment = (segments: Segments, segmentId: string, newStart: number, newEnd: number): Segments => {
    const range: Range = { start: Math.min(...segments.map(s => s.start)), end: Math.max(...segments.map(s => s.end)) };
    assert(areSegmentsValid(segments, segments[0].start, segments[segments.length - 1].end), "Invalid segments before shifting");

    const segment = segments.find(s => s.id === segmentId);
    if (!segment) {
        return segments;
    }

    // make sure newStart and newEnd are within the global range of segments
    newStart = Math.max(range.start, Math.min(range.end, newStart));
    newEnd = Math.max(range.start, Math.min(range.end, newEnd));


    const segmentIdx = segments.findIndex(s => s.id === segmentId);
    if (segmentIdx !== 0) { // clamp to left border
        newStart = Math.max(newStart, segments[segmentIdx - 1].end + 1); 
    }
    if (segmentIdx !== segments.length - 1) { // clamp to right border
        newEnd = Math.min(newEnd, segments[segmentIdx + 1].start - 1);
    }
    if (segmentIdx === 0) {
        newStart = range.start;
    }
    if (segmentIdx === segments.length - 1) {
        newEnd = range.end;
    }
    const shiftedSegment: Segment = {
        ...segment,
        start: newStart,
        end: newEnd,
    };
    const newSegments = segments.filter(s => s.id !== segmentId).concat(shiftedSegment).sort((a, b) => a.start - b.start);
    assert(areSegmentsValid(newSegments, range.start, range.end), "Invalid segments after shifting");
    return newSegments;
};

/**
 * test that the complete range is filled with segments, and that no segment has start > end or overlaps with other segments
 * @param segments 
 * @param start the start of the complete range that should be filled with segments
 * @param end the end of the complete range that should be filled with segments
 * @returns 
 */
export const areSegmentsValid = (segments: Segments, start: number, end: number): boolean => {
    if (!isRangeFilled(segments, start, end)) {
        return false;
    }
    let previousSegment: Segment | undefined = undefined;
    for (const segment of segments) {
        if (!isSegmentValid(segment)) {
            return false;
        }
        if (previousSegment && segment.start < previousSegment.end) {
            return false;
        }
        previousSegment = segment;
    }
    return true;
};

const isRangeFilled = (segments: Segments, start: number, end: number): boolean => {
        if (segments.length === 0) {
        return false;
    }
    const globalMin = Math.min(...segments.map(s => s.start));
    const globalMax = Math.max(...segments.map(s => s.end));
    if (globalMin != start || globalMax != end) {
        return false;
    }
    return true;
};

const isSegmentValid = (segment: Segment): boolean => {
    return segment.start <= segment.end;
};


const validateSegment = (segment: Segment): Segment => {
    const validatedStart = segment.start;
    const validatedEnd = segment.end;
    return {
        ...segment,
        start: Math.min(validatedStart, validatedEnd),
        end: Math.max(validatedStart, validatedEnd),
    };
};