import React, { useMemo, useRef, useState } from "react";
import { Menu, MenuItem } from "@mui/material";

type AllowedValue = string;

type Segment = {
  id: string;
  start: number;
  end: number;
  value: AllowedValue;
};

type Props = {
  min: number;
  max: number;
  allowedValues: AllowedValue[];
  initialSegments?: Segment[];
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
  min,
  max,
  allowedValues,
  initialSegments,
  height = 80,
}: Props) {
  const [segments, setSegments] = useState<Segment[]>(
    initialSegments ?? [
      { id: uid(), start: min, end: max, value: allowedValues[0] },
    ]
  );

  const [menuAnchor, setMenuAnchor] = useState<null | HTMLElement>(null);
  const [activeSegmentId, setActiveSegmentId] = useState<string | null>(null);

  const containerRef = useRef<HTMLDivElement | null>(null);

  // -------------------------
  // Segment split on click
  // -------------------------
  const handleAxisClick = (e: React.MouseEvent<SVGRectElement>) => {
    const rect = (e.target as SVGRectElement).getBoundingClientRect();
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
  // Value change
  // -------------------------
  const setSegmentValue = (id: string, value: AllowedValue) => {
    setSegments((prev) =>
      prev.map((s) => (s.id === id ? { ...s, value } : s))
    );
  };

  // -------------------------
  // Drag handling
  // -------------------------
  const dragState = useRef<{
    id: string;
    startX: number;
    initialStart: number;
    initialEnd: number;
  } | null>(null);

  const onHandlePointerDown = (
    e: React.PointerEvent,
    id: string
  ) => {
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

  const onPointerMove = (e: PointerEvent) => {
    const drag = dragState.current;
    if (!drag || !containerRef.current) return;

    const rect = containerRef.current.getBoundingClientRect();
    const dx = e.clientX - drag.startX;
    const deltaValue = (dx / rect.width) * (max - min);

    setSegments((prev) =>
      prev.map((s) => {
        if (s.id !== drag.id) return s;

        const newStart = clamp(
          drag.initialStart + deltaValue,
          min,
          max
        );
        const newEnd = clamp(
          drag.initialEnd + deltaValue,
          min,
          max
        );

        return {
          ...s,
          start: Math.min(newStart, newEnd),
          end: Math.max(newStart, newEnd),
        };
      })
    );
  };

  const onPointerUp = () => {
    dragState.current = null;
    window.removeEventListener("pointermove", onPointerMove);
    window.removeEventListener("pointerup", onPointerUp);
  };

  // -------------------------
  // Colors (simple modern palette)
  // -------------------------
  const colorMap: Record<string, string> = useMemo(() => {
    const colors = ["#4f8cff", "#22c55e", "#f97316", "#ef4444", "#a855f7"];
    const map: Record<string, string> = {};
    allowedValues.forEach((v, i) => {
      map[v] = colors[i % colors.length];
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
      }}
    >
      <svg
        width="100%"
        height={height}
        style={{ overflow: "visible", cursor: "pointer" }}
      >
        {/* Axis background hit area */}
        <rect
          x="0"
          y={height / 2 - 10}
          width="100%"
          height="20"
          fill="transparent"
          onClick={handleAxisClick}
        />

        {/* Axis line */}
        <line
          x1="0"
          x2="100%"
          y1={height / 2}
          y2={height / 2}
          stroke="#374151"
          strokeWidth={2}
        />

        {/* Segments */}
        {segments.map((s) => {
          const left = toPercent(s.start, min, max);
          const right = toPercent(s.end, min, max);
          const width = right - left;

          return (
            <g key={s.id}>
              {/* Segment block */}
              <rect
                x={`${left}%`}
                y={height / 2 - 18}
                width={`${width}%`}
                height={36}
                fill={colorMap[s.value]}
                opacity={0.85}
                rx={6}
                style={{ cursor: "pointer" }}
                onClick={(e) => {
                  e.stopPropagation();
                  setActiveSegmentId(s.id);
                  setMenuAnchor(e.currentTarget as any);
                }}
              />

              {/* Label */}
              <text
                x={`${left + width / 2}%`}
                y={height / 2 + 5}
                textAnchor="middle"
                fontSize={12}
                fill="white"
                pointerEvents="none"
              >
                {s.value}
              </text>

              {/* Right handle */}
              <rect
                x={`${right}%`}
                y={height / 2 - 14}
                width={6}
                height={28}
                fill="#e5e7eb"
                rx={3}
                style={{ cursor: "ew-resize" }}
                onPointerDown={(e) => onHandlePointerDown(e, s.id)}
              />
            </g>
          );
        })}
      </svg>

      {/* Value menu */}
      <Menu
        anchorEl={menuAnchor}
        open={Boolean(menuAnchor)}
        onClose={() => setMenuAnchor(null)}
      >
        {allowedValues.map((v) => (
          <MenuItem
            key={v}
            onClick={() => {
              if (activeSegmentId) {
                setSegmentValue(activeSegmentId, v);
              }
              setMenuAnchor(null);
            }}
          >
            {v}
          </MenuItem>
        ))}
      </Menu>
    </div>
  );
}