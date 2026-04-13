import { InputOutput } from '@/types/InputOutput';

// Minecraft height IO: -64 to 320
export const heightIO: InputOutput = {
    min: -64,
    max: 320,
    ignoreValue: -999,
    values: Array.from({ length: 385 }, (_, i) => ({
        numericValue: -64 + i,
        displayName: `y=${-64 + i}`,
    })).concat([{ numericValue: -999, displayName: '[Ignore]' }]),
    discrete: false,
    displayName: 'Terrain Height (Y)',
    description: '',
    uid: '',
    parameters: [
      { name: "min", type: "number", value: -64 },
      { name: "max", type: "number", value: 320 },
    ]
};

export const alwaysIO: InputOutput = {
    min: 0,
    max: 0,
    ignoreValue: -999,
    values: [
        { numericValue: 0, displayName: 'Always' },
    ],
    discrete: true,
    displayName: 'Always',
    description: '',
    uid: '',
    parameters: []
};

// Water depth IO: 0 to 30
export const waterdepthIO: InputOutput = {
    min: 0,
    max: 30,
    ignoreValue: -1,
    values: Array.from({ length: 31 }, (_, i) => ({
        numericValue: i,
        displayName: `${i} deep`,
    })).concat([{ numericValue: -1, displayName: '[Ignore]' }]),
    discrete: false,
    displayName: 'Water Depth',
    description: '',
    uid: '',
    parameters: []
}

// Annotations IO: 0-15 with annotation color names
const annotationNames = [
    'No annotation',
    'White',
    'Orange',
    'Magenta',
    'Light Blue',
    'Yellow',
    'Lime',
    'Pink',
    'Light Grey',
    'Cyan',
    'Purple',
    'Blue',
    'Brown',
    'Green',
    'Red',
    'Black',
]

export const annotationsIO: InputOutput = {
    displayName: "Annotations",
    min: 0,
    max: 15,
    ignoreValue: -1,
    values: Array.from({ length: 16 }, (_, i) => ({
        numericValue: i,
        displayName: annotationNames[i],
    })).concat([{ numericValue: -1, displayName: '[Ignore]' }]),
    discrete: true,
    description: '',
    uid: '',
    parameters: []
}

export const biomesIO: InputOutput = {
  displayName: "Biomes",
  min: 0,
  max: 255,
  ignoreValue: -1,
  discrete: true,
  description: 'Minecraft 1.21 biomes',
  uid: 'biomes',
  parameters: [],
  values: [
    { numericValue: -1, displayName: '[Ignore]' },
    { numericValue: 0, displayName: "Ocean" },
    { numericValue: 1, displayName: "Plains" },
    { numericValue: 2, displayName: "Desert" },
    { numericValue: 3, displayName: "Windswept Hills" },
    { numericValue: 4, displayName: "Forest" },
    { numericValue: 5, displayName: "Taiga" },
    { numericValue: 6, displayName: "Swamp" },
    { numericValue: 7, displayName: "River" },
    { numericValue: 8, displayName: "Nether Wastes" },
    { numericValue: 9, displayName: "The End" },
    { numericValue: 10, displayName: "Frozen Ocean" },
    { numericValue: 11, displayName: "Frozen River" },
    { numericValue: 12, displayName: "Snowy Plains" },
    { numericValue: 13, displayName: "Snowy Mountains" },
    { numericValue: 14, displayName: "Mushroom Fields" },
    { numericValue: 15, displayName: "Mushroom Field Shore" },
    { numericValue: 16, displayName: "Beach" },
    { numericValue: 17, displayName: "Desert Hills" },
    { numericValue: 18, displayName: "Wooded Hills" },
    { numericValue: 19, displayName: "Taiga Hills" },
    { numericValue: 20, displayName: "Mountain Edge" },
    { numericValue: 21, displayName: "Jungle" },
    { numericValue: 22, displayName: "Jungle Hills" },
    { numericValue: 23, displayName: "Jungle Edge" },
    { numericValue: 24, displayName: "Deep Ocean" },
    { numericValue: 25, displayName: "Stone Shore" },
    { numericValue: 26, displayName: "Snowy Beach" },
    { numericValue: 27, displayName: "Birch Forest" },
    { numericValue: 28, displayName: "Birch Forest Hills" },
    { numericValue: 29, displayName: "Dark Forest" },
    { numericValue: 30, displayName: "Snowy Taiga" },
    { numericValue: 31, displayName: "Snowy Taiga Hills" },
    { numericValue: 32, displayName: "Giant Tree Taiga" },
    { numericValue: 33, displayName: "Giant Tree Taiga Hills" },
    { numericValue: 34, displayName: "Wooded Mountains" },
    { numericValue: 35, displayName: "Savanna" },
    { numericValue: 36, displayName: "Savanna Plateau" },
    { numericValue: 37, displayName: "Badlands" },
    { numericValue: 38, displayName: "Wooded Badlands Plateau" },
    { numericValue: 39, displayName: "Badlands Plateau" },

    // Modern cave biomes
    { numericValue: 40, displayName: "Dripstone Caves" },
    { numericValue: 41, displayName: "Lush Caves" },
    { numericValue: 42, displayName: "Deep Dark" },

    // Modern ocean variants
    { numericValue: 43, displayName: "Warm Ocean" },
    { numericValue: 44, displayName: "Lukewarm Ocean" },
    { numericValue: 45, displayName: "Cold Ocean" },
    { numericValue: 46, displayName: "Deep Warm Ocean" },
    { numericValue: 47, displayName: "Deep Lukewarm Ocean" },
    { numericValue: 48, displayName: "Deep Cold Ocean" },
    { numericValue: 49, displayName: "Deep Frozen Ocean" },

    // Nether
    { numericValue: 50, displayName: "Crimson Forest" },
    { numericValue: 51, displayName: "Warped Forest" },
    { numericValue: 52, displayName: "Basalt Deltas" },
    { numericValue: 53, displayName: "Soul Sand Valley" },

    // End
    { numericValue: 54, displayName: "End Highlands" },
    { numericValue  : 55, displayName: "End Midlands" },
    { numericValue: 56, displayName: "Small End Islands" },
    { numericValue: 57, displayName: "End Barrens" },

    // Newer overworld additions
    { numericValue: 58, displayName: "Meadow" },
    { numericValue: 59, displayName: "Grove" },
    { numericValue: 60, displayName: "Snowy Slopes" },
    { numericValue: 61, displayName: "Frozen Peaks" },
    { numericValue: 62, displayName: "Jagged Peaks" },
    { numericValue: 63, displayName: "Stony Peaks" },
    { numericValue: 64, displayName: "Cherry Grove" },

    // Variants (trimmed but useful)
    { numericValue: 100, displayName: "Sunflower Plains" },
    { numericValue: 101, displayName: "Desert Lakes" },
    { numericValue: 102, displayName: "Gravelly Mountains" },
    { numericValue: 103, displayName: "Flower Forest" },
    { numericValue: 104, displayName: "Ice Spikes" },
    { numericValue: 105, displayName: "Modified Jungle" },
    { numericValue: 106, displayName: "Modified Jungle Edge" },
    { numericValue: 107, displayName: "Tall Birch Forest" },
    { numericValue: 108, displayName: "Tall Birch Hills" },
    { numericValue: 109, displayName: "Dark Forest Hills" },
    { numericValue: 110, displayName: "Snowy Taiga Mountains" },
    { numericValue: 111, displayName: "Giant Spruce Taiga" },
    { numericValue: 112, displayName: "Giant Spruce Taiga Hills" },
    { numericValue: 113, displayName: "Modified Gravelly Mountains" },
    { numericValue: 114, displayName: "Shattered Savanna" },
    { numericValue: 115, displayName: "Shattered Savanna Plateau" },
    { numericValue: 116, displayName: "Eroded Badlands" },
    { numericValue: 117, displayName: "Modified Wooded Badlands Plateau" },
    { numericValue: 118, displayName: "Modified Badlands Plateau" },
  ],
}

