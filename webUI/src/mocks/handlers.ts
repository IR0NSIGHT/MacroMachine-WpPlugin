// src/mocks/handlers.ts
import { http, HttpResponse } from "msw";
import { ActionDTO, MacroDTO, ExecutionQueueDTO, ExecutionStateDTO } from "@/types/DTO";

import mock_actions from "./data/actions.json";
import mock_macros from "./data/macros.json";
import mock_state from "./data/state.json";
import mock_queue from "./data/queue.json";

/**
 * Replace these with your actual hardcoded JSON fixtures later
 */
const actions: ActionDTO[] = mock_actions as ActionDTO[];
const macros: MacroDTO[] = mock_macros;
const executionState: ExecutionStateDTO = mock_state as any;

let executionQueue: ExecutionQueueDTO = mock_queue;

export const handlers = [
  /**
   * GET /actions
   */
  http.get("/api/actions", () => {
    return HttpResponse.json(actions);
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

    executionQueue = body;

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
