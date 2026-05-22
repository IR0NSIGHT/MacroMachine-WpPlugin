import { InputDTO, OutputDTO } from "@/types/DTO";

export const SKIP_DISPLAY_NAME = "Ignore";

export const valueToString = (io: InputDTO | OutputDTO): ((v: number) => string) => {
  const outputToString = new Map<number, string>();
  for (const [idx, value] of io.valueDisplayNames.entries()) {
    outputToString.set(idx - io.min, value);
  }
  return (value: number) => {
    if (io.ignoreValue === value) {
      console.log("encoutnered ignore value for IO, names:", io.displayName, io.valueDisplayNames);
      return SKIP_DISPLAY_NAME; // FIXME is this a hard convention?
    }
    if (outputToString.has(value)) return outputToString.get(value)!;
    else return "unknown";
  };
};
