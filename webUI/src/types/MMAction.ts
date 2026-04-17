import { InputOutput, isInputOutput, validateInputOutput } from "@/types/InputOutput"

export type ActionType =
    | "increments"
    | "subtracts"
    | "multiplies"
    | "divides"
    | "sets"
    | "limits"
    | "sets minimum";

    //INCREMENT("increments"), DECREMENT("subtracts"), MULTIPLY("multiplies"), DIVIDE("divides"), SET("sets"), LIMIT_TO(
    //        "limits"), AT_LEAST("sets minimum");

export const ACTION_TYPES: ActionType[] = [
    "increments",
    "subtracts",
    "multiplies",
    "divides",
    "sets",
    "limits",
    "sets minimum",
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

export function validateMMActionType(value: MMAction): ValidationResult<MMAction> {
    if (value === null || typeof value !== "object" || Array.isArray(value)) {
        return {
            valid: false,
            reason: "Value must be a non-null object",
            details: value,
        };
    }

    const v = value as any;

    // input
    if (!isInputOutput(v.input)) {
        return {
            valid: false,
            reason: "Invalid 'input' field",
            details: v.input,
        };
    }

    // output
    if (!isInputOutput(v.output)) {
        return {
            valid: false,
            reason: "Invalid 'output' field",
            details: v.output,
        };
    }

    // actionType
    if (!isActionType(v.actionType)) {
        return {
            valid: false,
            reason: "Invalid 'actionType'",
            details: v.actionType,
        };
    }

    // inputPoints
    if (!Array.isArray(v.inputPoints)) {
        return {
            valid: false,
            reason: "'inputPoints' must be an array",
            details: v.inputPoints,
        };
    }

    if (!v.inputPoints.every((n: unknown) => typeof n === "number")) {
        return {
            valid: false,
            reason: "'inputPoints' must contain only numbers",
            details: v.inputPoints,
        };
    }

    // outputPoints
    if (!Array.isArray(v.outputPoints)) {
        return {
            valid: false,
            reason: "'outputPoints' must be an array",
            details: v.outputPoints,
        };
    }

    if (!v.outputPoints.every((n: unknown) => typeof n === "number")) {
        return {
            valid: false,
            reason: "'outputPoints' must contain only numbers",
            details: v.outputPoints,
        };
    }

    // name
    if (typeof v.name !== "string") {
        return {
            valid: false,
            reason: "'name' must be a string",
            details: v.name,
        };
    }

    // description
    if (typeof v.description !== "string") {
        return {
            valid: false,
            reason: "'description' must be a string",
            details: v.description,
        };
    }

    // uid
    if (typeof v.uid !== "string") {
        return {
            valid: false,
            reason: "'uid' must be a string",
            details: v.uid,
        };
    }

    return {
        valid: true,
        value: v as MMAction,
    };
}

export type ValidationResult<T> = {
    valid: boolean;
    value?: T;
    reason?: string;
    details?: unknown;
};

const isValidInput = (
    inputs: number[],
    input: InputOutput
): ValidationResult<InputOutput> => {
    console.log("compare input:", inputs, input.values)
    const trueInputValueLen = inputs.filter(v => v != input.ignoreValue).length;
    if (trueInputValueLen !== input.values.filter(v => v.numericValue != input.ignoreValue).length) {
        return {
            valid: false,
            reason: "INPUT_LENGTH_MISMATCH",
            details: {
                expectedAmountOfInputNumbers: inputs.length,
                amountInputProviderValues: input.values.length,
                inputNumbers: inputs,
                inputProviderValues: input.values
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
): ValidationResult<InputOutput> => {
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

export const isValidAction = (action: MMAction): ValidationResult<any> => {
    const typeValidation = validateMMActionType(action);
    if (!typeValidation.valid) {
        return typeValidation
    }

    const inputValidation = isValidInput(action.inputPoints, action.input);
    if (!inputValidation.valid) {
        return inputValidation;
    }

    const outputValidation = isValidOutput(action.outputPoints, action.output);
    if (!outputValidation.valid) {
        return outputValidation;
    }

    return { valid: true };
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
