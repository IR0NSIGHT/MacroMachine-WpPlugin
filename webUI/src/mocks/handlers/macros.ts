// src/mocks/handlers/macros.ts
import { http, HttpResponse } from "msw";
import { MacroDTO } from "@/types/DTO";
import mock_macros from "../data/macros.json";

const macros: MacroDTO[] = mock_macros;
let lastMacroChange = Date.now();

export const macroHandlers = [
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
    lastMacroChange = Date.now();

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
    lastMacroChange = Date.now();

    return HttpResponse.json({
      success: true,
    });
  }),

  /**
   * GET /macros/lastChange
   */
  http.get("/api/macros/lastChange", () => {
    return HttpResponse.json(lastMacroChange);
  }),
];
