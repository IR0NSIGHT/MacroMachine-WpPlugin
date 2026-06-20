# ExecutionStepDTO

Represents a single action step within a macro execution

## Properties

| Name              | Type    |
| ----------------- | ------- |
| `actionId`        | string  |
| `breakpoint`      | boolean |
| `percentComplete` | number  |
| `status`          | string  |
| `error`           | string  |

## Example

```typescript
import type { ExecutionStepDTO } from ''

// TODO: Update the object below with actual values
const example = {
  "actionId": 550e8400-e29b-41d4-a716-446655440000,
  "breakpoint": false,
  "percentComplete": 42.5,
  "status": null,
  "error": null,
} satisfies ExecutionStepDTO

console.log(example)

// Convert the instance to a JSON string
const exampleJSON: string = JSON.stringify(example)
console.log(exampleJSON)

// Parse the JSON string back to an object
const exampleParsed = JSON.parse(exampleJSON) as ExecutionStepDTO
console.log(exampleParsed)
```

[[Back to top]](#) [[Back to API list]](../README.md#api-endpoints) [[Back to Model list]](../README.md#models) [[Back to README]](../README.md)
