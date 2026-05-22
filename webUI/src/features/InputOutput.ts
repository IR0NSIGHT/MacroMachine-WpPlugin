import { InputDTO, OutputDTO } from "@/types/DTO";

export const SKIP_DISPLAY_NAME = "Ignore";

export const valueToString = (io: InputDTO | OutputDTO): ((v: number) => string) => {
  const outputToString = new Map<number, string>();
  Array.from({ length: io.max - io.min + 1 }, (_, i) => io.min + i)
    .map((inputV, idx) => ({
      value: inputV,
      name: io.valueDisplayNames[idx],
    }))
    .forEach((item) => {
      outputToString.set(item.value, item.name);
    });
  return (value: number) => {
    if (io.ignoreValue === value) {
      return SKIP_DISPLAY_NAME;
    }
    if (outputToString.has(value)) {
      return outputToString.get(value)!;
    } else {
      return "unknown (" + value + ")";
    }
  };
};
