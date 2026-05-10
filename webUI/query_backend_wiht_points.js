const fetch = global.fetch; // Node 18+ (no install needed)

const API_BASE = "http://127.0.0.1:8080"; // adjust if needed

const uuid = "602c96eb-27a1-4d73-9599-f2d96fc78c91";

const mappingPoints = [
  { x: 1, y: 2 },
  { x: 3, y: 4 },
];

async function test() {
  const url = `${API_BASE}/action?uuid=${uuid}&points=${encodeURIComponent(JSON.stringify(mappingPoints))}`;

  console.log("Request URL:");
  console.log(url);

  try {
    const res = await fetch(url);

    if (!res.ok) {
      throw new Error(`HTTP ${res.status}`);
    }

    const data = await res.json();
    console.log("Response:");
    console.dir(data, { depth: null });
  } catch (err) {
    console.error("Error:", err);
  }
}

test();
