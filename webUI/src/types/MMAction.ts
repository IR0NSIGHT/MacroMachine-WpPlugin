import {InputOutput, isInputOutput, validateInputOutput} from "@/types/InputOutput"

export type ActionType =
    | "increment"
    | "subtract"
    | "multiply"
    | "divide"
    | "set"
    | "limit"
    | "set minimum";

export const ACTION_TYPES: ActionType[] = [
    "increment",
    "subtract",
    "multiply",
    "divide",
    "set",
    "limit",
    "set minimum",
]

export function isActionType(value: any): value is ActionType {
    return ACTION_TYPES.includes(value)
}

export interface MMAction {
    input: InputOutput
    output: InputOutput
    actionType: ActionType
    inputPoints: number[]
    outputPoints: number[]
    name: string
    description: string
    uid: string
}


export function isMMAction(value: any): value is MMAction {
  return (
      value &&
      typeof value === "object" &&
      isInputOutput(value.input) &&
      isInputOutput(value.output) &&
      isActionType(value.actionType) &&
      Array.isArray(value.inputPoints) &&
      value.inputPoints.every((n: any) => typeof n === "number") &&
      Array.isArray(value.outputPoints) &&
      value.outputPoints.every((n: any) => typeof n === "number") &&
      typeof value.name === "string" &&
      typeof value.description === "string" &&
      typeof value.uid === "string"
  )
}

export function assertMMAction(value: any): asserts value is MMAction {
    const path: string[] = []

    function fail(msg: string) {
        throw new Error(`MMAction validation failed at ${path.join(".")}: ${msg}`)
    }

    if (!value || typeof value !== "object") {
        throw new Error("MMAction is not an object")
    }

    // name
    path.push("name")
    if (typeof value.name !== "string") fail("expected string")
    path.pop()

    // uid
    path.push("uid")
    if (typeof value.uid !== "string") fail("expected string")
    path.pop()

    // description
    path.push("description")
    if (typeof value.description !== "string") fail("expected string")
    path.pop()

    // actionType
    path.push("actionType")
    if (typeof value.actionType !== "string") fail("expected string")
    path.pop()

    // input
    path.push("input")
    if (!value.input || typeof value.input !== "object") fail("expected object")
    validateInputOutput(value.input, path)
    path.pop()

    // output
    path.push("output")
    if (!value.output || typeof value.output !== "object") fail("expected object")
    validateInputOutput(value.output, path)
    path.pop()

    // inputPoints
    path.push("inputPoints")
    if (!Array.isArray(value.inputPoints)) fail("expected array")
    if (!value.inputPoints.every((n: any) => typeof n === "number")) {
        fail("must be number[]")
    }
    path.pop()

    // outputPoints
    path.push("outputPoints")
    if (!Array.isArray(value.outputPoints)) fail("expected array")
    if (!value.outputPoints.every((n: any) => typeof n === "number")) {
        fail("must be number[]")
    }
    path.pop()
}
