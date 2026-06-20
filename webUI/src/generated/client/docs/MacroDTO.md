# MacroDTO

Represents a macro: collection of macros and actions

## Properties

| Name             | Type                 |
| ---------------- | -------------------- |
| `executionUUIDs` | Array&lt;string&gt;  |
| `activeActions`  | Array&lt;boolean&gt; |
| `name`           | string               |
| `description`    | string               |
| `uid`            | string               |

## Example

```typescript
import type { MacroDTO } from ''

// TODO: Update the object below with actual values
const example = {
  "executionUUIDs": ["550e8400-e29b-41d4-a716-446655440000"],
  "activeActions": [true,false,true],
  "name": Morning Routine,
  "description": Runs all startup automation tasks,
  "uid": 550e8400-e29b-41d4-a716-446655440000,
} satisfies MacroDTO

console.log(example)

// Convert the instance to a JSON string
const exampleJSON: string = JSON.stringify(example)
console.log(exampleJSON)

// Parse the JSON string back to an object
const exampleParsed = JSON.parse(exampleJSON) as MacroDTO
console.log(exampleParsed)
```

[[Back to top]](#) [[Back to API list]](../README.md#api-endpoints) [[Back to Model list]](../README.md#models) [[Back to README]](../README.md)
