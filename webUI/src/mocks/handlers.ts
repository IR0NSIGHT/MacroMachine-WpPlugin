// src/mocks/handlers.ts
import { http, HttpResponse } from "msw";
import { ActionDTO, MacroDTO, ExecutionQueueDTO, ExecutionStateDTO } from "@/types/DTO";

import mock_actions from "./data/actions.json";
import mock_macros from "./data/macros.json";
import mock_state from "./data/state.json";
import mock_queue from "./data/queue.json";
import defaultApplyActions from "@/assets/defaultApplyActions.json";
import defaultFilters from "@/assets/defaultFilters.json";

/**
 * Replace these with your actual hardcoded JSON fixtures later
 */
const actions: ActionDTO[] = mock_actions as ActionDTO[];
const macros: MacroDTO[] = mock_macros;
const applyActions: ActionDTO[] = defaultApplyActions as ActionDTO[];
const filters: ActionDTO[] = defaultFilters as ActionDTO[];

let executionQueue: ExecutionQueueDTO = mock_queue;

// Mutable execution state
let executionState: ExecutionStateDTO = structuredClone(mock_state as any);

let isProcessingQueue = false;

export const handlers = [
  /**
   * GET /actions
   */
  http.get("/api/actions", () => {
    return HttpResponse.json(actions);
  }),

  /**
   * GET /actions/filters
   */
  http.get("/api/actions/filters", () => {
    return HttpResponse.json(filters);
  }),

  /**
   * GET /actions/appliers
   */
  http.get("/api/actions/appliers", () => {
    return HttpResponse.json(applyActions);
  }),

  /**
   * POST /actions
   */
  http.post("/api/actions", async ({ request }) => {
    const body = (await request.json()) as ActionDTO;

    const created: ActionDTO = {
      ...body,
      uid: body.uid || crypto.randomUUID(),
    };

    actions.push(created);

    return HttpResponse.json(created, {
      status: 201,
    });
  }),

  /**
   * GET /actions/:id
   */
  http.get("/api/actions/:id", ({ params }) => {
    const action = actions.find((a) => a.uid === params.id);

    if (!action) {
      return HttpResponse.json({ message: "Action not found" }, { status: 404 });
    }

    return HttpResponse.json(action);
  }),

  /**
   * DELETE /actions/:id
   */
  http.delete("/api/actions/:id", ({ params }) => {
    const index = actions.findIndex((a) => a.uid === params.id);

    if (index === -1) {
      return HttpResponse.json({ message: "Action not found" }, { status: 404 });
    }

    actions.splice(index, 1);

    return HttpResponse.json({
      success: true,
    });
  }),

  /**
   * GET /docs
   */
  http.get("/api/docs", () => {
    return new HttpResponse(
      `
      <html>
        <body>
          <h1>Mock Swagger Docs</h1>
        </body>
      </html>
      `,
      {
        headers: {
          "Content-Type": "text/html",
        },
      },
    );
  }),

  /**
   * GET /execution/state
   */
  http.get("/api/execution/state", () => {
    return HttpResponse.json(executionState);
  }),

  /**
   * GET /execution/queue
   */
  http.get("/api/execution/queue", () => {
    return HttpResponse.json(executionQueue);
  }),

  /**
   * POST /execution/queue
   */
  http.post("/api/execution/queue", async ({ request }) => {
    const body = (await request.json()) as ExecutionQueueDTO;

    executionQueue = {
      ...executionQueue,
      queuedMacroIds: executionQueue.queuedMacroIds.concat(body.queuedMacroIds),
    };

    // background processing
    void processQueue();

    return HttpResponse.json(executionQueue);
  }),

  /**
   * GET /macros
   */
  http.get("/api/macros", () => {
    return HttpResponse.json(macros);
  }),

  /**
   * POST /macros
   */
  http.post("/api/macros", async ({ request }) => {
    const body = (await request.json()) as MacroDTO;

    const created: MacroDTO = {
      ...body,
      uid: body.uid || crypto.randomUUID(),
    };

    macros.push(created);

    return HttpResponse.json(created, {
      status: 201,
    });
  }),

  /**
   * GET /macros/:id
   */
  http.get("/api/macros/:id", ({ params }) => {
    const macro = macros.find((m) => m.uid === params.id);

    if (!macro) {
      return HttpResponse.json({ message: "Macro not found" }, { status: 404 });
    }

    return HttpResponse.json(macro);
  }),

  /**
   * DELETE /macros/:id
   */
  http.delete("/api/macros/:id", ({ params }) => {
    const index = macros.findIndex((m) => m.uid === params.id);

    if (index === -1) {
      return HttpResponse.json({ message: "Macro not found" }, { status: 404 });
    }

    macros.splice(index, 1);

    return HttpResponse.json({
      success: true,
    });
  }),

  /**
   * OPTIONS /
   */
  http.options("/", () => {
    return new HttpResponse(null, {
      status: 204,
      headers: {
        Allow: "OPTIONS",
      },
    });
  }),
];

async function processQueue() {
  if (isProcessingQueue) {
    return;
  }

  isProcessingQueue = true;

  while (executionQueue.queuedMacroIds.length > 0) {
    const macroId = executionQueue.queuedMacroIds.shift();

    if (!macroId) {
      continue;
    }

    const macro = macros.find((m) => m.uid === macroId);

    if (!macro) {
      continue;
    }

    const stepIds = macro.executionUUIDs;

    executionState = {
      executionId: macro.uid,
      currentStepIndex: 0,
      status: "RUNNING",
      steps: stepIds.map((id) => ({
        actionId: id,
        breakpoint: false,
        percentComplete: 0,
        status: "IDLE",
      })),
    };

    // Simulate each step progressing
    for (let stepIndex = 0; stepIndex < executionState.steps.length; stepIndex++) {
      executionState.currentStepIndex = stepIndex;

      for (let progress = 0; progress <= 100; progress += 5) {
        executionState.steps[stepIndex].percentComplete = progress;

        await delay(120);
      }
    }

    executionState.status = "COMPLETED";

    await delay(750);

    executionState = {
      executionId: "",
      currentStepIndex: 0,
      status: "IDLE",
      steps: [],
    };
  }

  isProcessingQueue = false;
}

function delay(ms: number) {
  return new Promise((resolve) => {
    setTimeout(resolve, ms);
  });
}
