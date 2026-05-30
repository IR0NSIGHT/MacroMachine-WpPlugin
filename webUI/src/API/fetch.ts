import type {
  ActionDTO as GeneratedActionDTO,
  MacroDTO as GeneratedMacroDTO,
  ExecutionQueueDTO as GeneratedExecutionQueueDTO,
  ExecutionStateDTO as GeneratedExecutionStateDTO,
  ConfigurationParameters,
} from "../generated/client";
import {
  DefaultApi as GeneratedDefaultApi,
  Configuration as GeneratedConfiguration,
} from "../generated/client";
import { API_BASE } from "./api";
import { toCleanAction } from "./utils";

function safeCall<T>(fn: () => Promise<T>): Promise<T> {
  return fn().catch((e) => {
    if (e instanceof Error) throw new Error(`API error: ${e.message}`);
    throw e;
  });
}

export function createApi(config?: ConfigurationParameters) {
  const cfg = new GeneratedConfiguration({ basePath: API_BASE, ...config });
  return new GeneratedDefaultApi(cfg);
}

let api = createApi();

export function setApi(newApi: GeneratedDefaultApi) {
  api = newApi;
}

export { api };

type GetMacrosResponse = GeneratedMacroDTO[];
type GetActionsResponse = GeneratedActionDTO[];

export async function fetchMacros(): Promise<GetMacrosResponse> {
  return safeCall(() => api.getAllMacros());
}

export async function fetchActions(): Promise<GetActionsResponse> {
  return safeCall(() => api.getAllActions());
}

type ExecutionQueueDTO = GeneratedExecutionQueueDTO;

export async function postQueueMacros(macroIds: string[]): Promise<ExecutionQueueDTO> {
  return safeCall(() => api.addToQueue({ executionQueueDTO: { queuedMacroIds: macroIds } }));
}

export async function fetchExecutionQueue(): Promise<GeneratedExecutionQueueDTO> {
  return safeCall(() => api.getQueue());
}

export async function fetchExecutionState(): Promise<GeneratedExecutionStateDTO> {
  return safeCall(() => api.getCurrentState());
}

export async function deleteMacro(id: string): Promise<void> {
  return safeCall(() => api.deleteMacro({ id }));
}

export async function deleteAction(id: string): Promise<void> {
  return safeCall(() => api.deleteAction({ id }));
}

// #############################################
export async function postAction(action: GeneratedActionDTO): Promise<GeneratedActionDTO> {
  console.log(
    "post action with input params:",
    action.input.ioParameters,
    " output params:",
    action.output.ioParameters,
  );
  return safeCall(() => api.postAction({ actionDTO: toCleanAction(action) }));
}

export async function postActions(actions: GeneratedActionDTO[]): Promise<GeneratedActionDTO[]> {
  return Promise.all(
    actions.map((a) =>
      safeCall(() => {
        console.log(
          "post action with input params:",
          a.input.ioParameters,
          " output params:",
          a.output.ioParameters,
        );

        return api.postAction({ actionDTO: toCleanAction(a) });
      }),
    ),
  );
}

export async function postMacro(macro: GeneratedMacroDTO): Promise<GeneratedMacroDTO> {
  return safeCall(() => api.postMacro({ macroDTO: macro }));
}
