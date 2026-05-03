import React, { useRef, useState } from "react";
import { Box, Menu, styled } from "@mui/material";
import { useTheme } from "@mui/material/styles";
import { InputOutput, NamedValue } from "@/types/InputOutput";
import { InputValueEditor } from "../SingleValues/InputValueEditor";
import { Segment, splitAt, Interval, shiftSegment, mergeSegments } from "./Segment";
import { clamp, toPercent } from "@/util";
import { DeleteButton } from "./DeleteButton";

const Root = styled(Box)(({ theme }) => ({
    width: "100%",
    padding: theme.spacing(1.5),
    borderRadius: theme.shape.borderRadius,
    backgroundColor: theme.palette.background.paper,
    border: `1px solid ${theme.palette.divider}`,
    userSelect: "none",
}));

const Axis = styled(Box)(({ theme }) => ({
    height: 18,
    marginTop: theme.spacing(1),
    borderRadius: theme.shape.borderRadius,
    cursor: "pointer",
    backgroundColor: theme.palette.action.hover,
    border: `1px solid ${theme.palette.divider}`,
    position: "relative",
}));

const AxisCenterLine = styled(Box)(({ theme }) => ({
    position: "absolute",
    top: "50%",
    left: 0,
    right: 0,
    height: 1,
    backgroundColor: theme.palette.text.disabled,
    transform: "translateY(-50%)",
}));

const AxisLabel = styled(Box)(({ theme }) => ({
    position: "absolute",
    top: 0,
    transform: "translateX(-50%)",
    fontSize: theme.typography.caption.fontSize,
    color: theme.palette.text.secondary,
    pointerEvents: "none",
}));

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
    const theme = useTheme();
    const width = Math.max(0, right - left);

    const getDisplayName = (numericValue: number) => {
        const value = input.values.find(v => v.numericValue === numericValue);
        return value ? value.displayName : "?";
    };

    return (
        <g>
            <rect
                x={`${left}%`}
                y={10}
                width={`${width}%`}
                height={36}
                fill={theme.palette.primary.main}
                rx={6}
                style={{ cursor: "pointer" }}
                onClick={(e) => onSegmentClick(segment.start, e)}
            />

            <text
                x={`${left}%`}
                y={32}
                textAnchor="start"
                fontSize={12}
                fill={theme.palette.primary.contrastText}
                pointerEvents="none"
            >
                {getDisplayName(segment.start)}
            </text>

            <text
                x={`${left + width / 2}%`}
                y={32}
                textAnchor="middle"
                fontSize={12}
                fill={theme.palette.primary.contrastText}
                pointerEvents="none"
            >
                {segment.value?.displayName ?? "undefined value"}
            </text>

            <text
                x={`${left + width}%`}
                y={32}
                textAnchor="end"
                fontSize={12}
                fill={theme.palette.primary.contrastText}
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
    segments: Segment[];
    setSegments: (newSegs: Segment[]) => void;
};