export const slopeIO: InputOutput = {
    displayName: "Slope",
    min: 0,
    max: 90,
    ignoreValue: -1,
    values: Array.from({ length: 91 }, (_, i) => ({
        numericValue: i,
        displayName: i+"°",
    })).concat([{ numericValue: -1, displayName: '[Ignore]' }]),
    discrete: false,
    description: '',
    uid: '',
    parameters: []
}
export const perlinNoiseIO: InputOutput = {
  displayName: "Perlin Noise",
  description: "Perlin Noise Generator",

  min: 0,
  max: 100, // mock amplitude default

  ignoreValue: -1, // not present in Java, so we define a safe default

  discrete: false,

  uid: "perlin_noise",

  values: Array.from({ length: 101 }, (_, i) => ({
    numericValue: i,
    displayName: i.toString()
  })).concat([{ numericValue: -1, displayName: '[Ignore]' }]),

  parameters: [
    {
      name: "scale",
      type: "number",
      value: 5,
    },
    {
      name: "amplitude",
      type: "number",
      value: 100,
    },
    {
      name: "octaves",
      type: "number",
      value: 4,
    },
    {
      name: "seed",
      type: "number",
      value: 4206973845,
    }
  ]
};

export const forestIO: InputOutput = {
    displayName: "Deciduous",
    min: 0,
    max: 15,
    ignoreValue: -1,
    values: Array.from({ length: 16 }, (_, i) => ({
        numericValue: i,
        displayName: "lvl="+i,
    })).concat([{ numericValue: -1, displayName: '[Ignore]' }]),
    discrete: false,
    description: '',
    uid: '',
    parameters: []
}

const terrainNames = [
  'Air',
  'Stone',
  'Grass Block',
  'Dirt',
  'Cobblestone',
  'Bedrock',
  'Sand',
  'Gravel',
  'Oak Planks',
  'Spruce Planks',
  'Birch Planks',
  'Jungle Planks',
  'Acacia Planks',
  'Dark Oak Planks',
  'Water',
  'Lava',
  'Coal Ore',
  'Iron Ore',
  'Gold Ore',
  'Diamond Ore',
  'Redstone Ore',
  'Emerald Ore',
  'Quartz Ore',
  'Obsidian',
  'Netherrack',
  'End Stone',
  'Snow Block',
  'Ice',
  'Clay',
  'Bricks',
]

export const terrainIO: InputOutput = {
  displayName: "Terrain Types",
  min: 0,
  max: 29,
  ignoreValue: -1,
  values: Array.from({ length: 30 }, (_, i) => ({
    numericValue: i,
    displayName: terrainNames[i],
  })).concat([{ numericValue: -1, displayName: '[Ignore]' }]),
  discrete: true,
  description: 'Common Minecraft terrain and block types',
  uid: '',
  parameters: [],
}