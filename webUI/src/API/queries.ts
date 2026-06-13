// ======================================================
// Query Keys
// ======================================================
// queries.ts
import { useQuery, useMutation, useQueryClient, type UseQueryOptions } from "@tanstack/react-query";
import { MacroDTO, ActionDTO, ExecutionQueueDTO, ExecutionStateDTO } from "@/generated/client";
import {
  fetchMacros,
  fetchActions,
  fetchExecutionQueue,
  postQueueMacros,
  postAction,
  postActions,
  postMacro,
  deleteMacro,
  deleteAction,
  api,
  fetchExecutionState,
} from "./fetch";

export const queryKeys = {
  macros: ["macros"] as const,
  actions: ["actions"] as const,
  executionQueue: ["executionQueue"] as const,
  executionState: ["executionState"] as const,
};

// ======================================================
// Queries
// ======================================================

export function useMacrosQuery(
  options?: Omit<UseQueryOptions<MacroDTO[]>, "queryKey" | "queryFn">,
) {
  return useQuery({
    queryKey: queryKeys.macros,
    queryFn: fetchMacros,
    ...options,
  });
}

export function useActionsQuery(
  options?: Omit<UseQueryOptions<ActionDTO[]>, "queryKey" | "queryFn">,
) {
  return useQuery({
    queryKey: queryKeys.actions,
    queryFn: fetchActions,
    ...options,
  });
}

export function useExecutionQueueQuery(
  options?: Omit<UseQueryOptions<ExecutionQueueDTO>, "queryKey" | "queryFn">,
) {
  return useQuery({
    queryKey: queryKeys.executionQueue,
    queryFn: fetchExecutionQueue,
    refetchInterval: 1000,
    refetchIntervalInBackground: true,
    ...options,
  });
}

export function useExecutionHistoryQuery(
  options?: Omit<UseQueryOptions<ExecutionStateDTO[]>, "queryKey" | "queryFn">,
) {
  return useQuery({
    queryKey: ["executionHistory"],
    queryFn: () => api.getExecutionHistory(),
    refetchInterval: 500,
    ...options,
  });
}

export function useExecutionStateQuery(
  options?: Omit<UseQueryOptions<ExecutionStateDTO>, "queryKey" | "queryFn">,
) {
  return useQuery({
    queryKey: queryKeys.executionState,
    queryFn: fetchExecutionState,
    refetchInterval: 100,
    refetchIntervalInBackground: true,
    ...options,
  });
}

export function useActionLastChangeQuery() {
  return useQuery({
    queryKey: ["actions", "lastChange"],
    queryFn: () => api.getActionLastChange(),
    refetchInterval: 5000,
  });
}

export function useMacroLastChangeQuery() {
  return useQuery({
    queryKey: ["macros", "lastChange"],
    queryFn: () => api.getMacroLastChange(),
    refetchInterval: 5000,
  });
}

export function useLayersQuery() {
  return useQuery({
    queryKey: ["layers"],
    queryFn: () => api.getAllLayers(),
    refetchInterval: 5000,
  });
}

// ======================================================
// Mutations
// ======================================================

export function useQueueMacrosMutation() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: (macroIds: string[]) => postQueueMacros(macroIds),
    onSuccess: () => {
      queryClient.invalidateQueries({
        queryKey: queryKeys.executionQueue,
      });

      queryClient.invalidateQueries({
        queryKey: queryKeys.executionState,
      });
    },
  });
}

export function usePostActionMutation() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: (action: ActionDTO) => postAction(action),
    onSuccess: () => {
      queryClient.invalidateQueries({
        queryKey: queryKeys.actions,
      });
    },
  });
}

export function usePostActionsMutation() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: (actions: ActionDTO[]) => postActions(actions),
    onSuccess: () => {
      queryClient.invalidateQueries({
        queryKey: queryKeys.actions,
      });
    },
  });
}

export function usePostMacroMutation() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: (macro: MacroDTO) => postMacro(macro),
    onSuccess: () => {
      queryClient.invalidateQueries({
        queryKey: queryKeys.macros,
      });
    },
  });
}

export function useDeleteMacroMutation() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: (id: string) => deleteMacro(id),
    onSuccess: () => {
      queryClient.invalidateQueries({
        queryKey: queryKeys.macros,
      });
    },
  });
}

export function useDeleteActionMutation() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: (id: string) => deleteAction(id),
    onSuccess: () => {
      queryClient.invalidateQueries({
        queryKey: queryKeys.actions,
      });
    },
  });
}
