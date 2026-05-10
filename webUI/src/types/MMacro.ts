import { ValidationResult } from "./MMAction";

export type UUID = string;

export type Macro = {
  name: string;
  description: string;
  uid: UUID;
  executionUUIDs: UUID[];
  activeActions: boolean[];
};

export function isMacro(value: unknown): value is Macro {
  if (!value || typeof value !== "object") return false;

  const v = value as any;

  return (
    typeof v.name === "string" &&
    typeof v.description === "string" &&
    typeof v.uid === "string" &&
    Array.isArray(v.executionUUIDs) &&
    v.executionUUIDs.every((x: unknown) => typeof x === "string") &&
    Array.isArray(v.activeActions) &&
    v.activeActions.every((x: unknown) => typeof x === "boolean")
  );
}

export function validateMacro(value: unknown): ValidationResult<Macro> {
  if (!value || typeof value !== "object") {
    return {
      valid: false,
      reason: "NOT_OBJECT",
      details: value,
    };
  }

  const v = value as any;

  if (typeof v.name !== "string") {
    return { valid: false, reason: "INVALID_NAME", details: v.name };
  }

  if (typeof v.description !== "string") {
    return { valid: false, reason: "INVALID_DESCRIPTION", details: v.description };
  }

  if (typeof v.uid !== "string") {
    return { valid: false, reason: "INVALID_UID", details: v.uid };
  }

  if (!Array.isArray(v.executionUUIDs)) {
    return { valid: false, reason: "INVALID_EXECUTION_UUIDS" };
  }

  for (const id of v.executionUUIDs) {
    if (typeof id !== "string") {
      return {
        valid: false,
        reason: "EXECUTION_UUID_NOT_STRING",
        details: id,
      };
    }
  }

  if (!Array.isArray(v.activeActions)) {
    return { valid: false, reason: "INVALID_ACTIVE_ACTIONS" };
  }

  for (const a of v.activeActions) {
    if (typeof a !== "boolean") {
      return {
        valid: false,
        reason: "ACTIVE_ACTION_NOT_BOOLEAN",
        details: a,
      };
    }
  }

  return {
    valid: true,
    value: v as Macro,
  };
}
