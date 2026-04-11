import {
  FormControl,
  InputLabel,
  Select,
  MenuItem,
} from "@mui/material";
import { InputOutput, NamedValue } from "@/types/InputOutput";

export const InputValueEditor = (props: {
  open?: boolean;
  includeIgnore: boolean;
  label: string;
  value: number;
  input: InputOutput;
  onChange: (newInput: NamedValue) => void;
}) => {
  const { open = false, includeIgnore, label, value, input, onChange } = props;

  const isIgnore = (val: number) => val === input.ignoreValue;

  const filter = includeIgnore
    ? () => true
    : (v: NamedValue) => !isIgnore(v.numericValue);

  const sortedValues = input.values
    .filter(filter)
    .sort(input.discrete
      ? (a, b) => a.displayName.localeCompare(b.displayName)
      : (a, b) => a.numericValue - b.numericValue
    );

  return (
    <FormControl fullWidth margin="normal">
      <InputLabel>{`${label}: ${input.displayName}`}</InputLabel>

      <Select
        value={value}
        label={`${label}: ${input.displayName}`}
        onChange={(e) => {
          const selected = sortedValues.find(
            (v) => v.numericValue === Number(e.target.value)
          );
          if (selected) onChange(selected);
        }}
      >
        {sortedValues.map((option) => (
          <MenuItem key={option.numericValue} value={option.numericValue}>
            {isIgnore(option.numericValue)
              ? "[Ignore]"
              : option.displayName}
          </MenuItem>
        ))}
      </Select>
    </FormControl>
  );
};