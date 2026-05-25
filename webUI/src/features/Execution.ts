import { ActionDTO, MacroDTO } from "@/types/DTO";
import { StepItemType } from "./Filters";

export type uuid = string;
export function isUUID(value: unknown): value is uuid {
  return typeof value === "string";
}

export type runnableMacro = {
  steps: StepItemType[];
  name: string;
  description: string;
  uid: string;
};
export function isRunnableMacro(value: unknown): value is runnableMacro {
  if (typeof value !== "object" || value === null) {
    return false;
  }

  const obj = value as Record<string, unknown>;

  return (
    Array.isArray(obj.steps) &&
    typeof obj.name === "string" &&
    typeof obj.description === "string" &&
    typeof obj.uid === "string"
  );
}

export type MacroExecuteRequester = (runnable: uuid | runnableMacro, isDebug: boolean) => void;

export function toMacroDTO(runnabel: runnableMacro): MacroDTO {
  return {
    name: runnabel.name,
    description: runnabel.description,
    uid: runnabel.uid,
    executionUUIDs: runnabel.steps.map((a) => a.uid),
    activeActions: runnabel.steps.map((a) => a.active),
  };
}

export function toRunnable(
  macro: MacroDTO | undefined,
  actions: ActionDTO[],
): runnableMacro | undefined {
  if (!macro) return undefined;
  console.log("to runnable:", macro, actions);
  try {
    const uidSet = new Set(actions.map((a) => a.uid));
    const stepItems: StepItemType[] = macro.executionUUIDs
      .filter((uid) => uidSet.has(uid))
      .map((uid, idx) => ({
        ...actions.find((a) => a.uid === uid)!,
        active: macro.activeActions[idx],
      }));
    return {
      name: macro.name,
      description: macro.description,
      uid: macro.uid,
      steps: stepItems,
    };
  } catch (error) {
    console.log(error);
  }
}
