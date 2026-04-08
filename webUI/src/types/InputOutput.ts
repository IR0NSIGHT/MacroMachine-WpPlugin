export interface NamedValue {
  numericValue: number
  displayName: string
}

export interface ioParameter {
  name: string
  type: 'string' | 'number' | 'boolean' | 'select'
  options?: string[] | number[] | boolean[]
  value: string | number | boolean
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
