import type { ActionDTO as GeneratedActionDTO } from "../generated/client";

export const toCleanAction = (action: GeneratedActionDTO): GeneratedActionDTO => {
  const cleanCopy: GeneratedActionDTO = {
    name: action.name,
    description: action.description,
    uid: action.uid,
    input: action.input,
    output: action.output,
    mappedInputs: action.mappedInputs,
    mappedOutputs: action.mappedOutputs,
    mappingPointsX: action.mappingPointsX,
    mappingPointsY: action.mappingPointsY,
    actionType: action.actionType,
  };
  return cleanCopy;
};

export default toCleanAction;
