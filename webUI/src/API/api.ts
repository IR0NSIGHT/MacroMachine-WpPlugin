import { Macro, UUID } from "@/types/MMacro";
import { MappingPointDTO, MMAction } from "@/types/MMAction";
import { useState, useEffect } from "react";

export const API_BASE = "http://localhost:8080";

async function safeJsonFetch<T>(url: string): Promise<T> {
  const res = await fetch(url);

  const text = await res.text();

  if (!res.ok) {
    console.error(`Error fetching ${url}:`, text);
    throw new Error(`HTTP ${res.status}: ${text}`);
  }

  if (!text) {
    throw new Error(`Empty response from ${url}`);
  }

  return JSON.parse(text);
}

async function fetchMacroList() {
  return safeJsonFetch<UUID[]>(`${API_BASE}/macroList`);
}

export async function fetchMacro(uuid: string) {
  return safeJsonFetch<Macro>(`${API_BASE}/macro?uuid=${uuid}`);
}

async function fetchAction(uuid: string) {
  return safeJsonFetch<MMAction>(`${API_BASE}/action?uuid=${uuid}`);
}

/**
 * request recalculated action from backend using given mappingpoints.
 */
export const fetchActionWithPoints = (uuid: string, _mappingPoints: MappingPointDTO[]): Promise<MMAction> => {
  console.log("fetching action with points:", uuid, _mappingPoints);
  return safeJsonFetch<MMAction>(`${API_BASE}/action?uuid=${uuid}&points=${encodeURIComponent(JSON.stringify(_mappingPoints))}`);
}


export function useMacroSystem(selectedMacroUuid?: string) {
  const [macroUuids, setMacroUuids] = useState<UUID[]>([]);
  const [macros, setMacros] = useState<Macro[]>([]);

  const [actionUuids, setActionUuids] = useState<UUID[]>([]);
  const [actions, setActions] = useState<MMAction[]>([]);

  const [loading, setLoading] = useState(false);

  // 1. load UUID list
  useEffect(() => {
    fetchMacroList().then(setMacroUuids);
  }, []);

  // 2. load FULL macros (THIS WAS MISSING)
  useEffect(() => {
    if (macroUuids.length === 0) {
      setMacros([]);
      return;
    }

    setLoading(true);

    Promise.all(macroUuids.map(fetchMacro))
      .then(setMacros)
      .finally(() => setLoading(false));
  }, [macroUuids]);

  // 3. load macro → action UUIDs
  useEffect(() => {
    if (!selectedMacroUuid) return;

    setLoading(true);

    fetchMacro(selectedMacroUuid)
      .then((macro) => setActionUuids(macro.executionUUIDs))
      .finally(() => setLoading(false));
  }, [selectedMacroUuid]);

  // 4. load actions
  useEffect(() => {
    if (actionUuids.length === 0) {
      setActions([]);
      return;
    }

    setLoading(true);

    Promise.allSettled(actionUuids.map(fetchAction))
      .then((results) => {
        const ok = results
          .filter((r): r is PromiseFulfilledResult<MMAction> => r.status === "fulfilled")
          .map(r => r.value);

        const failed = results.filter(r => r.status === "rejected");

        if (failed.length > 0) {
          console.warn("Some actions failed:", failed);
        }

        setActions(ok);
      })
      .finally(() => setLoading(false));
  }, [actionUuids]);

  return {
    macros,
    actions,
    loading,
  };
}