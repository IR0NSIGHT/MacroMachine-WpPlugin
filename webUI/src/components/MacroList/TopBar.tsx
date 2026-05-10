import { AppBar, Avatar, Box, IconButton, Toolbar, Typography } from "@mui/material";

import NotificationsOutlinedIcon from "@mui/icons-material/NotificationsOutlined";

export function TopBar() {
  return (
    <AppBar
      position="sticky"
      elevation={0}
      sx={(theme) => ({
        backdropFilter: "blur(16px)",
        backgroundColor: theme.palette.background.paper,
        borderBottom: `1px solid ${theme.palette.divider}`,
        color: theme.palette.text.primary,
      })}
    >
      <Toolbar
        sx={{
          minHeight: 68,
          display: "flex",
          justifyContent: "space-between",
        }}
      >
        <Box
          sx={{
            display: "flex",
            alignItems: "center",
            gap: 1.5,
          }}
        >
          <Box
            sx={(theme) => ({
              width: 14,
              height: 14,
              borderRadius: "50%",
              background: `linear-gradient(135deg,
                ${theme.palette.primary.light},
                ${theme.palette.primary.main})`,
              boxShadow: `0 0 18px ${theme.palette.primary.main}66`,
            })}
          />

          <Typography
            variant="h6"
            sx={{
              fontWeight: 700,
              letterSpacing: "-0.03em",
            }}
          >
            Nimbus
          </Typography>
        </Box>

        <Box
          sx={{
            display: "flex",
            alignItems: "center",
            gap: 1,
          }}
        >
          <IconButton
            sx={(theme) => ({
              borderRadius: 3,
              backgroundColor: theme.palette.action.hover,
              transition: "all 0.2s ease",

              "&:hover": {
                backgroundColor: theme.palette.action.selected,
              },
            })}
          >
            <NotificationsOutlinedIcon />
          </IconButton>

          <Avatar
            sx={(theme) => ({
              width: 40,
              height: 40,
              fontWeight: 700,
              background: `linear-gradient(135deg,
                ${theme.palette.primary.main},
                ${theme.palette.secondary.main})`,
            })}
          >
            N
          </Avatar>
        </Box>
      </Toolbar>
    </AppBar>
  );
}
