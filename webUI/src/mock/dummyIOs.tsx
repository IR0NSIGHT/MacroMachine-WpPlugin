import { InputOutput } from '@/types';

// Minecraft height IO: -64 to 320
export const heightIO: InputOutput = {
    min: -64,
    max: 320,
    ignoreValue: -999,
    values: Array.from({ length: 385 }, (_, i) => ({
        value: -64 + i,
        displayName: `y=${-64 + i}`,
    })),
    discrete: false,
    displayName: '',
    description: '',
    uid: '',
    parameters: []
};

export const alwaysIO: InputOutput = {
    min: 0,
    max: 0,
    ignoreValue: -999,
    values: [
        { value: 0, displayName: 'Always' },
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
        value: i,
        displayName: `depth ${i}`,
    })),
    discrete: false,
    displayName: '',
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
        value: i,
        displayName: annotationNames[i],
    })),
    discrete: true,
    description: '',
    uid: '',
    parameters: []
}



export const slopeIO: InputOutput = {
    displayName: "Slope",
    min: 0,
    max: 90,
    ignoreValue: -1,
    values: Array.from({ length: 90 }, (_, i) => ({
        value: i,
        displayName: i+"°",
    })),
    discrete: false,
    description: '',
    uid: '',
    parameters: []
}

export const forestIO: InputOutput = {
    displayName: "Deciduous",
    min: 0,
    max: 15,
    ignoreValue: -1,
    values: Array.from({ length: 15 }, (_, i) => ({
        value: i,
        displayName: "lvl="+i,
    })),
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
    value: i,
    displayName: terrainNames[i],
  })),
  discrete: true,
  description: 'Common Minecraft terrain and block types',
  uid: '',
  parameters: [],
}