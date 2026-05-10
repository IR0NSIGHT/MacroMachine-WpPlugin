import { FormControl, InputLabel, Select, MenuItem } from "@mui/material";
import { Macro } from "./types/MMacro";

type Props = {
  macros: Macro[];
  selectedMacroId: string;
  onChange: (uid: string) => void;
};

export function MacroSelector({ macros, selectedMacroId, onChange }: Props) {
  // 0 macros
  if (macros.length === 0) {
    return (
      <FormControl size="small" fullWidth disabled>
        <InputLabel>Macro</InputLabel>
        <Select value="">
          <MenuItem value="">
            <em>No macros available</em>
          </MenuItem>
        </Select>
      </FormControl>
    );
  }

  return (
    <FormControl size="small" fullWidth>
      <InputLabel id="macro-select-label">Macro</InputLabel>

      <Select
        labelId="macro-select-label"
        value={selectedMacroId || macros[0].uid}
        label="Macro"
        onChange={(e) => onChange(e.target.value)}
      >
        {macros.map((macro) => (
          <MenuItem key={macro.uid} value={macro.uid}>
            {macro.name}
          </MenuItem>
        ))}
      </Select>
    </FormControl>
  );
}
