# ExecutionStateDTO

Represents the application\'s execution state

## Properties

| Name               | Type                                                 |
| ------------------ | ---------------------------------------------------- |
| `executionId`      | string                                               |
| `steps`            | [Array&lt;ExecutionStepDTO&gt;](ExecutionStepDTO.md) |
| `currentStepIndex` | number                                               |
| `status`           | string                                               |

## Example

```typescript
import type { ExecutionStateDTO } from "";

// TODO: Update the object below with actual values
const example = {
  executionId: null,
  steps: null,
  currentStepIndex: 0,
  status: null,
} satisfies ExecutionStateDTO;

console.log(example);

// Convert the instance to a JSON string
const exampleJSON: string = JSON.stringify(example);
console.log(exampleJSON);

// Parse the JSON string back to an object
const exampleParsed = JSON.parse(exampleJSON) as ExecutionStateDTO;
console.log(exampleParsed);
```

[[Back to top]](#) [[Back to API list]](../README.md#api-endpoints) [[Back to Model list]](../README.md#models) [[Back to README]](../README.md)
