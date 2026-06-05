import { Stack, Box, Typography } from "@mui/material";
import { LayerDTO } from "./generated/client";

export const LayerManager = ({ layers }: { layers: LayerDTO[] }) => {
  return (
    <Box
      sx={{
        display: "flex",
        flexDirection: "column",
        height: "95vh", // FIXME ugly hack to make the fucking flexbox work
      }}
      p={1}
    >
      <Typography variant="h4" gutterBottom>
        Layers used in current project
      </Typography>
      <Stack
        spacing={1}
        sx={{
          flex: 1,
          minHeight: 0,
          overflowY: "auto",
          p: 2,
        }}
      >
        {layers.map((layer) => (
          <Box
            key={layer.id}
            sx={{
              border: "1px solid",
              borderColor: "divider",
              borderRadius: 1,
              p: 2,
            }}
          >
            <Typography variant="h6">{layer.name}</Typography>
            <Typography variant="body2">Type: {layer.type}</Typography>
            <Typography variant="body2">Id: {layer.id}</Typography>
            {layer.custom && <Typography variant="body2">Custom Layer</Typography>}
          </Box>
        ))}
      </Stack>
    </Box>
  );
};
