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
    if (mappingPoints.length >= 2) {
        const firstPoint = mappingPoints[0];
        const secondPoint = mappingPoints[1];
        
        const lastPoint = mappingPoints[mappingPoints.length - 1];
        const secondLastPoint = mappingPoints[mappingPoints.length -2];

        if (firstPoint.y === secondPoint.y) {
            mappingPoints.shift(); // if first two points have same y value, first point is redundant and can be removed
        }
        if (lastPoint.y === secondLastPoint.y) {
            mappingPoints.pop(); // if last two points have same y value, last point is redundant and can be removed
        }
    }
    const segments: Segments = [];
    for (let i = 0; i < mappingPoints.length; i++) {
        const mappingPoint = mappingPoints[i];
        const rangeEnd = i !== mappingPoints.length - 1 ? mappingPoints[i + 1].x -1 :  action.input.max ;
        
        const segment = {
            start: i !== 0 ? mappingPoint.x : action.input.min, // first segment always starts at range min
            end: rangeEnd,
            value: action.output.values.find(v => v.numericValue == mappingPoint.y)!,
        }
        segments.push(segment);
    }
    return segments;
}

export const splitAt = (segments: Segments, position: number): Segments => {
    position = Math.round(position);
    const newSegments: Segments = [];
    const segmentToBeSplit = segments.find(s => s.start < position && s.end > position);
    if (!segmentToBeSplit) {
        return segments;
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
    const neighbourToEat = segments.find(s => s.start === position + 1 || s.end === position - 1);
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
        if (previousSegment && segment.start !== previousSegment.end + 1) {
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

export const getMappingPointArrayFromSegments = (segments: Segments): MappingPointDTO[] => {
    const mappingPoints: MappingPointDTO[] = [];
    if (segments.length === 0) {
        return [];
    }
    segments.forEach(seg => {
        mappingPoints.push({ x: seg.end, y: seg.value.numericValue });
    })
    return mappingPoints;
}   