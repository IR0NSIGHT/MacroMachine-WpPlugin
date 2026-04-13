import { MMAction } from "@/types/MMAction";
import { annotationsIO, forestIO, heightIO, slopeIO } from "./dummyIOs";

export const raiseYonCyan: MMAction = {
  input: annotationsIO,
  output: heightIO,
  actionType: "increment",
  inputPoints: annotationsIO.values.filter(v => v.numericValue != annotationsIO.ignoreValue).map(v => v.numericValue),
  outputPoints: annotationsIO.values.map(v => v.numericValue < 5 ? 3 : 10),
  name: 'raise on cyan',
  description: 'description of the action',
  uid: 'f5e02009-97ae-4955-a521-92639642c71b',
}

export const slopeToForest: MMAction = {
  input: slopeIO,
  output: forestIO,
  actionType: "set",
  inputPoints: slopeIO.values.map(v => v.numericValue),
  outputPoints: slopeIO.values.map(v => v.numericValue < 60 ? v.numericValue < 30 ?  15 : 8 : 2),
  name: 'slope defines forest strength',
  description: 'description of the action',
  uid: 'f5e02009-97ae-4955-a521-92639642c71b',
}
