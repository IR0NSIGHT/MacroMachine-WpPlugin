import { Box, CircularProgress } from "@mui/material";

export const PageLoadingSpinner = () => {
  return (
    <Box
      sx={{
        display: "flex",
        justifyContent: "center",
        alignItems: "center",
        height: "100%",
      }}
    >
      <CircularProgress aria-label="Loading…" />
    </Box>
  );
};
