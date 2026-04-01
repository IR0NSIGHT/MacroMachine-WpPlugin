import { InputOutput } from "@/types"

export interface MMAction {
  input: InputOutput
  output: InputOutput
  actionType: string
  inputPoints: number[]
  outputPoints: number[]
  name: string
  description: string
  uid: string
}
