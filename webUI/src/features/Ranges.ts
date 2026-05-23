import { NamedMapping } from "./Filters";

export type MappingRange = {
  start: NamedMapping;
  end: NamedMapping;
  length: number;
};

export function outputSet(items: NamedMapping[]): { output: number; outputName: string }[] {
  const map = new Map();
  items.forEach((item) => {
    map.set(item.output, { output: item.output, outputName: item.outputName });
  });
  return [...map.values()];
}

export function collectRanges(items: NamedMapping[]): MappingRange[] {
  if (items.length === 0) {
    return [];
  }

  const ranges: MappingRange[] = [];

  let current: MappingRange = {
    start: items[0],
    end: items[0],
    length: -1,
  };

  for (let i = 1; i < items.length; i++) {
    const item = items[i];

    if (item.output === current.start.output) {
      current.end = item;
    } else {
      ranges.push(current);

      current = {
        start: item,
        end: item,
        length: -1,
      };
    }
  }

  ranges.push(current);

  return ranges.map((range) => ({ ...range, length: range.end.input - range.start.input + 1 }));
}
