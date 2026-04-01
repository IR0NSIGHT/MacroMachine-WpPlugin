import { MMAction } from "@/types/MMAction";
import { annotationsIO, forestIO, heightIO, slopeIO } from "./dummyIOs";

export const raiseYonCyan: MMAction = {
  input: annotationsIO,
  output: heightIO,
  actionType: 'SET',
  inputPoints:[0,10],
  outputPoints: [3, 10],
  name: 'raise on cyan',
  description: 'description of the action',
  uid: 'f5e02009-97ae-4955-a521-92639642c71b',
}

export const slopeToForest: MMAction = {
  input: slopeIO,
  output: forestIO,
  actionType: 'SET',
  inputPoints:[0,45,70],
  outputPoints: [15,7, 0],
  name: 'slope defines forest strength',
  description: 'description of the action',
  uid: 'f5e02009-97ae-4955-a521-92639642c71b',
}
