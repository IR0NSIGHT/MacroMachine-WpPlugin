import { InputOutput } from "@/types/InputOutput"

export type ActionType =
  | "increment"
  | "subtract"
  | "multiply"
  | "divide"
  | "set"
  | "limit"
  | "set minimum";

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
