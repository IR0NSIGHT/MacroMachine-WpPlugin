import { InputOutput, isInputOutput, validateInputOutput } from "@/types/InputOutput"

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


export function isMMAction(value: unknown): value is MMAction {
    return (
        value !== null &&
        typeof value === "object" &&
        !Array.isArray(value) &&
        isInputOutput((value as any).input) &&
        isInputOutput((value as any).output) &&
        isActionType((value as any).actionType) &&
        Array.isArray((value as any).inputPoints) &&
        (value as any).inputPoints.every((n: unknown) => typeof n === "number") &&
        Array.isArray((value as any).outputPoints) &&
        (value as any).outputPoints.every((n: unknown) => typeof n === "number") &&
        typeof (value as any).name === "string" &&
        typeof (value as any).description === "string" &&
        typeof (value as any).uid === "string"
    );
}

type ValidationResult = {
    valid: boolean;
    reason?: string;
    details?: unknown;
};

const isValidInput = (
    inputs: number[],
    input: InputOutput
): ValidationResult => {
    console.log("compare input:", inputs, input.values)
    const trueInputValueLen = inputs.filter(v => v != input.ignoreValue).length;
    if (trueInputValueLen !== input.values.filter(v => v.numericValue != input.ignoreValue).length) {
        return {
            valid: false,
            reason: "LENGTH_MISMATCH",
            details: {
                expected: inputs.length,
                actual: input.values.length,
            },
        };
    }

    for (let i = 0; i < inputs.length; i++) {
        if (inputs[i] !== input.values[i].numericValue) {
            return {
                valid: false,
                reason: "VALUE_MISMATCH",
                details: {
                    index: i,
                    expected: inputs[i],
                    actual: input.values[i].numericValue,
                },
            };
        }
    }

    return { valid: true };
};

const isValidOutput = (
    outputs: number[],
    output: InputOutput
): ValidationResult => {
    const superSet = new Set(output.values.map(v => v.numericValue));
    const illegalValues = outputs.filter(v => !superSet.has(v));

    if (illegalValues.length > 0) {
        return {
            valid: false,
            reason: "SET_MISMATCH",
            details: {
                missing: illegalValues,
            },
        };
    }

    return { valid: true };
};

export const isValidAction = (action: MMAction): boolean => {
    if (!isMMAction(action)) {
        console.log(action,"not of type action");
        return false;
    }
    const inputValidation = isValidInput(action.inputPoints, action.input);
    if (!inputValidation.valid) {
        console.log(action, "invalid inputs", inputValidation)
        return false;
    }
    const outputValidation = isValidOutput(action.outputPoints, action.output);

    if (!outputValidation.valid) {
        console.log(action, "invalid outputs", outputValidation)
        return false;
    }

    return true;
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
