

import { http, HttpResponse } from "msw";
import { actions, macroList, macros } from "./db";
import { API_BASE } from "@/API/api";


export const handlers = [
  // ----------------------------
  // GET /macroList
  // ----------------------------
  http.get(`${API_BASE}/macroList`, () => {
    return HttpResponse.json(macroList);
  }),

  // ----------------------------
  // GET /macro?uuid=...
  // ----------------------------
  http.get(`${API_BASE}/macro`, ({ request }) => {
    const url = new URL(request.url);
    const uuid = url.searchParams.get("uuid");

    if (!uuid) {
      return new HttpResponse("Missing uuid", { status: 400 });
    }

    const macro = macros[uuid];

    if (!macro) {
      return new HttpResponse("Macro not found", { status: 404 });
    }

    return HttpResponse.json(macro);
  }),

  // ----------------------------
  // GET /action?uuid=...
  // ----------------------------
  http.get(`${API_BASE}/action`, ({ request }) => {
    const url = new URL(request.url);
    const uuid = url.searchParams.get("uuid");

    if (!uuid) {
      return new HttpResponse("Missing uuid", { status: 400 });
    }

    const action = actions[uuid];

    if (!action) {
      return new HttpResponse("Action not found", { status: 404 });
    }

    return HttpResponse.json(action);
  }),
];