import { defineConfig } from "vitest/config";
import path from 'node:path';

export default defineConfig({
    resolve: {
        alias: {
            "@": path.resolve(__dirname, "./src"),
        },
    },
    test: {
        coverage: {
            provider: "v8",
            include: ["src/**/*.ts"],
            exclude: ["src/**/*.test.ts","src/**/*.test.ts", "src/**/*.tsx", "src/**/*.d.ts"],
        }
    },
});