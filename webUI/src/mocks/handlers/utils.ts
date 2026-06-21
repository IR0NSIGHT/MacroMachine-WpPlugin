// src/mocks/handlers/utils.ts
import { http, HttpResponse } from "msw";

export const utilHandlers = [
  /**
   * GET /docs
   */
  http.get("/api/docs", () => {
    return new HttpResponse(
      `
      <html>
        <head>
          <title>MacroMachine API Documentation</title>
        </head>
        <body>
          <h1>MacroMachine API</h1>
          <p>Mock API documentation</p>
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
   * OPTIONS /
   */
  http.options("/", () => {
    return new HttpResponse(null, {
      status: 204,
      headers: {
        Allow: "OPTIONS, GET, POST, DELETE",
      },
    });
  }),
];