export default function RangeValueAxisEditor({
    input,
    output,
    segments,
    setSegments
}: Props) {
    console.log("range editor segments:",segments);
    const interval: Interval = { start: input.min, end: input.max, };

    const getActiveSegment: () => Segment | undefined = () => {
        const selectedSeg = segments.find(s => s.start === menuState.currentSegmentStart);
        console.log("Getting active segment for id", menuState.currentSegmentStart, "found", selectedSeg);
        return selectedSeg;
    };

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
        setSegments(shiftSegment(segments, currentSegment.start, currentSegment.start, value.numericValue));
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

    const getDisplayName = (numericValue?: number) => {
        const value = input.values.find(v => v.numericValue === numericValue);
        return value ? value.displayName : "?";
    };

    // -------------------------
    // SPLIT (NOW ONLY FROM AXIS)
    // -------------------------
    const handleAxisClick = (e: React.MouseEvent<HTMLDivElement>) => {
        if (!containerRef.current) return;

        const rect = containerRef.current.getBoundingClientRect();
        const x = e.clientX - rect.left;
        const ratio = x / rect.width;
        const value = interval.start + ratio * (interval.end - interval.start);

        setSegments(splitAt(segments, value));
    };

    // -------------------------
    // VALUE CHANGE
    // -------------------------
    const setSegmentOutputValue = (segmentStart: number, value: NamedValue) => {
        console.log("Setting segment output value for segment", segmentStart, "to", value);
        setSegments(
            segments.map((s) => (s.start === segmentStart ? { ...s, value } : s))
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
        setSegments(shiftSegment(segments, drag.initialStart, drag.initialStart, newEnd));
    };

    const onPointerUp = () => {
        dragState.current = null;
        window.removeEventListener("pointermove", onDragSegmentEnd);
        window.removeEventListener("pointerup", onPointerUp);
    };
    const closeMenu = () => {
        setMenuStateNull();
    };

    const intervalSize = interval.end - interval.start + 1;
    const xAxisSplitEvery = Math.round(intervalSize / 10);

    console.log("render ranges: ", segments)

    return (
        <Root ref={containerRef}>
            {/* =========================
          SEGMENTS (TOP LAYER)
      ========================= */}
            <svg width="100%" height={60}>
                {segments.map((s) => {
                    const left = toPercent(s.start, interval.start, interval.end);
                    const right = toPercent(s.end, interval.start, interval.end);

                    return (
                        <SegmentBody
                            key={s.start}
                            segment={s}
                            left={left}
                            right={right}
                            input={input}
                            onSegmentClick={selectSegment}
                        />
                    );
                })}

                {/* HANDLES */}
                <g>
                    {segments.map((segment) => {
                        const endPercent = toPercent(
                            segment.end + 0.5,
                            interval.start,
                            interval.end
                        );

                        return (
                            <rect
                                key={segment.start}
                                x={`${endPercent}%`}
                                y={14}
                                width={6}
                                height={28}
                                rx={3}
                                fill="currentColor"
                                style={{
                                    cursor: "ew-resize",
                                    transform: "translateX(-3px)",
                                }}
                                onPointerDown={(e) =>
                                    onHandlePointerDown(e, segment.start)
                                }
                                onDoubleClick={(e) => {
                                    setMenuState({
                                        mouse: { x: e.clientX, y: e.clientY },
                                        type: "input",
                                        anchor: null,
                                        currentSegmentStart: segment.start,
                                    });
                                }}
                            />
                        );
                    })}
                </g>
            </svg>

            {/* =========================
          AXIS
      ========================= */}
            <Axis onClick={handleAxisClick}>
                <AxisCenterLine />

                {input.values
                    .filter((v) => v.numericValue % xAxisSplitEvery === 0)
                    .map((value) => {
                        const leftPercent = toPercent(
                            value.numericValue,
                            interval.start,
                            interval.end
                        );

                        return (
                            <AxisLabel
                                key={value.numericValue}
                                sx={{ left: `${leftPercent}%` }}
                            >
                                {value.displayName}
                            </AxisLabel>
                        );
                    })}
            </Axis>

            {/* =========================
          OUTPUT MENU
      ========================= */}
            <Menu
                anchorReference="anchorPosition"
                anchorPosition={
                    menuState.mouse
                        ? { top: menuState.mouse.y, left: menuState.mouse.x }
                        : undefined
                }
                open={menuState.type === "output"}
                onClose={closeMenu}
                slotProps={{
                    paper: {
                        sx: {
                            minWidth: 260,
                            maxWidth: 400,
                            minHeight: 120,
                            p: 2,
                            bgcolor: "background.paper",
                        },
                    },
                }}
            >
                <Box
                    sx={{
                        display: "flex",
                        flexDirection: "column",
                        gap: 2,
                    }}
                >
                    <Box sx={{ typography: "body2", color: "text.secondary" }}>
                        For all blocks where {input.displayName} is between{" "}
                        {getDisplayName(getActiveSegment()?.start)} and{" "}
                        {getDisplayName(getActiveSegment()?.end)}, set{" "}
                        {output.displayName} to
                    </Box>

                    <InputValueEditor
                        includeIgnore
                        label="Output"
                        value={
                            getActiveSegment()?.value?.numericValue ?? output.min
                        }
                        input={output}
                        onChange={updateCurrentSegmentOutput}
                    />

                    {segments.length > 1 && (
                        <Box sx={{ pt: 1 }}>
                            <DeleteButton onClick={onDeleteCurrentSegment} />
                        </Box>
                    )}
                </Box>
            </Menu>

            {/* =========================
          INPUT MENU
      ========================= */}
            <Menu
                anchorReference="anchorPosition"
                anchorPosition={
                    menuState.mouse
                        ? { top: menuState.mouse.y, left: menuState.mouse.x }
                        : undefined
                }
                open={menuState.type === "input"}
                onClose={closeMenu}
                slotProps={{
                    paper: {
                        sx: {
                            minWidth: 260,
                            maxWidth: 400,
                            minHeight: 120,
                            p: 2,
                            bgcolor: "background.paper",
                        },
                    },
                }}
            >
                <Box sx={{ display: "flex", flexDirection: "column", gap: 2 }}>
                    <InputValueEditor
                        includeIgnore={false}
                        label="Input"
                        value={getActiveSegment()?.end ?? input.max}
                        input={input}
                        onChange={updateCurrentSegmentEnd}
                    />
                </Box>
            </Menu>
        </Root>
    );
}