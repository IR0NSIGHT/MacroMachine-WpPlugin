import { ActionDTO, MacroDTO } from "@/types/DTO";

export type uuid = string;
export function isUUID(value: unknown): value is uuid {
  return typeof value === "string";
}

export function isStepItem(value: StepItemType | StepMacroType): value is StepItemType {
  return "actionType" in value;
}

export function isStepMacro(value: StepItemType | StepMacroType): value is StepMacroType {
  return "executionUUIDs" in value;
}

export type runnableMacro = {
  steps: (StepItemType | StepMacroType)[];
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
  macros: MacroDTO[],
): runnableMacro | undefined {
  if (!macro) return undefined;
  const actionsAndMacros = [...actions, ...macros];
  try {
    const uidSet = new Set(actionsAndMacros.map((a) => a.uid));
    const stepItems: (StepItemType | StepMacroType)[] = macro.executionUUIDs
      .filter((uid) => uidSet.has(uid))
      .map((uid, idx) => ({
        ...actionsAndMacros.find((a) => a.uid === uid)!,
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
export type StepItemType = ActionDTO & { active: boolean };
export type StepMacroType = MacroDTO & { active: boolean };
