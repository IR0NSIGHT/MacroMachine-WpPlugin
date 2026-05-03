

import { http, HttpResponse } from "msw";
import { actions, macroList, macros } from "./db";
import { API_BASE } from "@/API/api";
import { MappingPointDTO } from "@/types/MMAction";
import { withNewPoints } from "./actionWithPoints";


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

    var action = actions[uuid];
    if (!action) {
      return new HttpResponse("Action not found", { status: 404 });
    }

    const pointsParam = url.searchParams.get("points");
    let mappingPoints: MappingPointDTO[] = [];
    if (pointsParam) {
      try {
        mappingPoints = JSON.parse(decodeURIComponent(pointsParam));
      } catch (e) {
        console.error("Invalid points parameter in MSW withNewPoints:", pointsParam, e);
        return HttpResponse.json(
          { error: "Invalid points parameter" },
          { status: 400 }
        );
      }
    }

    if (mappingPoints && mappingPoints.length > 0) {
      action = withNewPoints(action, mappingPoints);
      console.log("Returning action with new points:", action);
    }

    return HttpResponse.json(action);
  }),
];