import React, { useMemo, useRef, useState } from "react";
import { Menu } from "@mui/material";
import { InputOutput, NamedValue } from "@/types/InputOutput";
import { InputValueEditor } from "./SingleValues/InputValueEditor";
import { start } from "@storybook/builder-vite";

type Segment = {
    id: string;
    start: number;
    end: number;
    value: NamedValue;
};

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
    const width = Math.max(0, right - left);

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
                onClick={(e) => onSegmentClick(segment.id, e)}
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
    height?: number;
};

function clamp(n: number, min: number, max: number) {
    return Math.max(min, Math.min(max, n));
}

function uid() {
    return Math.random().toString(36).slice(2);
}

function toPercent(v: number, min: number, max: number) {
    return ((v - min) / (max - min)) * 100;
}

export default function RangeValueAxisEditor({
    input,
    output,
    initialSegments,
    height = 90,
}: Props) {
    const min = input.min;
    const max = input.max;
    const allowedValues = input.values;

    const [segments, setSegments] = useState<Segment[]>(
        initialSegments ?? [
            { id: uid(), start: min, end: max, value: allowedValues[0] },
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
        setSegmentEnd(activeSegmentId, value.numericValue);
        setMenuAnchor(null);
        setEditInput(false);
    };

    const handleSegmentClick = (segmentId: string, event?: React.MouseEvent<SVGRectElement>) => {
        setActiveSegmentId(segmentId);
        setMenuAnchor(event?.currentTarget as any);
        event?.stopPropagation();
    }


    // -------------------------
    // SPLIT (NOW ONLY FROM AXIS)
    // -------------------------
    const handleAxisClick = (e: React.MouseEvent<HTMLDivElement>) => {
        if (!containerRef.current) return;

        const rect = containerRef.current.getBoundingClientRect();
        const x = e.clientX - rect.left;
        const ratio = x / rect.width;
        const value = min + ratio * (max - min);

        setSegments((prev) => {
            const idx = prev.findIndex((s) => value >= s.start && value <= s.end);
            if (idx === -1) return prev;

            const seg = prev[idx];
            if (value === seg.start || value === seg.end) return prev;

            const left: Segment = {
                id: uid(),
                start: seg.start,
                end: value,
                value: seg.value,
            };

            const right: Segment = {
                id: uid(),
                start: value,
                end: seg.end,
                value: seg.value,
            };

            const copy = [...prev];
            copy.splice(idx, 1, left, right);
            return copy;
        });
    };

    // -------------------------
    // VALUE CHANGE
    // -------------------------
    const setSegmentOutputValue = (id: string, value: NamedValue) => {
        setSegments((prev) =>
            prev.map((s) => (s.id === id ? { ...s, value } : s))
        );
    };

    const setSegmentEnd = (id: string, newEnd: number) => {
        const currentSegIdx = segments.findIndex((s) => s.id === id);
        const leftBorderValue = currentSegIdx === 0 ? min : {
            ...segments[currentSegIdx - 1],
        }.end;
        const rightBorderValue = currentSegIdx >= segments.length - 2 ? max : {
            ...segments[currentSegIdx + 2],
        }.start;

        newEnd = clamp(newEnd, leftBorderValue + 1, rightBorderValue - 1);

        const currentSeg: Segment = validateSegment({
            ...segments.find((s) => s.id === id)!,
            end: newEnd,
        });
        if (!currentSeg) return;
        if (currentSegIdx === -1) return;
        const nextRightSeg: Segment = validateSegment({
            ...segments[currentSegIdx + 1],
            start: currentSeg.end,
        });



        setSegments((prev) =>
            prev.map((s) => {
                if (s.id === currentSeg.id) return currentSeg;
                if (s.id === nextRightSeg.id) return nextRightSeg;
                return s;
            })
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

        window.addEventListener("pointermove", onPointerMove);
        window.addEventListener("pointerup", onPointerUp);
    };

    const validateSegment = (segment: Segment): Segment => {
        const validatedStart = clamp(segment.start, min, max);
        const validatedEnd = clamp(segment.end, min, max);
        return {
            ...segment,
            start: Math.min(validatedStart, validatedEnd),
            end: Math.max(validatedStart, validatedEnd),
        };
    };

    const onPointerMove = (e: PointerEvent) => {
        const drag = dragState.current;
        if (!drag || !containerRef.current) return;

        const rect = containerRef.current.getBoundingClientRect();
        const dx = e.clientX - drag.startX;
        const deltaValue = (dx / rect.width) * (max - min);

        // dragging always changes the END of the current segment
        const newEnd = clamp(drag.initialEnd + deltaValue, min, max);
        setSegmentEnd(drag.id, newEnd);
    };

    const onPointerUp = () => {
        dragState.current = null;
        window.removeEventListener("pointermove", onPointerMove);
        window.removeEventListener("pointerup", onPointerUp);
    };

    // -------------------------
    // COLORS
    // -------------------------
    const colorMap: Record<string, string> = useMemo(() => {
        const colors = ["#4f8cff", "#22c55e", "#f97316", "#ef4444", "#a855f7"];
        const map: Record<string, string> = {};
        allowedValues.forEach((v, i) => {
            map[v.numericValue] = colors[i % colors.length];
        });
        return map;
    }, [allowedValues]);

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
            <svg width="100%" height={height - 30}>
                {segments.map((s) => {
                    const left = toPercent(s.start, min, max);
                    const right = toPercent(s.end, min, max);

                    return (
                        <SegmentBody key={s.id} segment={s} left={left} right={right} onSegmentClick={handleSegmentClick} />
                    );
                })}
                {/* SEGMENTS LAYER */}


                {/* HANDLES LAYER (always on top) */}
                <g>
                    {segments.map((segment) => {
                        const endPercent = toPercent(segment.end, min, max);

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
                                    pointerEvents: "all",
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
                <InputValueEditor includeIgnore={true} label={"Output"} value={segments.find((s) => s.id === activeSegmentId)?.value?.numericValue ?? 0} input={output} onChange={updateCurrentSegmentOutput} />
            </Menu>

            {/* SELECT MAX VALUE (input) FOR RANGE */}
            <Menu
                anchorEl={menuAnchor}
                open={editInput}    
                onClose={() => setMenuAnchor(null)}
            >
                <InputValueEditor includeIgnore={false} label={"Input"} value={segments.find((s) => s.id === activeSegmentId)?.end ?? 0} input={input} onChange={updateCurrentSegmentEnd} />
            </Menu>
        </div>
    );
}