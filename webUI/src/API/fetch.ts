import { ActionDTO, MacroDTO } from "@/types/DTO";
import type { paths, components, operations } from "../generated/api-types";
import { API_BASE } from "./api";

type GetMacrosResponse = paths["/macros"]["get"]["responses"]["200"]["content"]["application/json"];
type GetActionsResponse =
  paths["/actions"]["get"]["responses"]["default"]["content"]["application/json"];

export async function fetchMacros(): Promise<GetMacrosResponse> {
  const response = await fetch(`${API_BASE}/api/macros`);

  if (!response.ok) {
    throw new Error(`Failed to fetch macros: ${response.status}`);
  }

  return response.json();
}

export async function fetchActions(): Promise<GetActionsResponse> {
  const response = await fetch(`${API_BASE}/api/actions`);

  if (!response.ok) {
    throw new Error(`Failed to fetch actions: ${response.status}`);
  }

  return response.json();
}

type ExecutionQueueDTO = components["schemas"]["ExecutionQueueDTO"];

export async function postQueueMacros(macroIds: string[]): Promise<ExecutionQueueDTO> {
  const response = await fetch(`${API_BASE}/api/execution/queue`, {
    method: "POST",
    headers: {
      "Content-Type": "application/json",
      Accept: "application/json",
    },
    body: JSON.stringify({
      queuedMacroIds: macroIds,
    }),
  });

  if (!response.ok) {
    throw new Error(`Failed to queue macros: ${response.status} ${response.statusText}`);
  }

  return await response.json();
}

export async function fetchExecutionQueue(): Promise<components["schemas"]["ExecutionQueueDTO"]> {
  const response = await fetch(`${API_BASE}/api/execution/queue`, {
    method: "GET",
    headers: {
      Accept: "application/json",
    },
  });

  if (!response.ok) {
    throw new Error(`Failed to fetch queue: ${response.status} ${response.statusText}`);
  }

  return await response.json();
}

export async function fetchExecutionState(): Promise<components["schemas"]["ExecutionStateDTO"]> {
  const response = await fetch(`${API_BASE}/api/execution/state`, {
    method: "GET",
    headers: {
      Accept: "application/json",
    },
  });

  if (!response.ok) {
    throw new Error(`Failed to fetch state: ${response.status} ${response.statusText}`);
  }

  return await response.json();
}

export const toCleanAction = (action: ActionDTO): ActionDTO => {
  const cleanCopy: ActionDTO = {
    name: action.name,
    description: action.description,
    uid: action.uid,
    input: action.input,
    output: action.output,
    mappedInputs: action.mappedInputs,
    mappedOutputs: action.mappedOutputs,
    mappingPointsX: action.mappingPointsX,
    mappingPointsY: action.mappingPointsY,
    actionType: action.actionType,
  };
  return cleanCopy;
};

// #############################################
type _CreateActionBody = NonNullable<
  NonNullable<operations["create"]["requestBody"]>["content"]
>["application/json"];
export async function postAction(action: ActionDTO): Promise<ActionDTO> {
  const response = await fetch(`${API_BASE}/api/actions`, {
    method: "POST",
    headers: {
      "Content-Type": "application/json",
      Accept: "application/json",
    },
    body: JSON.stringify(toCleanAction(action)),
  });

  if (!response.ok) {
    console.error(`Failed to create action: ${response.status} ${response.statusText}`);
    throw new Error(`Failed to create action: ${response.status} ${response.statusText}`);
  }

  return await response.json();
}

export async function postActions(actions: ActionDTO[]): Promise<ActionDTO[]> {
  return Promise.all(actions.map(postAction));
}

export async function postMacro(macro: MacroDTO): Promise<MacroDTO> {
  const response = await fetch(`${API_BASE}/api/macros`, {
    method: "POST",
    headers: {
      "Content-Type": "application/json",
      Accept: "application/json",
    },
    body: JSON.stringify(macro),
  });

  if (!response.ok) {
    const message = await response.text();
    console.error(`Failed to create macro: ${response.status} ${response.statusText}`);
    throw new Error(message || `Failed to create macro: ${response.status} ${response.statusText}`);
  }

  return await response.json();
}
