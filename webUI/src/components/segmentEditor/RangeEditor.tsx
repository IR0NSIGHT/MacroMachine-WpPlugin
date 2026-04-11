import React, { useRef, useState } from "react";
import { Menu } from "@mui/material";
import { InputOutput, NamedValue } from "@/types/InputOutput";
import { InputValueEditor } from "../SingleValues/InputValueEditor";
import { Segment, splitAt, Interval, shiftSegment } from "./Segment";
import { clamp, toPercent } from "@/util";


const SegmentBody = ({
    segment,
    left,
    right,
    onSegmentClick,
}: {
    segment: Segment;
    left: number;
    right: number;
    onSegmentClick: (
        segmentId: string,
        event: React.MouseEvent<SVGRectElement>
    ) => void;
}) => {
    const width = Math.max(0, (right ) - (left )); // add small tolerance to prevent negative widths due to rounding

    return (
        <g>
            <rect
                x={`${left}%`}
                y={10}
                width={`${width}%`}
                height={36}
                fill={"#4f8cff"}
                rx={6}
                style={{ cursor: "pointer" }}
                onClick={(e) => {
                    console.log("Clicked segment body", segment.id);
                    onSegmentClick(segment.id, e)}}
            />

            <text
                x={`${left + width / 2}%`}
                y={32}
                textAnchor="middle"
                fontSize={12}
                fill="white"
                pointerEvents="none"
            >
                {segment.value.displayName}
            </text>
        </g>
    );
};

type Props = {
    input: InputOutput;
    output: InputOutput;
    initialSegments: Segment[];
};

function uid() {
    return Math.random().toString(36).slice(2);
}



