import { MMAction } from "@/types/MMAction";
import { alwaysIO, annotationsIO, filterIO, forestIO, heightIO, slopeIO, terrainIO, waterdepthIO } from "./dummyIOs";
import { InputOutput } from "@/types/InputOutput";

export const inputValues = (input: InputOutput) => {
  return input.values.filter(v => v.numericValue !== input.ignoreValue)
}

export const raiseYonCyan: MMAction = {
  input: annotationsIO,
  output: heightIO,
  actionType: "increments",
  inputPoints: inputValues(annotationsIO).map(v => v.numericValue),
  outputPoints: inputValues(annotationsIO).map(v => v.numericValue == 9 /**cyan*/ ? 3 : heightIO.ignoreValue),
  name: 'raise on cyan',
  description: '',
  uid: 'f5e02009-97ae-4955-a521-92639642c71b',
}

export const slopeToForest: MMAction = {
  input: slopeIO,
  output: forestIO,
  actionType: "limits",
  inputPoints: inputValues(slopeIO).map(v => v.numericValue),
  outputPoints: inputValues(slopeIO).map(v => v.numericValue < 60 ? v.numericValue < 30 ? 15 : 8 : 2),
  name: 'no forest on cliffs',
  description: '',
  uid: 'f5e02009-97ae-4955-a521-92639642c71b',
}

export const grassEverywhere: MMAction = {
  input: alwaysIO,
  output: terrainIO,
  actionType: "sets",
  inputPoints: [0],
  outputPoints: [2],
  name: 'apply grass',
  description: 'Apply grass-terrain everywhere',
  uid: 'f5e02009-97ae-4955-a521-92639642c71b',
}

export const onlyOnLand: MMAction = {
  input: waterdepthIO,
  output: filterIO,
  actionType: "sets",
  inputPoints: inputValues(waterdepthIO).map(p => p.numericValue),
  outputPoints: inputValues(waterdepthIO).map(p => p.numericValue != 0 ? 0  /*block */: -1 /** ignore */ ),
  name: 'only on land',
  description: 'filter',
  uid: 'f5e02009-97ae-4955-a521-92639642c71b',
}

export const onlyOnCyan: MMAction = {
  input: annotationsIO,
  output: filterIO,
  actionType: "sets",
  inputPoints: inputValues(annotationsIO).map(p => p.numericValue),
  outputPoints: inputValues(annotationsIO).map(p => p.numericValue != 9 /*cyan */ ? 0  /*block */: -1 /** ignore */ ),
  name: 'only on cyan',
  description: 'filter',
  uid: 'f5e02009-97ae-4955-a521-92639642c71b',
}