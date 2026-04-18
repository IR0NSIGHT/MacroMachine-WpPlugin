import {
  FormControl,
  InputLabel,
  Select,
  MenuItem,
  Menu,
} from "@mui/material";
import { InputOutput, NamedValue } from "@/types/InputOutput";

type InputValueSelectProps = {
  value: number;
  input: InputOutput;
  sortedValues: NamedValue[];
  onSelect: (newInput: NamedValue) => void;
  label: String
}


export const InputValueMenu = ({
  input,
  sortedValues,
  onSelect,
  anchorEl,
  open,
  onClose,
}: {
  input: InputOutput;
  sortedValues: NamedValue[];
  onSelect: (value: NamedValue) => void;
  anchorEl: HTMLElement | null;
  open: boolean;
  onClose: () => void;
}) => {
  const isIgnore = (val: number) => val === input.ignoreValue;

  return (
    <Menu
      anchorEl={anchorEl}
      open={open}
      onClose={onClose}
    >
      {sortedValues.map((option) => (
        <MenuItem
          key={option.numericValue}
          onClick={() => {
            onSelect(option);
            onClose();
          }}
        >
          {isIgnore(option.numericValue)
            ? "[Ignore]"
            : option.displayName}
        </MenuItem>
      ))}
    </Menu>
  );
};

export const InputValueSelect = ({ value, input, sortedValues, onSelect, label }: InputValueSelectProps) => {
  const isIgnore = (val: number) => val === input.ignoreValue;

  return (<Select
    value={value}
    label={`${label}: ${input.displayName}`}
    onChange={(e) => {
      const selected = sortedValues.find(
        (v) => v.numericValue === Number(e.target.value)
      );
      if (selected) onSelect(selected);
    }}
  >
    {sortedValues.map((option) => (
      <MenuItem key={option.numericValue} value={option.numericValue}>
        {isIgnore(option.numericValue)
          ? "[Ignore]"
          : option.displayName}
      </MenuItem>
    ))}
  </Select>)
};

export const InputValueEditor = (props: {
  open?: boolean;
  includeIgnore: boolean;
  label: string;
  value: number;
  input: InputOutput;
  onChange: (newInput: NamedValue) => void;
}) => {
  const { includeIgnore, label, value, input, onChange } = props;

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
      <InputValueSelect value={value} input={input} sortedValues={sortedValues} onSelect={onChange} label={label} />
    </FormControl>
  );
};