export interface NamedValue {
  numericValue: number
  displayName: string
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
  parameters: (string|number|number[])[]
}
