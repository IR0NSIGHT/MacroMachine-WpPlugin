import { MappingPointDTO, MMAction } from "@/types/MMAction";
import {
  alwaysIO,
  annotationsIO,
  filterIO,
  forestIO,
  heightIO,
  slopeIO,
  terrainIO,
  waterdepthIO,
} from "./dummyIOs";
import { InputOutput } from "@/types/InputOutput";

export const inputValues = (input: InputOutput) => {
  return input.values.filter((v) => v.numericValue !== input.ignoreValue);
};

const mappingPointsRaiseYonCyan: MappingPointDTO[] = inputValues(annotationsIO).map((p) => ({
  x: p.numericValue,
  y: p.numericValue != 9 ? 3 : heightIO.ignoreValue,
}));
export const raiseYonCyan: MMAction = {
  input: annotationsIO,
  output: heightIO,
  actionType: "increments",
  mappedInputs: mappingPointsRaiseYonCyan.map((p) => p.x),
  mappedOutputs: mappingPointsRaiseYonCyan.map((p) => p.y),
  mappingPoints: mappingPointsRaiseYonCyan,
  name: "raise on cyan",
  description: "",
  uid: "f5e02009-97ae-1213-a521-92123132c71b",
};

export const slopeToForest: MMAction = {
  input: slopeIO,
  output: forestIO,
  actionType: "limits",
  mappedInputs: inputValues(slopeIO).map((v) => v.numericValue),
  mappedOutputs: inputValues(slopeIO).map((v) =>
    v.numericValue <= 30
      ? 0
      : v.numericValue <= 60
        ? Math.round(((v.numericValue - 30) / (60 - 30)) * 7)
        : 7,
  ), // flat 0 till 30, itnerpolate to y=50 till x=60, flat till end
  mappingPoints: [
    { x: 30, y: 0 },
    { x: 60, y: 7 },
  ],
  name: "no forest on cliffs",
  description: "",
  uid: "f5e02009-97ae-1011-a521-921232c71b",
};

export const slopeToTerrain: MMAction = {
  input: slopeIO,
  output: terrainIO,
  actionType: "sets",
  mappedInputs: inputValues(slopeIO).map((v) => v.numericValue),
  mappedOutputs: inputValues(slopeIO).map((v) =>
    v.numericValue < 30 ? 0 : v.numericValue < 60 ? 3 : 7,
  ), // flat 0 till 30, itnerpolate to y=50 till x=60, flat till end
  mappingPoints: [
    { x: 30, y: 3 },
    { x: 60, y: 7 },
  ],
  name: "slope defines rock color",
  description: "",
  uid: "f5e02009-97ae-1011-a521-921232c71b",
};

export const grassEverywhere: MMAction = {
  input: alwaysIO,
  output: terrainIO,
  actionType: "sets",
  mappedInputs: [0],
  mappedOutputs: [2],
  mappingPoints: [{ x: 0, y: 2 }],
  name: "apply grass",
  description: "Apply grass-terrain everywhere",
  uid: "f5e02009-97ae-789-a521-125152c71b",
};

// fully defined filter
const mappingPointsOnlyOnLand: MappingPointDTO[] = inputValues(waterdepthIO).map((p) => ({
  x: p.numericValue,
  y: p.numericValue != 0 ? 0 /*block */ : -1 /** ignore */,
}));
export const onlyOnLand: MMAction = {
  input: waterdepthIO,
  output: filterIO,
  actionType: "sets",
  mappedInputs: mappingPointsOnlyOnLand.map((p) => p.x),
  mappedOutputs: mappingPointsOnlyOnLand.map((p) => p.y),
  mappingPoints: mappingPointsOnlyOnLand,
  name: "only on land",
  description: "filter",
  uid: "f5e02009-97ae-123-a521-92639642c71b",
};

// fully defined because output is discrete
const mappingPointsOnlyOnCyan: MappingPointDTO[] = inputValues(annotationsIO).map((p) => ({
  x: p.numericValue,
  y: p.numericValue != 9 ? 0 : -1,
}));
export const onlyOnCyan: MMAction = {
  input: annotationsIO,
  output: filterIO,
  actionType: "sets",
  mappedInputs: mappingPointsOnlyOnCyan.map((p) => p.x),
  mappedOutputs: mappingPointsOnlyOnCyan.map((p) => p.y),
  mappingPoints: mappingPointsOnlyOnCyan,
  name: "only on cyan",
  description: "filter",
  uid: "f5e02009-97ae-456-a521-91231232c71b",
};
