import Typography from "@mui/material/Typography";
import Stack from "@mui/material/Stack";
import Box from "@mui/material/Box";
import Paper from "@mui/material/Paper";
import Chip from "@mui/material/Chip";
import { PROVIDER_DESCRIPTIONS } from "../../types/ProviderType";
import { InputOutput } from "@/types/InputOutput";

interface InputProviderProps {
  inputOutput: InputOutput;
  type: "input" | "output";
}

export default function InputOutputDisplay({ inputOutput, type }: InputProviderProps) {
  const displayName = inputOutput.displayName || "UNKNOWN";
  const description = PROVIDER_DESCRIPTIONS[displayName] || "Custom provider";
  return (
    <Paper variant="outlined" sx={{ p: 2 }}>
      <Stack spacing={1}>
        <Box>
          <Typography variant="subtitle2" color="primary" gutterBottom>
            {type === "input" ? "Input Provider" : "Output Provider"}
          </Typography>
          <Chip label={displayName} size="small" color="primary" variant="outlined" />
        </Box>
        <Typography variant="caption" color="text.secondary">
          {description}
        </Typography>
        {inputOutput.parameters && inputOutput.parameters.length > 0 && (
          <Box>
            <Typography variant="caption" component="div" color="text.secondary">
              <strong>Data:</strong>
            </Typography>

            <Box component="ul" sx={{ pl: 2, m: 0 }}>
              {inputOutput.parameters.map((param) => (
                <Typography
                  key={param.name}
                  component="li"
                  variant="caption"
                  sx={{ fontFamily: "monospace" }}
                >
                  {param.name}: {param.value}
                </Typography>
              ))}
            </Box>
          </Box>
        )}
      </Stack>
    </Paper>
  );
}
