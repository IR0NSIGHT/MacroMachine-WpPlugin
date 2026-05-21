import type { components } from "../generated/api-types";

export type ActionDTO = components["schemas"]["ActionDTO"];
export type MacroDTO = components["schemas"]["MacroDTO"];
export type ExecutionStateDTO = components["schemas"]["ExecutionStateDTO"];
export type ExecutionQueueDTO = components["schemas"]["ExecutionQueueDTO"];
export type InputDTO = ActionDTO["input"];
export type OutputDTO = ActionDTO["output"];
export type IOType = ActionDTO["output"]["type"] | ActionDTO["input"]["type"];
export type ActionType = ActionDTO["actionType"];

export function isMacroDTO(value: MacroDTO | ActionDTO): value is MacroDTO {
  return "executionUUIDs" in value;
}
