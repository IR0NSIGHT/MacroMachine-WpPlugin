import { ActionDTO } from "@/types/DTO";
import { valueToString } from "./InputOutput";
import { collectRanges } from "./Ranges";
import { StepItemType } from "./Execution";

export const filterValuePass = 1;
export const filterValueBlock = 0;

export type NamedMapping = {
  input: number;
  output: number;
  inputName: string;
  outputName: string;
};

export const isFilter = (item: ActionDTO): boolean => {
  return item.output.type === "INTERMEDIATE_SELECTION";
};

export const namedMapping = (action: ActionDTO): NamedMapping[] => {
  const outputToString = valueToString(action.output);
  const inputToString = valueToString(action.input);

  return Array.from(
    { length: action.input.max - action.input.min + 1 },
    (_, i) => action.input.min + i,
  ).map((inputNumericValue, idx) => {
    const outputNumericValue = action.mappedOutputs[idx];
    const inputName = inputToString(inputNumericValue);
    const outputName = outputToString(outputNumericValue);
    return {
      input: inputNumericValue,
      output: outputNumericValue,
      inputName: inputName,
      outputName: outputName,
    };
  });
};

const isValueBlockedByFilter = (filterValue: number): boolean => {
  return filterValue !== filterValueBlock;
};

export const allowedValues = (action: ActionDTO) => {
  const all = namedMapping(action);
  const allowed = all.filter((mapping) => isValueBlockedByFilter(mapping.output));
  return allowed;
};

export const forbiddenValues = (action: ActionDTO) => {
  const all = namedMapping(action);
  const forbidden = all.filter((mapping) => !isValueBlockedByFilter(mapping.output));
  return forbidden;
};

export const invertFilter = (filter: StepItemType): StepItemType => {
  const filterValueIgnore = filter.output.ignoreValue;
  const inverted = {
    ...filter,
    mappingPointsY: filter.mappingPointsY.map((v) =>
      v === filterValueBlock ? filterValueIgnore : filterValueBlock,
    ),
    mappedOutputs: filter.mappedOutputs.map((v) =>
      v === filterValueBlock ? filterValueIgnore : filterValueBlock,
    ),
  };
  return inverted;
};

export function invertFilterSinglePosition<T extends ActionDTO>(filter: T, input: number): T {
  const filterValueIgnore = filter.output.ignoreValue;
  const inverted = {
    ...filter,
    mappingPointsY: filter.mappingPointsY.map((v, idx) => {
      if (filter.mappingPointsX[idx] === input)
        return v === filterValueBlock ? filterValueIgnore : filterValueBlock;
      else return v;
    }),
    mappedOutputs: filter.mappedOutputs.map((v, idx) => {
      if (filter.mappedInputs[idx] === input)
        return v === filterValueBlock ? filterValueIgnore : filterValueBlock;
      else return v;
    }),
  };
  return inverted;
}

export const isRangeFilter = (filter: ActionDTO): boolean => {
  if (filter.input.discrete) return false;
  const all = getRelevantMappings(filter);
  const ranges = collectRanges(all);
  return ranges.some((range) => range.length > 1); // continuous ranges exist that are interesting
};

export const getRelevantMappings = (filter: ActionDTO): NamedMapping[] => {
  const passValues = allowedValues(filter);
  const blockValues = forbiddenValues(filter);
  // only show either PASS or BLOCK values to keep it simple as "Only on .." or "Except on"
  const relevant = passValues.length > blockValues.length ? blockValues : passValues;
  return relevant;
};

export const isOnlyOnRangeFilter = (filter: ActionDTO): boolean => {
  const ranges = collectRanges(namedMapping(filter));
  const blockRanges = ranges.filter((r) => r.start.output === filterValueBlock);
  const passRanges = ranges.filter((r) => r.start.output !== filterValueBlock);
  const isOnlyOn = passRanges.length <= blockRanges.length;
  return isOnlyOn;
};

export const isInsideRangeFilter = (item: StepItemType): boolean => {
  const ranges = getRelevantMappings(item);
  return ranges[0]?.output !== filterValueBlock; // if the first relevant mapping is not blocked, we are an INSIDE RANGE filter
};

const explainRangeFilter = (filter: ActionDTO): string => {
  // only show either PASS or BLOCK values to keep it simple as "Only on .." or "Except on"
  const ranges = collectRanges(namedMapping(filter));
  const blockRanges = ranges.filter((r) => r.start.output === filterValueBlock);
  const passRanges = ranges.filter((r) => r.start.output !== filterValueBlock);
  const isOnlyOn = passRanges.length <= blockRanges.length;
  const relevant = isOnlyOn ? passRanges : blockRanges;
  return (
    "Filter by " +
    filter.input.displayName +
    ": " +
    (isOnlyOn ? "Only on " : "Except on ") +
    relevant
      .map((range) => {
        return "[" + range.start.inputName + " to " + range.end.inputName + "]";
      })
      .join(", ")
  );
};

const explainSimpleFilter = (filter: ActionDTO): string => {
  const passValues = allowedValues(filter);
  const blockValues = forbiddenValues(filter);
  if (blockValues.length < passValues.length) {
    const name =
      "Filter by " +
      filter.input.displayName +
      ": Except on " +
      blockValues.map((mapping) => mapping.inputName).join(", ");
    return name;
  } else {
    const name =
      "Filter by " +
      filter.input.displayName +
      ": Only on " +
      passValues.map((mapping) => mapping.inputName).join(", ");
    return name;
  }
};

export const explainSingleFilterMapping = (mapping: NamedMapping, inputDisplayName: string) => {
  const reject = mapping.output === filterValueBlock ? "rejects" : "allows";
  return (
    "if a position on the map has *" +
    inputDisplayName +
    "*=*" +
    mapping.inputName +
    "* then the filter **" +
    reject +
    "** it."
  );
};

export const filterAutoName = (filter: StepItemType): StepItemType => {
  const description = "Filter: Reject blocks based on " + filter.input.displayName;
  const isRange = isRangeFilter(filter);
  if (!isRange) {
    return { ...filter, name: explainSimpleFilter(filter), description: description };
  } else {
    const name = explainRangeFilter(filter);
    console.log("auto naming filter", name);
    return { ...filter, name: name, description: description };
  }
};
