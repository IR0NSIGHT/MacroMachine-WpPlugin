// src/mocks/db.ts
import { raiseYonCyan, slopeToForest, grassEverywhere } from "@/mock/dummyActions";
import { Macro, UUID } from "@/types/MMacro";
import { MMAction } from "@/types/MMAction";

export const macroList: UUID[] = [
  "macro-1",
  "macro-2"
];

export const macros: Record<string, Macro> = {
  "macro-1": {
    uid: "macro-1",
    name: "Macro-1",
    description: "",
    activeActions: [true,true],
    executionUUIDs: [raiseYonCyan.uid, slopeToForest.uid]
  },
  "macro-2": {
    uid: "macro-2",
    name: "Macro-2",
    description: "",
    activeActions: [true],
    executionUUIDs: [grassEverywhere.uid]
  }
};

const actionList = [raiseYonCyan, slopeToForest, grassEverywhere];

export const actions: Record<string, MMAction> =
  Object.fromEntries(actionList.map(a => [a.uid, a]));
