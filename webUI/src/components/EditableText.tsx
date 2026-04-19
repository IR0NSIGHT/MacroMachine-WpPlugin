import TextField from "@mui/material/TextField";
import { TypographyVariants, useTheme } from "@mui/material/styles";

type EditableTextProps = {
    value: string;
    onChange: (value: string) => void;
    variant?: keyof TypographyVariants; // "h5", "body1", etc.
    placeholder?: string;
    label: string;
};

export function EditableText({
    value,
    onChange,
    variant = "body1",
    placeholder = "Untitled",
    label
}: EditableTextProps) {
    const isEmpty = !value;
    const theme = useTheme();
    return (
        <TextField
            value={value}
            onChange={(e) => onChange(e.target.value)}
            variant="outlined"
            fullWidth
            placeholder={placeholder}
            slotProps={{
                input: {
                    disableUnderline: true,
                    sx: (theme) => ({
                        ...(theme.typography[variant] as React.CSSProperties),
                        fontStyle: value ? "normal" : "italic",
                        color: value
                            ? theme.palette.text.primary
                            : theme.palette.text.secondary,

                        "& input::placeholder": {
                            color: theme.palette.text.disabled,
                            opacity: 1,
                        },

                        // 🧼 remove default visible border
                        "& fieldset": {
                            borderColor: "transparent",
                            transition: "border-color 120ms ease",
                        },

                    })
                },
            }}
        />
    );
}