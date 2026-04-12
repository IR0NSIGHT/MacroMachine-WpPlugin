import React, { useRef, useState } from "react";
import { Button, Menu } from "@mui/material";
import { InputOutput, NamedValue } from "@/types/InputOutput";
import { InputValueEditor } from "../SingleValues/InputValueEditor";
import { Segment, splitAt, Interval, shiftSegment, mergeSegments } from "./Segment";
import { clamp, toPercent } from "@/util";
import { DeleteButton } from "./DeleteButton";
const SegmentBody = ({
    segment,
    left,
    right,
    input,
    onSegmentClick,
}: {
    segment: Segment;
    left: number;
    right: number;
    input: InputOutput;
    onSegmentClick: (
        segmentStart: number,
        event: React.MouseEvent<SVGRectElement>
    ) => void;
}) => {
    const width = Math.max(0, right - left);

    const getDisplayName = (numericValue: number) => {
        const value = input.values.find(v => v.numericValue === numericValue);
        return value ? value.displayName : "?";
    };

    return (
        <g>
            {/* Segment background */}
            <rect
                x={`${left}%`}
                y={10}
                width={`${width}%`}
                height={36}
                fill={"#4f8cff"}
                rx={6}
                style={{ cursor: "pointer" }}
                onClick={(e) => onSegmentClick(segment.start, e)}
            />

            {/* LEFT label (segment start) */}
            <text
                x={`${left}%`}
                y={32}
                textAnchor="start"
                fontSize={12}
                fill="white"
                pointerEvents="none"
            >
                {getDisplayName(segment.start)}
            </text>

            {/* CENTER label (display name) */}
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

            {/* RIGHT label (segment end) */}
            <text
                x={`${left + width}%`}
                y={32}
                textAnchor="end"
                fontSize={12}
                fill="white"
                pointerEvents="none"
            >
                {getDisplayName(segment.end)}
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

    const getActiveSegment: () => Segment | undefined = () => {
        const selectedSeg = segments.find(s => s.start === menuState.currentSegmentStart);
        console.log("Getting active segment for id", menuState.currentSegmentStart, "found", selectedSeg);
        return selectedSeg;
    };

    const [segments, setSegments] = useState<Segment[]>(
        initialSegments ?? [
            { id: uid(), start: interval.start, end: interval.end, value: allowedValues[0] },
        ]
    );

    const [menuState, setMenuState] = useState<{
        mouse: { x: number; y: number } | null;
        type: "output" | "input" | null;
        anchor: HTMLElement | null;
        currentSegmentStart: number | null;
    }>({
        mouse: null,
        type: null,
        anchor: null,
        currentSegmentStart: null,
    });

    const setMenuStateNull = () => setMenuState({
        mouse: null,
        type: null,
        anchor: null,
        currentSegmentStart: null,
    });

    const containerRef = useRef<HTMLDivElement | null>(null);

    const updateCurrentSegmentOutput = (value: NamedValue) => {
        console.log("Updating segment output value for segment", menuState.currentSegmentStart, "to", value, "menuzState", menuState);
        if (menuState.currentSegmentStart === null) return;
        setSegmentOutputValue(menuState.currentSegmentStart, value);
        setMenuStateNull();
    };

    const updateCurrentSegmentEnd = (value: NamedValue) => {
        if (null === menuState.currentSegmentStart) return;
        const currentSegment = segments.find(s => s.start === menuState.currentSegmentStart);
        if (!currentSegment) return;
        shiftSegment(segments, currentSegment.start, currentSegment.start, value.numericValue);
        setMenuStateNull();
    };

    const selectSegment = (segmentStart: number, event?: React.MouseEvent<SVGRectElement>) => {
        setMenuState({
            mouse: event ? { x: event.clientX, y: event.clientY } : null,
            type: "output",
            anchor: event?.currentTarget as any ?? null,
            currentSegmentStart: segmentStart,
        });
        event?.stopPropagation();
        console.log("Selected segment", segmentStart, getActiveSegment());
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
    const setSegmentOutputValue = (segmentStart: number, value: NamedValue) => {
        console.log("Setting segment output value for segment", segmentStart, "to", value);
        setSegments((prev) =>
            prev.map((s) => (s.start === segmentStart ? { ...s, value } : s))
        );
    };

    const onDeleteCurrentSegment =
        () => {
            setSegments(mergeSegments(segments, getActiveSegment()?.end ?? -1));
            setMenuStateNull();
        }


    // -------------------------
    // DRAG RESIZE
    // -------------------------
    const dragState = useRef<{
        startX: number;
        initialStart: number;
        initialEnd: number;
    } | null>(null);

    const onHandlePointerDown = (e: React.PointerEvent, start: number) => {
        e.stopPropagation();

        const seg = segments.find((s) => s.start === start);
        if (!seg) return;

        dragState.current = {
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
        setSegments((prev) => shiftSegment(prev, drag.initialStart, drag.initialStart, newEnd));
    };

    const onPointerUp = () => {
        dragState.current = null;
        window.removeEventListener("pointermove", onDragSegmentEnd);
        window.removeEventListener("pointerup", onPointerUp);
    };
    const closeMenu = () => {
        setMenuStateNull();
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
                        <SegmentBody key={s.start} segment={s} left={left} right={right} input={input} onSegmentClick={selectSegment} />
                    );
                })}

                {/* SEGMENT HANDLES (always on top) */}
                <g>
                    {segments.map((segment) => {
                        const endPercent = toPercent(segment.end + 0.5, interval.start, interval.end);

                        // convert percent → actual positioning via SVG percent offset
                        // we subtract half handle width in *pixels*, so we need a stable transform approach

                        return (
                            <g key={segment.start}>
                                <rect
                                    x={`${endPercent}%`}
                                    y={14}
                                    width={6}
                                    height={28}
                                    fill="#e5e7eb"
                                    rx={3}
                                    style={{
                                        cursor: "ew-resize",
                                        transform: "translateX(-3px)",
                                    }}
                                    onPointerDown={(e) => onHandlePointerDown(e, segment.start)}
                                    onDoubleClick={(e) => {
                                        setMenuState({
                                            mouse: e ? { x: e.clientX, y: e.clientY } : null,
                                            type: "input",
                                            anchor: e.currentTarget as any,
                                            currentSegmentStart: segment.start,
                                        });
                                    }}
                                />
                            </g>

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
                        "rgb(210, 210, 210) ",

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
                {input.values
                    .filter(v => v.numericValue % 5 == 0)
                    .map((value) => {
                        const leftPercent = toPercent(value.numericValue, interval.start, interval.end);
                        // convert percent → actual positioning via SVG percent offset
                        return (
                            <span
                                key={value.numericValue}
                                style={{
                                    position: "absolute",
                                    left: `${leftPercent}%`,
                                    top: 0,
                                    transform: "translateX(-50%)",
                                    fontSize: 10,
                                    pointerEvents: "none",
                                }}
                            >
                                {value.displayName}
                            </span>
                        );
                    })}
            </div>

            {/* SELECT OUTPUT FOR RANGE */}
            <Menu
                anchorReference="anchorPosition"
                anchorPosition={
                    menuState.mouse
                        ? { top: menuState.mouse.y, left: menuState.mouse.x }
                        : undefined
                }
                open={menuState.type === "output"}
                onClose={closeMenu}
            >
                <div className="card">
                    <InputValueEditor includeIgnore={true} label={"Output"} value={getActiveSegment()?.value?.numericValue ?? output.min} input={output} onChange={updateCurrentSegmentOutput} />
                    {segments.length > 1 && <DeleteButton onClick={onDeleteCurrentSegment} />}
                </div>

            </Menu>

            {/* SELECT INTERVAL END (input) FOR RANGE */}
            <Menu
                anchorReference="anchorPosition"
                anchorPosition={
                    menuState.mouse
                        ? { top: menuState.mouse.y, left: menuState.mouse.x }
                        : undefined
                }
                open={menuState.type === "input"}
                onClose={closeMenu}
            >
                <div className="card">
                    <InputValueEditor includeIgnore={false} label={"Input"} value={getActiveSegment()?.end ?? input.max} input={input} onChange={updateCurrentSegmentEnd} />
                </div>
            </Menu>
        </div>
    );
}