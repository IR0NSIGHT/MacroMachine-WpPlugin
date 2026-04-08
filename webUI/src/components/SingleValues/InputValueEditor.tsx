import { InputOutput, NamedValue } from "@/types/InputOutput";
import { MenuItem, TextField } from "@mui/material";

export const InputValueEditor = (props: { 
    includeIgnore: boolean, 
    label: string, 
    value: number, 
    input: InputOutput, 
    onChange: (newInput: NamedValue) => void 
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
        <TextField
            select
            fullWidth
            label={`${label}: ${input.displayName}`}
            value={value}
            onChange={(e) => {
                const selected = sortedValues.find(v => v.numericValue === Number(e.target.value));
                if (selected) onChange(selected);
            }}
            margin="normal"
        >
            {sortedValues.map(option => (
                <MenuItem key={option.numericValue} value={option.numericValue}>
                    {isIgnore(option.numericValue) ? `[Ignore]` : option.displayName}
                </MenuItem>
            ))}
        </TextField>
    );
};