import { ActionDTO } from "@/types/DTO";
import { valueToString } from "./InputOutput";

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
