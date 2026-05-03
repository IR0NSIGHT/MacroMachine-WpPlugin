import { NamedValue } from "@/types/InputOutput";
import { MappingPointDTO, MMAction } from "@/types/MMAction";

export type Segment = {
    start: number;
    end: number;
    value: NamedValue;
};

export type Interval = {
    start: number;
    end: number;
};

export type Segments = Segment[];

export function buildSegmentsFromAction(action: MMAction): Segments {
    var mappingPoints = [...action.mappingPoints].sort((a, b) => a.x - b.x);
    if (mappingPoints.length === 0) {
        mappingPoints.unshift({ x: action.input.min, y: action.output.ignoreValue });
    }

    const segments: Segments = [];
    for (let i = 0; i < mappingPoints.length; i++) {
        const mappingPoint = mappingPoints[i];
        const rangeEnd = i !== mappingPoints.length - 1 ? mappingPoints[i + 1].x -1 :  action.input.max ;
        
        console.log("idx",i,"range",mappingPoint.x,rangeEnd);
        const segment = {
            start: i !== 0 ? mappingPoint.x : action.input.min, // first segment always starts at range min
            end: rangeEnd,
            value: action.output.values.find(v => v.numericValue == mappingPoint.y)!,
        }
        segments.push(segment);
    }
    console.log("built segments from action:", action, segments);
    return segments;
}

export const splitAt = (segments: Segments, position: number): Segments => {
    position = Math.round(position);
    const newSegments: Segments = [];
    const segmentToBeSplit = segments.find(s => s.start < position && s.end > position);
    if (!segmentToBeSplit) {
        return segments;
    }
    if (segmentToBeSplit.start === position || segmentToBeSplit.end === position) {
        return segments; // don't split if position is at the border of a segment
    }
    if (segmentToBeSplit.end - segmentToBeSplit.start <= 2) { // smallest split is 0-1 and 1-2, so segment has to be at least 3 wide to be split
        return segments; // don't split if segment is too small to be split
    }
    for (const segment of segments) {
        if (position > segment.start && position < segment.end) {
            newSegments.push(
                { ...segment, end: position },
                { ...segment, start: position + 1 },
            );
        } else {
            newSegments.push(segment);
        }
    }
    //    assert(areSegmentsValid(newSegments, segments[0].start, segments[segments.length - 1].end), "Invalid segments after splitting");
    return newSegments;
};

export const mergeSegments = (segments: Segments, position: number): Segments => {
    const targetSegment = segments.find(s => s.start === position || s.end === position);
    if (!targetSegment) {
        return segments;
    }
    const neighbourToEat = segments.find(s => s.start === targetSegment.end + 1 || s.end === targetSegment.start - 1);
    if (!neighbourToEat) {
        return segments;
    }
    const mergedSegment: Segment = {
        start: Math.min(targetSegment.start, neighbourToEat.start),
        end: Math.max(targetSegment.end, neighbourToEat.end),
        value: targetSegment.value,
    };
    const newSegments = segments.filter(s => s.start !== targetSegment.start && s.start !== neighbourToEat.start).concat(mergedSegment).sort((a, b) => a.start - b.start);
    //    assert(areSegmentsValid(newSegments, segments[0].start, segments[segments.length - 1].end), "Invalid segments after merging");  
    return newSegments;
};

export const shiftSegment = (segments: Segments, oldSegmentStart: number, newStart: number, newEnd: number): Segments => {
    const range: Interval = { start: Math.min(...segments.map(s => s.start)), end: Math.max(...segments.map(s => s.end)) };
    //assert(areSegmentsValid(segments, segments[0].start, segments[segments.length - 1].end), "Invalid segments before shifting");

    newStart = Math.round(newStart);
    newEnd = Math.round(newEnd);

    const segment = segments.find(s => s.start === oldSegmentStart);
    if (!segment) {
        return segments;
    }

    if (newEnd <= newStart) {
        newEnd = newStart + 1; // ensure segment has width of at least 1
    }
    // make sure newStart and newEnd are within the global range of segments
    newStart = Math.max(range.start, Math.min(range.end, newStart));
    newEnd = Math.max(range.start, Math.min(range.end, newEnd));


    const segmentIdx = segments.findIndex(s => s.start === oldSegmentStart);
    if (segmentIdx !== 0) { // clamp to left border: avoid eating up left neighbour
        newStart = Math.max(newStart, segments[segmentIdx - 1].start + 2);
    }
    if (segmentIdx !== segments.length - 1) { // clamp to right border
        newEnd = Math.min(newEnd, segments[segmentIdx + 1].end - 2);
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
    const leftNeighbour = segments.find(s => s.end === segment.start - 1);
    const rightNeighbour = segments.find(s => s.start === segment.end + 1);
    const newSegments = segments.filter(s => s.start !== oldSegmentStart && s.start !== leftNeighbour?.start && s.start !== rightNeighbour?.start)
        .concat(shiftedSegment)
        .concat(leftNeighbour ? { ...leftNeighbour, end: shiftedSegment.start - 1 } : [])
        .concat(rightNeighbour ? { ...rightNeighbour, start: shiftedSegment.end + 1 } : [])
        .sort((a, b) => a.start - b.start);
    if (!areSegmentsValid(newSegments, range.start, range.end)) {
        console.error("Invalid segments after shifting:", newSegments);
        return segments; // return original segments if new segments are invalid
    }
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
            console.error("Invalid segment:", segment);
            return false;
        }
        if (previousSegment && segment.start !== previousSegment.end + 1) {
            console.error("Segments have a hole between them:", previousSegment, segment);
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

export const mappingsFromSegments = (segments: Segments): { inputs: number[], outputs: number[] } => {
    const inputs: number[] = [];
    const outputs: number[] = [];
    segments.forEach(seg => {
        for (let val = seg.start; val <= seg.end; val++) {
            inputs.push(val);
            outputs.push(seg.value.numericValue);
        }
    })


    return { inputs: inputs, outputs: outputs }
}

export const getMappingPointArrayFromSegments = (segments: Segments): MappingPointDTO[] => {
    const mappingPoints: MappingPointDTO[] = [];
    segments.slice(0,segments.length-1).forEach(seg => {
        mappingPoints.push({ x: seg.end, y: seg.value.numericValue });
    })
    console.log("turn segemtns into points:", segments, mappingPoints);
    return mappingPoints;
}   