import { ActionDTO } from "@/types/DTO";
import { namedMapping } from "./Filters";
import { StepItemType } from "./Execution";

export const explainAlwaysAction = (item: ActionDTO): string => {
  if (item.input.type !== "ALWAYS") return "Complex: " + item.name;

  const all = namedMapping(item);
  const [incrementStr, byStr] = (() => {
    switch (item.actionType as string) {
      case "DIVIDE":
        return ["divide", "by"];
      case "INCREMENT":
        return ["increment", "by"];
      case "LIMIT_TO":
        return ["limit", "to"];
      case "MULTIPLY":
        return "multiplies";
      case "SET":
        return ["set", "to"];
      case "AT_LEAST":
        return ["set ", "to at least"];
      case "DECREMENT":
        return ["decrement", "by"];
      default:
        return [item.actionType, "??"];
    }
  })();
  return [
    incrementStr,
    item.output.displayName,
    byStr,
    all.map((mapping) => mapping.outputName),
  ].join(" ");
};

export const actionAutoName = (action: StepItemType): StepItemType => {
  if (action.input.type === "ALWAYS") return { ...action, name: explainAlwaysAction(action) };
  else return { ...action };
};
