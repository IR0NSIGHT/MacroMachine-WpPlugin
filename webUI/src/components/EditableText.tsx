import TextField from "@mui/material/TextField";
import Box from "@mui/material/Box";
import { TypographyVariants, useTheme } from "@mui/material/styles";
import { FormControl, InputLabel, Select, MenuItem } from "@mui/material";

type EditableTextProps = {
    value: string;
    onChange: (value: string) => void;
    variant?: keyof TypographyVariants;
    placeholder?: string;
    label: string;
};
type UidItem = { uid: string; displayName: string };

type EditableSelectProps<T extends UidItem> = {
    value: T;
    setValue: (value: T) => void;
    options: T[];
    label: string;
};

export function EditableSelect<T extends UidItem>({
    value,
    setValue,
    options,
    label,
}: EditableSelectProps<T>) {
    return (
        <FormControl size="small" fullWidth>
            <InputLabel
                sx={(theme) => ({
                    backgroundColor: theme.palette.background.paper,
                    px: 0.5,
                })}
            >
                {label}
            </InputLabel>

            <Select
                value={value.uid}
                onChange={(e) =>
                    setValue(
                        options.find((option) => option.uid === e.target.value) ?? value
                    )
                }
            >
                {options.map((option) => (
                    <MenuItem key={option.uid} value={option.uid}>
                        {option.displayName}
                    </MenuItem>
                ))}
            </Select>
        </FormControl>
    );
}

export function EditableText({
    value,
    onChange,
    variant = "body1",
    placeholder = "Untitled",
    label
}: EditableTextProps) {
    const theme = useTheme();

    return (
        <Box
            sx={{
                position: "relative",

                "&:hover .label": {
                    opacity: 1,
                },

                "&:focus-within .label": {
                    opacity: 1,
                    color: theme.palette.primary.main,
                },
            }}
        >
            {/* label */}
            <Box
                className="label"
                sx={(theme) => ({
                    position: "absolute",
                    top: -10,
                    left: 8,
                    fontSize: 12,
                    color: theme.palette.text.secondary,
                    opacity: 0,
                    transition: "opacity 120ms ease, color 120ms ease",
                    pointerEvents: "none",
                    backgroundColor: theme.palette.background.paper,
                    px: 0.5, // small horizontal padding so text doesn't touch edges
                    zIndex: 1, // 👈 this is the missing piece
                })}
            >
                {label}
            </Box>

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
                            ...(theme.typography[variant] as any),

                            fontStyle: value ? "normal" : "italic",

                            color: value
                                ? theme.palette.text.primary
                                : theme.palette.text.secondary,

                            "& input::placeholder": {
                                color: theme.palette.text.disabled,
                                opacity: 1,
                            },

                            "& fieldset": {
                                borderColor: "transparent",
                                transition: "border-color 120ms ease",
                            },
                        }),
                    },
                }}
            />
        </Box>
    );
}