# InputOutputDTO

Describes an input/output provider configuration

## Properties

| Name                | Type                                                                               |
| ------------------- | ---------------------------------------------------------------------------------- |
| `displayName`       | string                                                                             |
| `description`       | string                                                                             |
| `min`               | number                                                                             |
| `max`               | number                                                                             |
| `ignoreValue`       | number                                                                             |
| `valueDisplayNames` | Array&lt;string&gt;                                                                |
| `colors`            | Array&lt;number&gt;                                                                |
| `iconName`          | string                                                                             |
| `discrete`          | boolean                                                                            |
| `type`              | string                                                                             |
| `ioParameters`      | [Array&lt;InputOutputDTOIoParametersInner&gt;](InputOutputDTOIoParametersInner.md) |

## Example

```typescript
import type { InputOutputDTO } from ''

// TODO: Update the object below with actual values
const example = {
  "displayName": Terrain Height,
  "description": Controls the generated terrain elevation,
  "min": 0,
  "max": 255,
  "ignoreValue": -1,
  "valueDisplayNames": null,
  "colors": null,
  "iconName": droplet,
  "discrete": true,
  "type": null,
  "ioParameters": null,
} satisfies InputOutputDTO

console.log(example)

// Convert the instance to a JSON string
const exampleJSON: string = JSON.stringify(example)
console.log(exampleJSON)

// Parse the JSON string back to an object
const exampleParsed = JSON.parse(exampleJSON) as InputOutputDTO
console.log(exampleParsed)
```

[[Back to top]](#) [[Back to API list]](../README.md#api-endpoints) [[Back to Model list]](../README.md#models) [[Back to README]](../README.md)
