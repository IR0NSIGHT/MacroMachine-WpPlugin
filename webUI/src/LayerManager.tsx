import {
  Stack,
  Box,
  Typography,
  Grid,
  Card,
  CardContent,
  Avatar,
  ButtonGroup,
  Switch,
  FormControlLabel,
  Pagination,
  FormControl,
  InputLabel,
  MenuItem,
  Select,
} from "@mui/material";
import { LayerDTO } from "./generated/client";
import { useEffect, useMemo, useState } from "react";
import { Search, SearchIconWrapper, StyledInputBase } from "./MacroGrid";
import SearchIcon from "@mui/icons-material/Search";

export const LayerManager = ({ layers }: { layers: LayerDTO[] }) => {
  const [onlyProject, setOnlyProject] = useState(false);
  const [onlyUsedInMacros, setOnlyUsedInMacros] = useState(false);
  const [onlyCustom, setOnlyCustom] = useState(false);
  const [selectedType, setSelectedType] = useState<string>("all");
  const [search, setSearch] = useState("");
  const PAGE_SIZE = 50;
  const [page, setPage] = useState(0);

  const sortedFilteredLayers = useMemo(() => {
    let filtered = layers
      .filter((layer) => !onlyUsedInMacros || (layer.macrosUsingLayer?.length ?? 0) > 0)
      .filter((layer) => !onlyProject || layer.presentInProject)
      .filter((layer) => !onlyCustom || layer.custom)
      .filter((layer) => selectedType === "all" || layer.type === selectedType)
      .filter((layer) => search === "" || layer.name.toLowerCase().includes(search.toLowerCase()));

    return filtered.sort((a, b) => a.name.localeCompare(b.name));
  }, [layers, onlyProject, onlyUsedInMacros, onlyCustom, selectedType, search]);

  useEffect(() => {
    setPage(0);
  }, [sortedFilteredLayers]);

  const pagedLayers = useMemo(() => {
    const start = page * PAGE_SIZE;
    return sortedFilteredLayers.slice(start, start + PAGE_SIZE);
  }, [sortedFilteredLayers, page]);

  const layerTypes = useMemo(() => {
    const types = new Set<string>();
    layers.filter((layer) => layer.custom).forEach((layer) => types.add(layer.type));
    return Array.from(types);
  }, [layers]);

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
        Layers known to MacroMachine
      </Typography>
      <Search>
        <SearchIconWrapper>
          <SearchIcon />
        </SearchIconWrapper>
        <StyledInputBase
          placeholder="Search…"
          value={search}
          onChange={(e) => setSearch(e.target.value)}
          inputProps={{ "aria-label": "search" }}
        />
      </Search>
      <ButtonGroup sx={{ pl: 2 }}>
        <FormControlLabel
          control={
            <Switch checked={onlyProject} onChange={(e) => setOnlyProject(e.target.checked)} />
          }
          label="Only in current project"
        />
        <FormControlLabel
          control={
            <Switch
              checked={onlyUsedInMacros}
              onChange={(e) => setOnlyUsedInMacros(e.target.checked)}
            />
          }
          label="Only used in macros"
        />
        <FormControlLabel
          control={
            <Switch checked={onlyCustom} onChange={(e) => setOnlyCustom(e.target.checked)} />
          }
          label="Only custom"
        />
        <FormControl size="small" sx={{ minWidth: 200 }}>
          <InputLabel>Layer Type</InputLabel>
          <Select
            value={selectedType}
            label="Layer Type"
            onChange={(e) => setSelectedType(e.target.value)}
          >
            <MenuItem value="all">All Types</MenuItem>

            {layerTypes.map((type) => (
              <MenuItem key={type} value={type}>
                {type}
              </MenuItem>
            ))}
          </Select>
        </FormControl>
      </ButtonGroup>
      <Box
        sx={{
          flex: 1,
          minHeight: 0,
          overflowY: "auto",
          p: 2,
        }}
      >
        <Grid container spacing={2}>
          {pagedLayers.length === 0 && (
            <Typography
              variant="body1"
              color="text.secondary"
              align="center"
              sx={{ width: "100%", mt: 4 }}
            >
              No layers found.
            </Typography>
          )}
          {pagedLayers.length !== 0 && (
            <Typography
              variant="body1"
              color="text.secondary"
              align="center"
              sx={{ width: "100%", mt: 4 }}
            >
              {sortedFilteredLayers.length} layers
            </Typography>
          )}
          {pagedLayers.map((layer) => (
            <Grid key={layer.id} size={{ xs: 12, sm: 4, md: 3, lg: 2 }}>
              <Card
                variant="outlined"
                sx={{
                  height: "100%",
                }}
              >
                <CardContent>
                  <Box
                    sx={{
                      display: "flex",
                      gap: 2,
                      alignItems: "flex-start",
                    }}
                  >
                    {/* Icon */}
                    <Avatar
                      variant="rounded"
                      src={`http://localhost:8080/api/layers/${layer.id}/icon`}
                      sx={{
                        width: 48,
                        height: 48,
                        flexShrink: 0,
                      }}
                    />

                    {/* Content */}
                    <Stack spacing={0.5} sx={{ minWidth: 0 }}>
                      <Typography variant="h6">{layer.name}</Typography>

                      <Typography variant="body2" color="text.secondary">
                        Type: {layer.type}
                      </Typography>

                      <Typography
                        variant="body2"
                        color="text.secondary"
                        sx={{ wordBreak: "break-all" }}
                      >
                        Id: {layer.id}
                        <br />
                        Used in {layer.macrosUsingLayer?.length ?? 0} macros
                        <br />
                        Custom: {layer.custom ? "Yes" : "No"}
                        <br />
                        Present in project: {layer.presentInProject ? "Yes" : "No"}
                      </Typography>
                    </Stack>
                  </Box>
                </CardContent>
              </Card>
            </Grid>
          ))}
        </Grid>
      </Box>
      <Pagination
        count={Math.ceil(sortedFilteredLayers.length / PAGE_SIZE)}
        page={page + 1}
        onChange={(_, value) => setPage(value - 1)}
      />
    </Box>
  );
};