export default function RangeValueAxisEditor({
    input,
    output,
    initialSegments,
}: Props) {
    const interval: Interval = { start: input.min, end: input.max };
    const allowedValues = input.values;

    const getActiveSegment: () => Segment | undefined = () => segments.find(s => s.id === activeSegmentId);

    const [segments, setSegments] = useState<Segment[]>(
        initialSegments ?? [
            { id: uid(), start: interval.start, end: interval.end, value: allowedValues[0] },
        ]
    );

    const [editOutput, setEditOutput] = useState<boolean>(false);
    const [editInput, setEditInput] = useState<boolean>(false);

    const [menuAnchor, setMenuAnchor] = useState<null | HTMLElement>(null);
    const [activeSegmentId, setActiveSegmentId] = useState<string | null>(null);

    const containerRef = useRef<HTMLDivElement | null>(null);

    const updateCurrentSegmentOutput = (value: NamedValue) => {
        if (!activeSegmentId) return;
        setSegmentOutputValue(activeSegmentId, value);
        setMenuAnchor(null);
        setEditOutput(false);
    };

    const updateCurrentSegmentEnd = (value: NamedValue) => {
        if (!activeSegmentId) return;
        const currentSegment = segments.find(s => s.id === activeSegmentId);
        if (!currentSegment) return;
        shiftSegment(segments, currentSegment.id, currentSegment.start, value.numericValue);
        setMenuAnchor(null);
        setEditInput(false);
    };

    const selectSegment = (segmentId: string, event?: React.MouseEvent<SVGRectElement>) => {
       
        setActiveSegmentId(segmentId);
        setMenuAnchor(event?.currentTarget as any);
        event?.stopPropagation();
         console.log("Selected segment", segmentId, getActiveSegment());
    }


    // -------------------------
    // SPLIT (NOW ONLY FROM AXIS)
    // -------------------------
    const handleAxisClick = (e: React.MouseEvent<HTMLDivElement>) => {
        if (!containerRef.current) return;

        const rect = containerRef.current.getBoundingClientRect();
        const x = e.clientX - rect.left;
        const ratio = x / rect.width;
        const value = interval.start + ratio * (interval.end - interval.start);

        setSegments((prev) => splitAt(prev, value));
    };

    // -------------------------
    // VALUE CHANGE
    // -------------------------
    const setSegmentOutputValue = (id: string, value: NamedValue) => {
        setSegments((prev) =>
            prev.map((s) => (s.id === id ? { ...s, value } : s))
        );
    };

    // -------------------------
    // DRAG RESIZE
    // -------------------------
    const dragState = useRef<{
        id: string;
        startX: number;
        initialStart: number;
        initialEnd: number;
    } | null>(null);

    const onHandlePointerDown = (e: React.PointerEvent, id: string) => {
        e.stopPropagation();

        const seg = segments.find((s) => s.id === id);
        if (!seg) return;

        dragState.current = {
            id,
            startX: e.clientX,
            initialStart: seg.start,
            initialEnd: seg.end,
        };

        window.addEventListener("pointermove", onDragSegmentEnd);
        window.addEventListener("pointerup", onPointerUp);
    };

    const onDragSegmentEnd = (e: PointerEvent) => {
        const drag = dragState.current;
        if (!drag || !containerRef.current) return;

        const rect = containerRef.current.getBoundingClientRect();
        const dx = e.clientX - drag.startX;
        const deltaValue = (dx / rect.width) * (interval.end - interval.start);

        // dragging always changes the END of the current segment
        const newEnd = clamp(drag.initialEnd + deltaValue, interval.start, interval.end);
        setSegments((prev) => shiftSegment(prev, drag.id, drag.initialStart, newEnd));
    };

    const onPointerUp = () => {
        dragState.current = null;
        window.removeEventListener("pointermove", onDragSegmentEnd);
        window.removeEventListener("pointerup", onPointerUp);
    };

    return (
        <div
            ref={containerRef}
            style={{
                width: "100%",
                padding: 12,
                borderRadius: 12,
                background: "#0b0f17",
                border: "1px solid #1f2937",
                userSelect: "none",
            }}
        >
            {/* =========================
          SEGMENTS (TOP LAYER)
      ========================= */}
            <svg width="100%" height={60}>
                {/* SEGMENT BODIES */}
                {segments.map((s) => {
                    const left = toPercent(s.start, interval.start, interval.end);
                    const right = toPercent(s.end, interval.start, interval.end);

                    return (
                        <SegmentBody key={s.id} segment={s} left={left} right={right} onSegmentClick={selectSegment} />
                    );
                })}

                {/* SEGMENT HANDLES (always on top) */}
                <g>
                    {segments.map((segment) => {
                        const endPercent = toPercent(segment.end + 0.5, interval.start, interval.end);

                        // convert percent → actual positioning via SVG percent offset
                        // we subtract half handle width in *pixels*, so we need a stable transform approach

                        return (
                            <rect
                                key={segment.id}
                                x={`${endPercent}%`}
                                y={14}
                                width={6}
                                height={28}
                                fill="#e5e7eb"
                                rx={3}
                                style={{
                                    cursor: "ew-resize",
                                    transform: "translateX(-3px)", // 👈 centers the 6px handle
                                }}
                                onPointerDown={(e) => onHandlePointerDown(e, segment.id)}
                                onDoubleClick={(e) => {
                                    setActiveSegmentId(segment.id);
                                    setMenuAnchor(e.currentTarget as any);
                                    setEditInput(true);
                                }}
                            />
                        );
                    })}
                </g>
            </svg>

            {/* =========================
          STRIPED AXIS (BOTTOM)
          CLICK TO SPLIT
      ========================= */}
            <div
                onClick={handleAxisClick}
                style={{
                    height: 18,
                    marginTop: 8,
                    borderRadius: 6,
                    cursor: "pointer",

                    // striped black/white axis
                    background:
                        "repeating-linear-gradient(45deg, #000 0px, #000 6px, #fff 6px, #fff 12px)",

                    border: "1px solid rgba(255,255,255,0.15)",
                    position: "relative",
                }}
            >
                {/* subtle center line */}
                <div
                    style={{
                        position: "absolute",
                        top: "50%",
                        left: 0,
                        right: 0,
                        height: 1,
                        background: "rgba(0,0,0,0.4)",
                        transform: "translateY(-50%)",
                    }}
                />
            </div>

            {/* SELECT OUTPUT FOR RANGE */}
            <Menu
                anchorEl={menuAnchor}
                open={editOutput}
                onClose={() => setMenuAnchor(null)}
            >
                <InputValueEditor includeIgnore={true} label={"Output"} value={getActiveSegment()?.value?.numericValue ?? output.min} input={output} onChange={updateCurrentSegmentOutput} />
            </Menu>

            {/* SELECT INTERVAL END (input) FOR RANGE */}
            <Menu
                anchorEl={menuAnchor}
                open={editInput}
                onClose={() => setMenuAnchor(null)}
            >
                <InputValueEditor includeIgnore={false} label={"Input"} value={getActiveSegment()?.end ?? input.max} input={input} onChange={updateCurrentSegmentEnd} />
            </Menu>
        </div>
    );
}