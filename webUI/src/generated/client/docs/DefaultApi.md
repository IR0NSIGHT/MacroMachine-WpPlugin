# DefaultApi

All URIs are relative to _http://localhost_

| Method                                                         | HTTP request                | Description                     |
| -------------------------------------------------------------- | --------------------------- | ------------------------------- |
| [**addToQueue**](DefaultApi.md#addtoqueue)                     | **POST** /execution/queue   |                                 |
| [**deleteAction**](DefaultApi.md#deleteaction)                 | **DELETE** /actions/{id}    |                                 |
| [**deleteMacro**](DefaultApi.md#deletemacro)                   | **DELETE** /macros/{id}     |                                 |
| [**existsLayerInProject**](DefaultApi.md#existslayerinproject) | **GET** /layers/{id}        |                                 |
| [**getActionById**](DefaultApi.md#getactionbyid)               | **GET** /actions/{id}       |                                 |
| [**getActionLastChange**](DefaultApi.md#getactionlastchange)   | **GET** /actions/lastChange | Get last modification timestamp |
| [**getAllActions**](DefaultApi.md#getallactions)               | **GET** /actions            |                                 |
| [**getAllLayers**](DefaultApi.md#getalllayers)                 | **GET** /layers             |                                 |
| [**getAllMacros**](DefaultApi.md#getallmacros)                 | **GET** /macros             | Get all macros                  |
| [**getAppliers**](DefaultApi.md#getappliers)                   | **GET** /actions/appliers   |                                 |
| [**getCurrentState**](DefaultApi.md#getcurrentstate)           | **GET** /execution/state    |                                 |
| [**getDocs**](DefaultApi.md#getdocs)                           | **GET** /docs               |                                 |
| [**getFilters**](DefaultApi.md#getfilters)                     | **GET** /actions/filters    |                                 |
| [**getLayerIcon**](DefaultApi.md#getlayericon)                 | **GET** /layers/{id}/icon   |                                 |
| [**getMacroById**](DefaultApi.md#getmacrobyid)                 | **GET** /macros/{id}        |                                 |
| [**getMacroLastChange**](DefaultApi.md#getmacrolastchange)     | **GET** /macros/lastChange  | Get last modification timestamp |
| [**getQueue**](DefaultApi.md#getqueue)                         | **GET** /execution/queue    |                                 |
| [**options**](DefaultApi.md#options)                           | **OPTIONS** /               |                                 |
| [**postAction**](DefaultApi.md#postaction)                     | **POST** /actions           |                                 |
| [**postMacro**](DefaultApi.md#postmacro)                       | **POST** /macros            |                                 |

## addToQueue

> ExecutionQueueDTO addToQueue(executionQueueDTO)

### Example

```ts
import {
  Configuration,
  DefaultApi,
} from '';
import type { AddToQueueRequest } from '';

async function example() {
  console.log("🚀 Testing  SDK...");
  const api = new DefaultApi();

  const body = {
    // ExecutionQueueDTO (optional)
    executionQueueDTO: ...,
  } satisfies AddToQueueRequest;

  try {
    const data = await api.addToQueue(body);
    console.log(data);
  } catch (error) {
    console.error(error);
  }
}

// Run the test
example().catch(console.error);
```

### Parameters

| Name                  | Type                                      | Description | Notes      |
| --------------------- | ----------------------------------------- | ----------- | ---------- |
| **executionQueueDTO** | [ExecutionQueueDTO](ExecutionQueueDTO.md) |             | [Optional] |

### Return type

[**ExecutionQueueDTO**](ExecutionQueueDTO.md)

### Authorization

No authorization required

### HTTP request headers

- **Content-Type**: `application/json`
- **Accept**: `application/json`

### HTTP response details

| Status code | Description      | Response headers |
| ----------- | ---------------- | ---------------- |
| **0**       | default response | -                |

[[Back to top]](#) [[Back to API list]](../README.md#api-endpoints) [[Back to Model list]](../README.md#models) [[Back to README]](../README.md)

## deleteAction

> deleteAction(id)

### Example

```ts
import {
  Configuration,
  DefaultApi,
} from '';
import type { DeleteActionRequest } from '';

async function example() {
  console.log("🚀 Testing  SDK...");
  const api = new DefaultApi();

  const body = {
    // string
    id: 38400000-8cf0-11bd-b23e-10b96e4ef00d,
  } satisfies DeleteActionRequest;

  try {
    const data = await api.deleteAction(body);
    console.log(data);
  } catch (error) {
    console.error(error);
  }
}

// Run the test
example().catch(console.error);
```

### Parameters

| Name   | Type     | Description | Notes                     |
| ------ | -------- | ----------- | ------------------------- |
| **id** | `string` |             | [Defaults to `undefined`] |

### Return type

`void` (Empty response body)

### Authorization

No authorization required

### HTTP request headers

- **Content-Type**: Not defined
- **Accept**: `application/json`

### HTTP response details

| Status code | Description      | Response headers |
| ----------- | ---------------- | ---------------- |
| **0**       | default response | -                |

[[Back to top]](#) [[Back to API list]](../README.md#api-endpoints) [[Back to Model list]](../README.md#models) [[Back to README]](../README.md)

## deleteMacro

> deleteMacro(id)

### Example

```ts
import {
  Configuration,
  DefaultApi,
} from '';
import type { DeleteMacroRequest } from '';

async function example() {
  console.log("🚀 Testing  SDK...");
  const api = new DefaultApi();

  const body = {
    // string
    id: 38400000-8cf0-11bd-b23e-10b96e4ef00d,
  } satisfies DeleteMacroRequest;

  try {
    const data = await api.deleteMacro(body);
    console.log(data);
  } catch (error) {
    console.error(error);
  }
}

// Run the test
example().catch(console.error);
```

### Parameters

| Name   | Type     | Description | Notes                     |
| ------ | -------- | ----------- | ------------------------- |
| **id** | `string` |             | [Defaults to `undefined`] |

### Return type

`void` (Empty response body)

### Authorization

No authorization required

### HTTP request headers

- **Content-Type**: Not defined
- **Accept**: `application/json`

### HTTP response details

| Status code | Description      | Response headers |
| ----------- | ---------------- | ---------------- |
| **0**       | default response | -                |

[[Back to top]](#) [[Back to API list]](../README.md#api-endpoints) [[Back to Model list]](../README.md#models) [[Back to README]](../README.md)

## existsLayerInProject

> boolean existsLayerInProject(id)

### Example

```ts
import { Configuration, DefaultApi } from "";
import type { ExistsLayerInProjectRequest } from "";

async function example() {
  console.log("🚀 Testing  SDK...");
  const api = new DefaultApi();

  const body = {
    // string
    id: id_example,
  } satisfies ExistsLayerInProjectRequest;

  try {
    const data = await api.existsLayerInProject(body);
    console.log(data);
  } catch (error) {
    console.error(error);
  }
}

// Run the test
example().catch(console.error);
```

### Parameters

| Name   | Type     | Description | Notes                     |
| ------ | -------- | ----------- | ------------------------- |
| **id** | `string` |             | [Defaults to `undefined`] |

### Return type

**boolean**

### Authorization

No authorization required

### HTTP request headers

- **Content-Type**: Not defined
- **Accept**: `application/json`

### HTTP response details

| Status code | Description      | Response headers |
| ----------- | ---------------- | ---------------- |
| **0**       | default response | -                |

[[Back to top]](#) [[Back to API list]](../README.md#api-endpoints) [[Back to Model list]](../README.md#models) [[Back to README]](../README.md)

## getActionById

> ActionDTO getActionById(id)

### Example

```ts
import {
  Configuration,
  DefaultApi,
} from '';
import type { GetActionByIdRequest } from '';

async function example() {
  console.log("🚀 Testing  SDK...");
  const api = new DefaultApi();

  const body = {
    // string
    id: 38400000-8cf0-11bd-b23e-10b96e4ef00d,
  } satisfies GetActionByIdRequest;

  try {
    const data = await api.getActionById(body);
    console.log(data);
  } catch (error) {
    console.error(error);
  }
}

// Run the test
example().catch(console.error);
```

### Parameters

| Name   | Type     | Description | Notes                     |
| ------ | -------- | ----------- | ------------------------- |
| **id** | `string` |             | [Defaults to `undefined`] |

### Return type

[**ActionDTO**](ActionDTO.md)

### Authorization

No authorization required

### HTTP request headers

- **Content-Type**: Not defined
- **Accept**: `application/json`

### HTTP response details

| Status code | Description      | Response headers |
| ----------- | ---------------- | ---------------- |
| **0**       | default response | -                |

[[Back to top]](#) [[Back to API list]](../README.md#api-endpoints) [[Back to Model list]](../README.md#models) [[Back to README]](../README.md)

## getActionLastChange

> number getActionLastChange()

Get last modification timestamp

Returns the timestamp of the most recent modification to the action container as milliseconds since the Unix epoch (equivalent to System.currentTimeMillis()).

### Example

```ts
import { Configuration, DefaultApi } from "";
import type { GetActionLastChangeRequest } from "";

async function example() {
  console.log("🚀 Testing  SDK...");
  const api = new DefaultApi();

  try {
    const data = await api.getActionLastChange();
    console.log(data);
  } catch (error) {
    console.error(error);
  }
}

// Run the test
example().catch(console.error);
```

### Parameters

This endpoint does not need any parameter.

### Return type

**number**

### Authorization

No authorization required

### HTTP request headers

- **Content-Type**: Not defined
- **Accept**: `application/json`

### HTTP response details

| Status code | Description      | Response headers |
| ----------- | ---------------- | ---------------- |
| **0**       | default response | -                |

[[Back to top]](#) [[Back to API list]](../README.md#api-endpoints) [[Back to Model list]](../README.md#models) [[Back to README]](../README.md)

## getAllActions

> Array&lt;ActionDTO&gt; getAllActions()

### Example

```ts
import { Configuration, DefaultApi } from "";
import type { GetAllActionsRequest } from "";

async function example() {
  console.log("🚀 Testing  SDK...");
  const api = new DefaultApi();

  try {
    const data = await api.getAllActions();
    console.log(data);
  } catch (error) {
    console.error(error);
  }
}

// Run the test
example().catch(console.error);
```

### Parameters

This endpoint does not need any parameter.

### Return type

[**Array&lt;ActionDTO&gt;**](ActionDTO.md)

### Authorization

No authorization required

### HTTP request headers

- **Content-Type**: Not defined
- **Accept**: `application/json`

### HTTP response details

| Status code | Description      | Response headers |
| ----------- | ---------------- | ---------------- |
| **0**       | default response | -                |

[[Back to top]](#) [[Back to API list]](../README.md#api-endpoints) [[Back to Model list]](../README.md#models) [[Back to README]](../README.md)

## getAllLayers

> Array&lt;LayerDTO&gt; getAllLayers()

### Example

```ts
import { Configuration, DefaultApi } from "";
import type { GetAllLayersRequest } from "";

async function example() {
  console.log("🚀 Testing  SDK...");
  const api = new DefaultApi();

  try {
    const data = await api.getAllLayers();
    console.log(data);
  } catch (error) {
    console.error(error);
  }
}

// Run the test
example().catch(console.error);
```

### Parameters

This endpoint does not need any parameter.

### Return type

[**Array&lt;LayerDTO&gt;**](LayerDTO.md)

### Authorization

No authorization required

### HTTP request headers

- **Content-Type**: Not defined
- **Accept**: `application/json`

### HTTP response details

| Status code | Description      | Response headers |
| ----------- | ---------------- | ---------------- |
| **0**       | default response | -                |

[[Back to top]](#) [[Back to API list]](../README.md#api-endpoints) [[Back to Model list]](../README.md#models) [[Back to README]](../README.md)

## getAllMacros

> Array&lt;MacroDTO&gt; getAllMacros()

Get all macros

### Example

```ts
import { Configuration, DefaultApi } from "";
import type { GetAllMacrosRequest } from "";

async function example() {
  console.log("🚀 Testing  SDK...");
  const api = new DefaultApi();

  try {
    const data = await api.getAllMacros();
    console.log(data);
  } catch (error) {
    console.error(error);
  }
}

// Run the test
example().catch(console.error);
```

### Parameters

This endpoint does not need any parameter.

### Return type

[**Array&lt;MacroDTO&gt;**](MacroDTO.md)

### Authorization

No authorization required

### HTTP request headers

- **Content-Type**: Not defined
- **Accept**: `application/json`

### HTTP response details

| Status code | Description    | Response headers |
| ----------- | -------------- | ---------------- |
| **200**     | List of macros | -                |

[[Back to top]](#) [[Back to API list]](../README.md#api-endpoints) [[Back to Model list]](../README.md#models) [[Back to README]](../README.md)

## getAppliers

> Array&lt;ActionDTO&gt; getAppliers()

### Example

```ts
import { Configuration, DefaultApi } from "";
import type { GetAppliersRequest } from "";

async function example() {
  console.log("🚀 Testing  SDK...");
  const api = new DefaultApi();

  try {
    const data = await api.getAppliers();
    console.log(data);
  } catch (error) {
    console.error(error);
  }
}

// Run the test
example().catch(console.error);
```

### Parameters

This endpoint does not need any parameter.

### Return type

[**Array&lt;ActionDTO&gt;**](ActionDTO.md)

### Authorization

No authorization required

### HTTP request headers

- **Content-Type**: Not defined
- **Accept**: `application/json`

### HTTP response details

| Status code | Description      | Response headers |
| ----------- | ---------------- | ---------------- |
| **0**       | default response | -                |

[[Back to top]](#) [[Back to API list]](../README.md#api-endpoints) [[Back to Model list]](../README.md#models) [[Back to README]](../README.md)

## getCurrentState

> ExecutionStateDTO getCurrentState()

### Example

```ts
import { Configuration, DefaultApi } from "";
import type { GetCurrentStateRequest } from "";

async function example() {
  console.log("🚀 Testing  SDK...");
  const api = new DefaultApi();

  try {
    const data = await api.getCurrentState();
    console.log(data);
  } catch (error) {
    console.error(error);
  }
}

// Run the test
example().catch(console.error);
```

### Parameters

This endpoint does not need any parameter.

### Return type

[**ExecutionStateDTO**](ExecutionStateDTO.md)

### Authorization

No authorization required

### HTTP request headers

- **Content-Type**: Not defined
- **Accept**: `application/json`

### HTTP response details

| Status code | Description      | Response headers |
| ----------- | ---------------- | ---------------- |
| **0**       | default response | -                |

[[Back to top]](#) [[Back to API list]](../README.md#api-endpoints) [[Back to Model list]](../README.md#models) [[Back to README]](../README.md)

## getDocs

> getDocs()

### Example

```ts
import { Configuration, DefaultApi } from "";
import type { GetDocsRequest } from "";

async function example() {
  console.log("🚀 Testing  SDK...");
  const api = new DefaultApi();

  try {
    const data = await api.getDocs();
    console.log(data);
  } catch (error) {
    console.error(error);
  }
}

// Run the test
example().catch(console.error);
```

### Parameters

This endpoint does not need any parameter.

### Return type

`void` (Empty response body)

### Authorization

No authorization required

### HTTP request headers

- **Content-Type**: Not defined
- **Accept**: `text/html`

### HTTP response details

| Status code | Description      | Response headers |
| ----------- | ---------------- | ---------------- |
| **0**       | default response | -                |

[[Back to top]](#) [[Back to API list]](../README.md#api-endpoints) [[Back to Model list]](../README.md#models) [[Back to README]](../README.md)

## getFilters

> Array&lt;ActionDTO&gt; getFilters()

### Example

```ts
import { Configuration, DefaultApi } from "";
import type { GetFiltersRequest } from "";

async function example() {
  console.log("🚀 Testing  SDK...");
  const api = new DefaultApi();

  try {
    const data = await api.getFilters();
    console.log(data);
  } catch (error) {
    console.error(error);
  }
}

// Run the test
example().catch(console.error);
```

### Parameters

This endpoint does not need any parameter.

### Return type

[**Array&lt;ActionDTO&gt;**](ActionDTO.md)

### Authorization

No authorization required

### HTTP request headers

- **Content-Type**: Not defined
- **Accept**: `application/json`

### HTTP response details

| Status code | Description      | Response headers |
| ----------- | ---------------- | ---------------- |
| **0**       | default response | -                |

[[Back to top]](#) [[Back to API list]](../README.md#api-endpoints) [[Back to Model list]](../README.md#models) [[Back to README]](../README.md)

## getLayerIcon

> getLayerIcon(id)

### Example

```ts
import { Configuration, DefaultApi } from "";
import type { GetLayerIconRequest } from "";

async function example() {
  console.log("🚀 Testing  SDK...");
  const api = new DefaultApi();

  const body = {
    // string
    id: id_example,
  } satisfies GetLayerIconRequest;

  try {
    const data = await api.getLayerIcon(body);
    console.log(data);
  } catch (error) {
    console.error(error);
  }
}

// Run the test
example().catch(console.error);
```

### Parameters

| Name   | Type     | Description | Notes                     |
| ------ | -------- | ----------- | ------------------------- |
| **id** | `string` |             | [Defaults to `undefined`] |

### Return type

`void` (Empty response body)

### Authorization

No authorization required

### HTTP request headers

- **Content-Type**: Not defined
- **Accept**: `image/png`

### HTTP response details

| Status code | Description      | Response headers |
| ----------- | ---------------- | ---------------- |
| **0**       | default response | -                |

[[Back to top]](#) [[Back to API list]](../README.md#api-endpoints) [[Back to Model list]](../README.md#models) [[Back to README]](../README.md)

## getMacroById

> MacroDTO getMacroById(id)

### Example

```ts
import {
  Configuration,
  DefaultApi,
} from '';
import type { GetMacroByIdRequest } from '';

async function example() {
  console.log("🚀 Testing  SDK...");
  const api = new DefaultApi();

  const body = {
    // string
    id: 38400000-8cf0-11bd-b23e-10b96e4ef00d,
  } satisfies GetMacroByIdRequest;

  try {
    const data = await api.getMacroById(body);
    console.log(data);
  } catch (error) {
    console.error(error);
  }
}

// Run the test
example().catch(console.error);
```

### Parameters

| Name   | Type     | Description | Notes                     |
| ------ | -------- | ----------- | ------------------------- |
| **id** | `string` |             | [Defaults to `undefined`] |

### Return type

[**MacroDTO**](MacroDTO.md)

### Authorization

No authorization required

### HTTP request headers

- **Content-Type**: Not defined
- **Accept**: `application/json`

### HTTP response details

| Status code | Description      | Response headers |
| ----------- | ---------------- | ---------------- |
| **0**       | default response | -                |

[[Back to top]](#) [[Back to API list]](../README.md#api-endpoints) [[Back to Model list]](../README.md#models) [[Back to README]](../README.md)

## getMacroLastChange

> number getMacroLastChange()

Get last modification timestamp

Returns the timestamp of the most recent modification to the macro container as milliseconds since the Unix epoch (equivalent to System.currentTimeMillis()).

### Example

```ts
import { Configuration, DefaultApi } from "";
import type { GetMacroLastChangeRequest } from "";

async function example() {
  console.log("🚀 Testing  SDK...");
  const api = new DefaultApi();

  try {
    const data = await api.getMacroLastChange();
    console.log(data);
  } catch (error) {
    console.error(error);
  }
}

// Run the test
example().catch(console.error);
```

### Parameters

This endpoint does not need any parameter.

### Return type

**number**

### Authorization

No authorization required

### HTTP request headers

- **Content-Type**: Not defined
- **Accept**: `application/json`

### HTTP response details

| Status code | Description      | Response headers |
| ----------- | ---------------- | ---------------- |
| **0**       | default response | -                |

[[Back to top]](#) [[Back to API list]](../README.md#api-endpoints) [[Back to Model list]](../README.md#models) [[Back to README]](../README.md)

## getQueue

> ExecutionQueueDTO getQueue()

### Example

```ts
import { Configuration, DefaultApi } from "";
import type { GetQueueRequest } from "";

async function example() {
  console.log("🚀 Testing  SDK...");
  const api = new DefaultApi();

  try {
    const data = await api.getQueue();
    console.log(data);
  } catch (error) {
    console.error(error);
  }
}

// Run the test
example().catch(console.error);
```

### Parameters

This endpoint does not need any parameter.

### Return type

[**ExecutionQueueDTO**](ExecutionQueueDTO.md)

### Authorization

No authorization required

### HTTP request headers

- **Content-Type**: Not defined
- **Accept**: `application/json`

### HTTP response details

| Status code | Description      | Response headers |
| ----------- | ---------------- | ---------------- |
| **0**       | default response | -                |

[[Back to top]](#) [[Back to API list]](../README.md#api-endpoints) [[Back to Model list]](../README.md#models) [[Back to README]](../README.md)

## options

> options()

### Example

```ts
import { Configuration, DefaultApi } from "";
import type { OptionsRequest } from "";

async function example() {
  console.log("🚀 Testing  SDK...");
  const api = new DefaultApi();

  try {
    const data = await api.options();
    console.log(data);
  } catch (error) {
    console.error(error);
  }
}

// Run the test
example().catch(console.error);
```

### Parameters

This endpoint does not need any parameter.

### Return type

`void` (Empty response body)

### Authorization

No authorization required

### HTTP request headers

- **Content-Type**: Not defined
- **Accept**: `*/*`

### HTTP response details

| Status code | Description      | Response headers |
| ----------- | ---------------- | ---------------- |
| **0**       | default response | -                |

[[Back to top]](#) [[Back to API list]](../README.md#api-endpoints) [[Back to Model list]](../README.md#models) [[Back to README]](../README.md)

## postAction

> ActionDTO postAction(actionDTO)

### Example

```ts
import {
  Configuration,
  DefaultApi,
} from '';
import type { PostActionRequest } from '';

async function example() {
  console.log("🚀 Testing  SDK...");
  const api = new DefaultApi();

  const body = {
    // ActionDTO (optional)
    actionDTO: ...,
  } satisfies PostActionRequest;

  try {
    const data = await api.postAction(body);
    console.log(data);
  } catch (error) {
    console.error(error);
  }
}

// Run the test
example().catch(console.error);
```

### Parameters

| Name          | Type                      | Description | Notes      |
| ------------- | ------------------------- | ----------- | ---------- |
| **actionDTO** | [ActionDTO](ActionDTO.md) |             | [Optional] |

### Return type

[**ActionDTO**](ActionDTO.md)

### Authorization

No authorization required

### HTTP request headers

- **Content-Type**: `application/json`
- **Accept**: `application/json`

### HTTP response details

| Status code | Description      | Response headers |
| ----------- | ---------------- | ---------------- |
| **0**       | default response | -                |

[[Back to top]](#) [[Back to API list]](../README.md#api-endpoints) [[Back to Model list]](../README.md#models) [[Back to README]](../README.md)

## postMacro

> MacroDTO postMacro(macroDTO)

### Example

```ts
import {
  Configuration,
  DefaultApi,
} from '';
import type { PostMacroRequest } from '';

async function example() {
  console.log("🚀 Testing  SDK...");
  const api = new DefaultApi();

  const body = {
    // MacroDTO (optional)
    macroDTO: ...,
  } satisfies PostMacroRequest;

  try {
    const data = await api.postMacro(body);
    console.log(data);
  } catch (error) {
    console.error(error);
  }
}

// Run the test
example().catch(console.error);
```

### Parameters

| Name         | Type                    | Description | Notes      |
| ------------ | ----------------------- | ----------- | ---------- |
| **macroDTO** | [MacroDTO](MacroDTO.md) |             | [Optional] |

### Return type

[**MacroDTO**](MacroDTO.md)

### Authorization

No authorization required

### HTTP request headers

- **Content-Type**: `application/json`
- **Accept**: `application/json`

### HTTP response details

| Status code | Description      | Response headers |
| ----------- | ---------------- | ---------------- |
| **0**       | default response | -                |

[[Back to top]](#) [[Back to API list]](../README.md#api-endpoints) [[Back to Model list]](../README.md#models) [[Back to README]](../README.md)
