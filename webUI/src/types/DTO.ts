import type {
  ActionDTO as GeneratedActionDTO,
  MacroDTO as GeneratedMacroDTO,
  ExecutionStateDTO as GeneratedExecutionStateDTO,
  ExecutionQueueDTO as GeneratedExecutionQueueDTO,
} from "../generated/client";

export type ActionDTO = GeneratedActionDTO;
export type MacroDTO = GeneratedMacroDTO;
export type ExecutionStateDTO = GeneratedExecutionStateDTO;
export type ExecutionQueueDTO = GeneratedExecutionQueueDTO;
export type InputDTO = ActionDTO["input"];
export type OutputDTO = ActionDTO["output"];
export type IOType = ActionDTO["output"]["type"] | ActionDTO["input"]["type"];
export type ActionType = ActionDTO["actionType"];

export function isMacroDTO(value: MacroDTO | ActionDTO): value is MacroDTO {
  return "executionUUIDs" in value;
}
