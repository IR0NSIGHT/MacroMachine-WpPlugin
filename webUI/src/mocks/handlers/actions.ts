// src/mocks/handlers/actions.ts
import { http, HttpResponse } from "msw";
import { ActionDTO } from "@/types/DTO";
import mock_actions from "../data/actions.json";
import defaultApplyActions from "../data/defaultApplyActions.json";
import defaultFilters from "../data/defaultFilters.json";

const actions: ActionDTO[] = mock_actions as ActionDTO[];
const applyActions: ActionDTO[] = defaultApplyActions as ActionDTO[];
const filters: ActionDTO[] = defaultFilters as ActionDTO[];

let lastActionChange = Date.now();

export const actionHandlers = [
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
    lastActionChange = Date.now();

    return HttpResponse.json(created, {
      status: 201,
    });
  }),

  /**
   * GET /actions/lastChange
   */
  http.get("/api/actions/lastChange", () => {
    return HttpResponse.json(lastActionChange);
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
    lastActionChange = Date.now();

    return HttpResponse.json({
      success: true,
    });
  }),
];
