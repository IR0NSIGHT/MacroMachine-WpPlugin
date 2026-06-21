// src/mocks/handlers/layers.ts
import { http, HttpResponse } from "msw";
import mock_layers from "../data/layers.json";
import { LayerDTO } from "@/generated/client";

// Mock layers data
const layers: LayerDTO[] = mock_layers as LayerDTO[];

export const layerHandlers = [
  /**
   * GET /layers
   */
  http.get("/api/layers", () => {
    return HttpResponse.json(layers);
  }),

  /**
   * GET /layers/:id
   */
  http.get("/api/layers/:id", ({ params }) => {
    const layer = layers.find((l) => l.id === params.id);

    if (!layer) {
      return HttpResponse.json({ message: "Layer not found" }, { status: 404 });
    }

    return HttpResponse.json(layer.presentInProject);
  }),

  /**
   * GET /layers/:id/icon
   */
  http.get("/api/layers/:id/icon", ({ params }) => {
    const layer = layers.find((l) => l.id === params.id);

    if (!layer) {
      return HttpResponse.json({ message: "Layer not found" }, { status: 404 });
    }

    // Return a transparent 1x1 PNG as placeholder
    const png = new Uint8Array([
      0x89, 0x50, 0x4e, 0x47, 0x0d, 0x0a, 0x1a, 0x0a, 0x00, 0x00, 0x00, 0x0d, 0x49, 0x48, 0x44,
      0x52, 0x00, 0x00, 0x00, 0x01, 0x00, 0x00, 0x00, 0x01, 0x08, 0x06, 0x00, 0x00, 0x00, 0x1f,
      0x15, 0xc4, 0x89, 0x00, 0x00, 0x00, 0x0a, 0x49, 0x44, 0x41, 0x54, 0x78, 0x9c, 0x63, 0x00,
      0x01, 0x00, 0x00, 0x05, 0x00, 0x01, 0x0d, 0x0a, 0x2d, 0xb4, 0x00, 0x00, 0x00, 0x00, 0x49,
      0x45, 0x4e, 0x44, 0xae, 0x42, 0x60, 0x82,
    ]);

    return new HttpResponse(png, {
      status: 200,
      headers: {
        "Content-Type": "image/png",
      },
    });
  }),
];
