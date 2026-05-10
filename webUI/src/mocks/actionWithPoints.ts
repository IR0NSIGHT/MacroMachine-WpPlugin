import { InputOutput, inputValues, isIgnoreValue } from "@/types/InputOutput";
import { MappingPoint } from "@/types/MappingPoint";
import { MappingPointDTO, MMAction } from "@/types/MMAction";

function generateMappings(
  mappingPoints: MappingPointDTO[],
  input: InputOutput,
  output: InputOutput,
): number[] {
  let mappings: number[];

  if (output.discrete) {
    mappings = Array.from({ length: input.max + 1 - input.min }, () => 0);

    if (mappingPoints.length === 0) return mappings;

    let j = -1;

    for (const i of inputValues(input)) {
      if (isIgnoreValue(input, i.numericValue)) {
        continue;
      }

      const mappingPointIdx = Math.max(Math.min(j + 1, mappingPoints.length - 1), 0);

      let point = mappingPoints[mappingPointIdx];

      if (point.x === i.numericValue) {
        j++;
        point = mappingPoints[Math.max(j, 0)];
      }

      mappings[i.numericValue - input.min] = point.y;
    }
  } else {
    mappings = generateInterpolatedOutput(mappingPoints, input, output);
  }

  return mappings;
}

function generateInterpolatedOutput(
  mappingPoints: MappingPointDTO[],
  getter: InputOutput,
  setter: InputOutput,
): number[] {
  const mappings = Array.from({ length: getter.values.length }, () => setter.min);

  if (mappingPoints.length === 0) return mappings;

  if (mappingPoints.length === 1) {
    return mappings.fill(mappingPoints[0].x);
  }

  // clone + extend points
  const points: MappingPointDTO[] = [...mappingPoints];

  if (points[0].x !== getter.min) {
    points.unshift({
      x: getter.min,
      y: points[0].y,
    });
  }

  if (points[points.length - 1].x !== getter.max) {
    points.push({
      x: getter.max,
      y: points[points.length - 1].y,
    });
  }

  // interpolation
  for (let i = 0; i < points.length - 1; i++) {
    const low = points[i];
    const high = points[i + 1];

    mappings[low.x - getter.min] = low.y;

    const range = high.x - low.x;

    let useIgnoreOutput = false;
    let ignore = -1;

    if (isIgnoreValue(setter, low.y) || isIgnoreValue(setter, high.y)) {
      ignore = low.y;
      useIgnoreOutput = true;
    }

    for (let inputValue = low.x + 1; inputValue <= high.x; inputValue++) {
      const t = (inputValue - low.x) / range;

      const outputValue = useIgnoreOutput ? ignore : Math.round((1 - t) * low.y + t * high.y);

      mappings[inputValue - getter.min] = outputValue;
    }
  }

  return mappings;
}

export const withNewPoints = (action: MMAction, newPoints: MappingPointDTO[]): MMAction => {
  const newAction = { ...action };
  const filteredPoints = newPoints.filter((p) => !isIgnoreValue(action.output, p.y));
  console.log("filtered points:", filteredPoints);
  newAction.mappingPoints = filteredPoints;
  newAction.mappedOutputs = generateMappings(filteredPoints, action.input, action.output);
  return newAction;
};

export const fetchActionWithPoints = async (
  actionUuid: string,
  newPoints: MappingPoint[],
): Promise<MMAction> => {
  const response = await fetch(`/api/action/${actionUuid}/updatePoints`, {
    method: "POST",
    headers: {
      "Content-Type": "application/json",
    },
    body: JSON.stringify({ points: newPoints }),
  });

  if (!response.ok) {
    throw new Error(`Failed to update action points: ${response.statusText}`);
  }

  const updatedAction: MMAction = await response.json();
  return updatedAction;
};
