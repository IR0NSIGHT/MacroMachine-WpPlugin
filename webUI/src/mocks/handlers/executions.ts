// src/mocks/handlers/executions.ts
import { http, HttpResponse } from "msw";
import { ExecutionQueueDTO, ExecutionStateDTO } from "@/types/DTO";
import mock_state from "../data/state.json";
import mock_queue from "../data/queue.json";
import mock_history from "../data/history.json";

let executionQueue: ExecutionQueueDTO = mock_queue;
let executionState: ExecutionStateDTO = structuredClone(mock_state as any);
let executionHistory: ExecutionStateDTO[] = mock_history as ExecutionStateDTO[];
let isProcessingQueue = false;

export const executionHandlers = [
  /**
   * GET /execution/state
   */
  http.get("/api/execution/state", () => {
    return HttpResponse.json(executionState);
  }),

  /**
   * GET /execution/state/history
   */
  http.get("/api/execution/state/history", () => {
    return HttpResponse.json(executionHistory);
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

    // Simulate execution state progression
    executionState = {
      executionId: macroId,
      currentStepIndex: 0,
      status: "RUNNING",
      steps: [
        {
          actionId: crypto.randomUUID(),
          breakpoint: false,
          percentComplete: 0,
          status: "RUNNING",
        },
      ],
    };

    for (let progress = 0; progress <= 100; progress += 5) {
      executionState.steps[0].percentComplete = progress;
      await delay(120);
    }

    executionState.status = "COMPLETED";
    executionHistory.push(structuredClone(executionState));

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
