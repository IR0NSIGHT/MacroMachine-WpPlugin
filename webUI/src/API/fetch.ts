import { ActionDTO, MacroDTO } from "@/types/DTO";
import type {
  ActionDTO as GeneratedActionDTO,
  MacroDTO as GeneratedMacroDTO,
  ExecutionQueueDTO as GeneratedExecutionQueueDTO,
  ExecutionStateDTO as GeneratedExecutionStateDTO,
} from "../generated/client";
import { API_BASE } from "./api";

type GetMacrosResponse = GeneratedMacroDTO[];
type GetActionsResponse = GeneratedActionDTO[];

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

type ExecutionQueueDTO = GeneratedExecutionQueueDTO;

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

export async function fetchExecutionQueue(): Promise<GeneratedExecutionQueueDTO> {
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

export async function fetchExecutionState(): Promise<GeneratedExecutionStateDTO> {
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

export async function deleteMacro(id: string): Promise<void> {
  const response = await fetch(`${API_BASE}/api/macros/${id}`, {
    method: "DELETE",
  });

  if (!response.ok) {
    throw new Error(`Failed to delete macro: ${response.status}`);
  }
}

export async function deleteAction(id: string): Promise<void> {
  const response = await fetch(`${API_BASE}/api/actions/${id}`, {
    method: "DELETE",
  });

  if (!response.ok) {
    throw new Error(`Failed to delete action: ${response.status}`);
  }
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
