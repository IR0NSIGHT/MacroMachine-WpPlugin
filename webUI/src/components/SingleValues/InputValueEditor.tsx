import { InputOutput, NamedValue } from "@/types/InputOutput"
import { TextField } from "@mui/material"

export const InputValueEditor = (props: { label: string, value: number, input: InputOutput, onChange: (newInput: NamedValue) => void }) => {
    return (<TextField
        label={props.label}
        select
        value={props.value}
        onChange={(e) => props.onChange(props.input.values.find((v) => v.numericValue === Number(e.target.value))! )}
        SelectProps={{ native: true }}
    >
        {props.input.values.sort((a, b) => a.displayName.localeCompare(b.displayName)).map((option) => (
            <option key={option.numericValue} value={option.numericValue}>
                {option.displayName}
            </option>
        ))}
    </TextField>)
}