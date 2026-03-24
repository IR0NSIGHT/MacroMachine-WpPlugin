export interface MMAction {
  inputId: string
  inputData: unknown[]
  outputId: string
  outputData: unknown[]
  actionType: string
  inputPoints: number[]
  outputPoints: number[]
  name: string
  description: string
  uid: string
}
