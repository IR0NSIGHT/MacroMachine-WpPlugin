import { promises as fs } from "fs";
import path from "path";
import { fileURLToPath } from "url";

const BACKEND_URL = process.env.BACKEND_URL ?? "http://localhost:8080";

const __filename = fileURLToPath(import.meta.url);
const __dirname = path.dirname(__filename);

const DEFAULT_FILTERS_PATH = path.join(__dirname, "../mocks/data/defaultFilters.json");
const DEFAULT_APPLIERS_PATH = path.join(__dirname, "../mocks/data/defaultApplyActions.json");

async function fetchJson(endpoint) {
  const url = new URL(endpoint, BACKEND_URL);
  const response = await fetch(url);

  if (!response.ok) {
    throw new Error(`GET ${url} failed: ${response.status} ${response.statusText}`);
  }

  return response.json();
}

async function writeJson(filePath, data) {
  if (!Array.isArray(data)) {
    throw new Error(`${filePath} data must be an array`);
  }

  await fs.writeFile(filePath, `${JSON.stringify(data, null, 2)}\n`, "utf8");
  console.log(`Wrote ${data.length} items to ${filePath}`);
}

async function main() {
  console.log(`Fetching mock data from ${BACKEND_URL}`);

  const [filters, appliers] = await Promise.all([
    fetchJson("/api/actions/filters"),
    fetchJson("/api/actions/appliers"),
  ]);

  await Promise.all([
    writeJson(DEFAULT_FILTERS_PATH, filters),
    writeJson(DEFAULT_APPLIERS_PATH, appliers),
  ]);
}

main().catch((err) => {
  console.error(err);
  process.exitCode = 1;
});
