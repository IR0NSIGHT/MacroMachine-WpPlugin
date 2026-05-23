// THIS IS ONLY TEMPORARY TO EXTRACT INPUTS AND OUTPUTS FROM A JSON

import { promises as fs } from "fs";

const _filterType = "INTERMEDIATE_SELECTION";
const alwaysType = "ALWAYS";
async function filterJsonFile(inputPath, outputPath) {
  try {
    // Read file
    const raw = await fs.readFile(inputPath, "utf8");
    // Parse JSON
    const data = JSON.parse(raw);

    if (!Array.isArray(data)) {
      throw new Error("JSON file must contain an array");
    }

    const seen = new Set();

    const accpetedFilters = data.filter((action) => {
      if (action.input.type !== alwaysType) return false;

      const actionHash = action.output.type.trim().toLowerCase();

      if (seen.has(actionHash)) {
        return false;
      }

      seen.add(actionHash);
      return true;
    });
    console.log(accpetedFilters.map((a) => a.name));

    const outputRaw = JSON.stringify(accpetedFilters, null, 3);
    // Write output
    await fs.writeFile(outputPath, outputRaw, "utf8");

    console.log(`Wrote ${accpetedFilters.length} items to ${outputPath}`);
  } catch (err) {
    console.error("Error:", err.message);
  }
}

// Usage
filterJsonFile(
  "/home/klipper/Documents/repos/MacroMachine-WpPlugin/webUI/src/mocks/data/actions.json",
  "./defaultApplyActions.json",
);
