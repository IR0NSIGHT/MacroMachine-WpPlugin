// THIS IS ONLY TEMPORARY TO EXTRACT INPUTS AND OUTPUTS FROM A JSON

import { promises as fs } from "fs";

async function filterJsonFile(inputPath, outputPath) {
  try {
    // Read file
    const raw = await fs.readFile(inputPath, "utf8");
    // Parse JSON
    const data = JSON.parse(raw);

    if (!Array.isArray(data)) {
      throw new Error("JSON file must contain an array");
    }

    // Example filter:
    // Keep only items where `active === true`
    const inputs = data
      .filter((item) => item.input.type === "ALWAYS")
      .map((item) => item)
      .flat();
    console.log(inputs);
    const map = new Map();
    inputs.forEach((input) => {
      map.set(input.type, input);
    });
    console.log(map);
    const outputRaw = JSON.stringify(inputs, null, 3);
    // Write output
    await fs.writeFile(outputPath, outputRaw, "utf8");

    console.log(`Wrote ${inputs.length} items to ${outputPath}`);
  } catch (err) {
    console.error("Error:", err.message);
  }
}

// Usage
filterJsonFile(
  "/home/klipper/Documents/repos/MacroMachine-WpPlugin/webUI/src/mocks/data/actions.json",
  "./defaultApplyActions.json",
);
