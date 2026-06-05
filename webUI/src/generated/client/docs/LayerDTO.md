# LayerDTO

A layer used in worldpainter.

## Properties

| Name          | Type    |
| ------------- | ------- |
| `name`        | string  |
| `description` | string  |
| `dataSize`    | string  |
| `priority`    | number  |
| `id`          | string  |
| `discrete`    | boolean |
| `type`        | string  |
| `custom`      | boolean |

## Example

```typescript
import type { LayerDTO } from "";

// TODO: Update the object below with actual values
const example = {
  name: null,
  description: null,
  dataSize: null,
  priority: null,
  id: null,
  discrete: null,
  type: null,
  custom: null,
} satisfies LayerDTO;

console.log(example);

// Convert the instance to a JSON string
const exampleJSON: string = JSON.stringify(example);
console.log(exampleJSON);

// Parse the JSON string back to an object
const exampleParsed = JSON.parse(exampleJSON) as LayerDTO;
console.log(exampleParsed);
```

[[Back to top]](#) [[Back to API list]](../README.md#api-endpoints) [[Back to Model list]](../README.md#models) [[Back to README]](../README.md)
