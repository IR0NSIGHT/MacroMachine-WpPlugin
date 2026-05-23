import { ActionDTO } from "@/types/DTO";
import { valueToString } from "./InputOutput";
import { collectRanges } from "./Ranges";

export const _filterValuePass = 1;
export const filterValueBlock = 0;

export type StepItemType = ActionDTO & { active: boolean };
export type NamedMapping = {
  input: number;
  output: number;
  inputName: string;
  outputName: string;
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
  //  console.log("allowed values for ", action, " are:", allowed);
  return allowed;
};

export const forbiddenValues = (action: ActionDTO) => {
  const all = namedMapping(action);
  const forbidden = all.filter((mapping) => !isValueBlockedByFilter(mapping.output));
  //  console.log("allowed values for ", action, " are:", allowed);
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

const isRangeFilter = (filter: ActionDTO): boolean => {
  if (filter.input.discrete) return false;
  const all = getRelevantMappings(filter);
  const ranges = collectRanges(all);
  return ranges.some((range) => range.length > 1); // continuous ranges exist that are interesting
};

const getRelevantMappings = (filter: ActionDTO): NamedMapping[] => {
  const passValues = allowedValues(filter);
  const blockValues = forbiddenValues(filter);
  // only show either PASS or BLOCK values to keep it simple as "Only on .." or "Except on"
  const relevant = passValues.length > blockValues.length ? blockValues : passValues;
  return relevant;
};

const explainRangeFilter = (filter: ActionDTO): string => {
  // only show either PASS or BLOCK values to keep it simple as "Only on .." or "Except on"
  const all = getRelevantMappings(filter);
  const ranges = collectRanges(all);
  return ranges
    .map((range) => {
      let onlyOn = "";
      switch (range.start.output) {
        case filterValueBlock:
          onlyOn = "Except on ";
          break;
        case _filterValuePass: //FIXME this is misleading. its not "only on", its an OR operation
          onlyOn = "Only on ";
          break;
        case filter.output.ignoreValue:
          onlyOn = "Only on ";
          break;
      }
      return (
        onlyOn +
        filter.input.displayName +
        ": " +
        range.start.inputName +
        " to " +
        range.end.inputName
      );
    })
    .join(", ");
};

const explainSimpleFilter = (filter: ActionDTO): string => {
  const passValues = allowedValues(filter);
  const blockValues = forbiddenValues(filter);
  if (blockValues.length < passValues.length) {
    const name =
      "Except on " +
      filter.input.displayName +
      ": " +
      blockValues.map((mapping) => mapping.inputName).join(", ");
    return name;
  } else {
    const name =
      "Only on " +
      filter.input.displayName +
      ": " +
      passValues.map((mapping) => mapping.inputName).join(", ");
    return name;
  }
};

export const filterAutoName = (filter: StepItemType): StepItemType => {
  const isRange = isRangeFilter(filter);
  if (!isRange) {
    return { ...filter, name: explainSimpleFilter(filter) };
  } else {
    return { ...filter, name: explainRangeFilter(filter) };
  }
};
