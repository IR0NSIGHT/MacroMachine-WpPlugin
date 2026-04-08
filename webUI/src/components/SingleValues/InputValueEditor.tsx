import { InputOutput, NamedValue } from "@/types/InputOutput";
import { MenuItem, TextField } from "@mui/material";

export const InputValueEditor = (props: { 
    open?: boolean, 
    includeIgnore: boolean, 
    label: string, 
    value: number, 
    input: InputOutput, 
    onChange: (newInput: NamedValue) => void 
}) => {
    const { open = false, includeIgnore, label, value, input, onChange } = props;
    
    const isIgnore = (val: number) => val === input.ignoreValue;
    const filter = includeIgnore ? (v: NamedValue) => true : (v: NamedValue) => !isIgnore(v.numericValue);
    const sort = input.discrete 
        ? (a: NamedValue, b: NamedValue) => a.displayName.localeCompare(b.displayName)
        : (a: NamedValue, b: NamedValue) => a.numericValue - b.numericValue;
    
    const values = input.values.filter(filter);
    const labelText = label + ": " + input.displayName;

    return (
        <TextField
            label={labelText}
            select
            value={value}
            onChange={(e) =>
                onChange(values.find((v) => v.numericValue === Number(e.target.value))!)
            }
            sx={{
                minWidth: `${labelText.length}ch`, // roughly match the label length
                maxWidth: '100%',                   // optional, prevent overflow
            }}
            slotProps={{
                select: {
                    native: false,
                    open,
                    MenuProps: {
                        PaperProps: {
                            style: {
                                minWidth: 'auto',
                                width: 'max-content', // dropdown fits content
                            }
                        }
                    },
                }
            }}
        >
            {values.sort(sort).map((option) => (
                <MenuItem key={option.numericValue} value={option.numericValue}>
                    {isIgnore(option.numericValue) ? `[Ignore]` : option.displayName}
                </MenuItem>
            ))}
        </TextField>
    );
};