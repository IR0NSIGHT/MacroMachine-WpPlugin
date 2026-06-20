// src/mocks/handlers.ts
// Centralized handler exports combining all modular handlers

import { actionHandlers } from "./handlers/actions";
import { executionHandlers } from "./handlers/executions";
import { layerHandlers } from "./handlers/layers";
import { macroHandlers } from "./handlers/macros";
import { utilHandlers } from "./handlers/utils";

/**
 * Combined handlers for all endpoints defined in openapi.json
 * Organized by DTO type for maintainability
 */
export const handlers = [
  ...actionHandlers,
  ...executionHandlers,
  ...layerHandlers,
  ...macroHandlers,
  ...utilHandlers,
];
