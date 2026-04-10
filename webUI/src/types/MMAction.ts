import {InputOutput} from "@/types/InputOutput"

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

export interface NamedValue {
    numericValue: number
    displayName: string
}

export function isNamedValue(value: any): value is NamedValue {
    return (
        value &&
        typeof value === "object" &&
        typeof value.numericValue === "number" &&
        typeof value.displayName === "string"
    )
}

export interface ioParameter {
    name: string
    type: 'string' | 'number' | 'boolean' | 'select'
    options?: string[] | number[] | boolean[]
    value: string | number | boolean
}

export function isIoParameter(value: any): value is ioParameter {
  const validTypes = ["string", "number", "boolean", "select"]

  return (
      value &&
      typeof value === "object" &&
      typeof value.name === "string" &&
      validTypes.includes(value.type) &&
      ("value" in value)
  )
}

export interface InputOutput {
    displayName: string
    description: string
    min: number
    max: number
    ignoreValue: number
    values: NamedValue[]
    discrete: boolean
    uid: string
    parameters: ioParameter[]
}

export function isInputOutput(value: any): value is InputOutput {
  return (
      value &&
      typeof value === "object" &&
      typeof value.displayName === "string" &&
      typeof value.description === "string" &&
      typeof value.min === "number" &&
      typeof value.max === "number" &&
      typeof value.ignoreValue === "number" &&
      typeof value.discrete === "boolean" &&
      typeof value.uid === "string" &&
      Array.isArray(value.values) &&
      value.values.every(isNamedValue) &&
      Array.isArray(value.parameters) &&
      value.parameters.every(isIoParameter)
  )
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

function validateInputOutput(value: any, path: string[]) {
    const fail = (msg: string) => {
        throw new Error(`MMAction.${path.join(".")}: ${msg}`)
    }

    // displayName
    path.push("displayName")
    if (typeof value.displayName !== "string") fail("expected string")
    path.pop()

    // description
    path.push("description")
    if (typeof value.description !== "string") fail("expected string")
    path.pop()

    // min/max
    path.push("min")
    if (typeof value.min !== "number") fail("expected number")
    path.pop()

    path.push("max")
    if (typeof value.max !== "number") fail("expected number")
    path.pop()

    path.push("ignoreValue")
    if (typeof value.ignoreValue !== "number") fail("expected number")
    path.pop()

    // discrete
    path.push("discrete")
    if (typeof value.discrete !== "boolean") fail("expected boolean")
    path.pop()

    // uid
    path.push("uid")
    if (typeof value.uid !== "string") fail("expected string")
    path.pop()

    // values
    path.push("values")
    if (!Array.isArray(value.values)) fail("expected array")
    for (let i = 0; i < value.values.length; i++) {
        const v = value.values[i]
        path.push(`[${i}]`)

        if (typeof v.numericValue !== "number") fail("numericValue must be number")
        if (typeof v.displayName !== "string") fail("displayName must be string")

        path.pop()
    }
    path.pop()

    // parameters
    path.push("parameters")
    if (!Array.isArray(value.parameters)) fail("expected array")
    for (let i = 0; i < value.parameters.length; i++) {
        const p = value.parameters[i]
        path.push(`[${i}]`)

        if (typeof p.name !== "string") fail("name must be string")
        if (!["string", "number", "boolean", "select"].includes(p.type)) {
            fail("invalid type")
        }
        if (!("value" in p)) fail("missing value field")

        path.pop()
    }
    path.pop()
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
