// THIS IS ONLY TEMPORARY TO EXTRACT INPUTS AND OUTPUTS FROM A JSON

import { promises as fs } from "fs";

const _filterType = "INTERMEDIATE_SELECTION";
const alwaysType = "ALWAYS";

async function fetchActionsToFile(fileOutputPath) {
  const response = await fetch("http://localhost:8080/api/actions");

  if (!response.ok) {
    throw new Error(`Request failed: ${response.status}`);
  }

  const data = await response.json();

  await fs.writeFile(fileOutputPath, JSON.stringify(data, null, 2), "utf8");

  console.log(`Saved JSON to ${fileOutputPath}`);
}

const getFilters = async (data, outputPath) => {
  const seen = new Set();

  const accpetedFilters = data.filter((action) => {
    if (
      action.input.type === "INTERMEDIATE" ||
      action.input.type === alwaysType ||
      action.output.type !== _filterType
    )
      return false;

    const actionHash = action.input.type.trim().toLowerCase();

    if (seen.has(actionHash)) {
      return false;
    }

    seen.add(actionHash);
    return true;
  });
  console.log(
    "filters:",
    accpetedFilters.map((a) => a.name),
  );

  const outputRaw = JSON.stringify(accpetedFilters, null, 3);
  // Write output
  await fs.writeFile(outputPath, outputRaw, "utf8");

  console.log(`Wrote ${accpetedFilters.length} items to ${outputPath}`);
};

const getApplyActions = async (data, outputPath) => {
  const seen = new Set();

  const acceptedItems = data.filter((action) => {
    if (action.input.type !== alwaysType || action.output.type === _filterType) return false;

    const actionHash = action.output.type.trim().toLowerCase();

    if (seen.has(actionHash)) {
      return false;
    }

    seen.add(actionHash);
    return true;
  });
  console.log(
    "actions",
    acceptedItems.map((a) => a.name),
  );

  const outputRaw = JSON.stringify(acceptedItems, null, 3);
  // Write output
  await fs.writeFile(outputPath, outputRaw, "utf8");

  console.log(`Wrote ${acceptedItems.length} items to ${outputPath}`);
};

async function filterJsonFile(inputPath, outputFilters, outputActions) {
  try {
    // Read file
    const raw = await fs.readFile(inputPath, "utf8");
    // Parse JSON
    const data = JSON.parse(raw);

    if (!Array.isArray(data)) {
      throw new Error("JSON file must contain an array");
    }

    await getFilters(data, outputFilters);
    await getApplyActions(data, outputActions);
  } catch (err) {
    console.error("Error:", err.message);
  }
}

const ACTIONS_PATH =
  "/home/klipper/Documents/repos/MacroMachine-WpPlugin/webUI/src/mocks/data/actions.json";

// pull data
await fetchActionsToFile(ACTIONS_PATH);
// Usage
filterJsonFile(ACTIONS_PATH, "./defaultFilters.json", "./defaultApplyActions.json");
