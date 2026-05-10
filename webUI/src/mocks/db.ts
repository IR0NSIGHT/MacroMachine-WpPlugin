// src/mocks/db.ts
import {
  raiseYonCyan,
  slopeToForest,
  grassEverywhere,
  onlyOnLand,
  onlyOnCyan,
} from "@/mock/dummyActions";
import { Macro, UUID } from "@/types/MMacro";
import { MMAction } from "@/types/MMAction";
import Test_Macro from "@/mock/Test_Macro.json";

export const macroList: UUID[] = ["macro-1", "macro-2"];

export const macros: Record<string, Macro> = {
  "macro-1": {
    uid: "macro-1",
    name: "Macro-1",
    description: "",
    activeActions: [true, true],
    executionUUIDs: [onlyOnLand.uid, onlyOnCyan.uid, raiseYonCyan.uid, slopeToForest.uid],
  },
  "macro-2": {
    uid: "macro-2",
    name: "Macro-2",
    description: "",
    activeActions: [true],
    executionUUIDs: [grassEverywhere.uid],
  },
};

const actionList: MMAction[] = [
  raiseYonCyan,
  slopeToForest,
  grassEverywhere,
  onlyOnLand,
  onlyOnCyan,
  Test_Macro[0] as MMAction,
  Test_Macro[1] as MMAction,
];

console.log(Test_Macro[0]);

export const actions: Record<string, MMAction> = Object.fromEntries(
  actionList.map((a) => [a.uid, a]),
);
