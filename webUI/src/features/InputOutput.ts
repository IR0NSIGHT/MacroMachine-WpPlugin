import { InputDTO, OutputDTO } from "@/types/DTO";

export const valueToString = (io: InputDTO | OutputDTO) : (v: number)=>string => {
  const outputToString = new Map<number, string>();
  for (const [idx, value] of io.valueDisplayNames.entries()) {
    outputToString.set(idx - io.min, value);
  }
  return (value: number) => {
    if (io.ignoreValue === value)
      return "SKIP";
    if (outputToString.has(value))
      return outputToString.get(value)!
    else
      return "unknown";
  };
};
