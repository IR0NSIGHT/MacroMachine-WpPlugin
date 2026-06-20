# ActionDTO

Represents an executable mapping action, that reads one inputtype from the map and based on that writes output values to the map.

## Properties

| Name             | Type                                |
| ---------------- | ----------------------------------- |
| `input`          | [InputOutputDTO](InputOutputDTO.md) |
| `output`         | [InputOutputDTO](InputOutputDTO.md) |
| `actionType`     | string                              |
| `name`           | string                              |
| `description`    | string                              |
| `uid`            | string                              |
| `mappingPointsX` | Array&lt;number&gt;                 |
| `mappingPointsY` | Array&lt;number&gt;                 |
| `mappedOutputs`  | Array&lt;number&gt;                 |
| `mappedInputs`   | Array&lt;number&gt;                 |

## Example

```typescript
import type { ActionDTO } from "";

// TODO: Update the object below with actual values
const example = {
  input: null,
  output: null,
  actionType: null,
  name: null,
  description: null,
  uid: null,
  mappingPointsX: null,
  mappingPointsY: null,
  mappedOutputs: null,
  mappedInputs: null,
} satisfies ActionDTO;

console.log(example);

// Convert the instance to a JSON string
const exampleJSON: string = JSON.stringify(example);
console.log(exampleJSON);

// Parse the JSON string back to an object
const exampleParsed = JSON.parse(exampleJSON) as ActionDTO;
console.log(exampleParsed);
```

[[Back to top]](#) [[Back to API list]](../README.md#api-endpoints) [[Back to Model list]](../README.md#models) [[Back to README]](../README.md)
