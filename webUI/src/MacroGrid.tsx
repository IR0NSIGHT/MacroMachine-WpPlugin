import {
  Box,
  Tooltip,
  FormControlLabel,
  Switch,
  Grid,
  alpha,
  InputBase,
  styled,
} from "@mui/material";
import { useMemo, useState } from "react";
import { ActionDetailsDialog } from "./components/MacroList/ActionDetailsDialog";
import MacroCard from "./components/MacroList/MacroCard";
import { MacroDetailsDialog } from "./components/MacroList/MacroDetailsDialog";
import { MacroDTO, ActionDTO, ExecutionStateDTO, isMacroDTO } from "./types/DTO";
import Item from "@mui/material/Grid";
import { MacroExecuteRequester, runnableMacro, toRunnable, uuid } from "./features/Execution";
import Typography from "@mui/material/Typography";
import SearchIcon from "@mui/icons-material/Search";
import { PageLoadingSpinner } from "./PageLoadingSpinner";

const imageURLs = [
  "https://images.unsplash.com/photo-1506744038136-46273834b3fb?q=80&w=400&auto=format&fit=crop",
  "https://images.unsplash.com/photo-1500530855697-b586d89ba3ee?q=80&w=400&auto=format&fit=crop",
  "https://images.unsplash.com/photo-1470770841072-f978cf4d019e?q=80&w=400&auto=format&fit=crop",
  "https://images.unsplash.com/photo-1441974231531-c6227db76b6e?q=80&w=400&auto=format&fit=crop",
  "https://images.unsplash.com/photo-1501785888041-af3ef285b470?q=80&w=400&auto=format&fit=crop",
  "https://images.unsplash.com/photo-1439066615861-d1af74d74000?q=80&w=400&auto=format&fit=crop",
  "https://images.unsplash.com/photo-1464822759023-fed622ff2c3b?q=80&w=400&auto=format&fit=crop",
  "https://images.unsplash.com/photo-1500375592092-40eb2168fd21?q=80&w=400&auto=format&fit=crop",
  "https://images.unsplash.com/photo-1433086966358-54859d0ed716?q=80&w=400&auto=format&fit=crop",
  "https://images.unsplash.com/photo-1426604966848-d7adac402bff?q=80&w=400&auto=format&fit=crop",
  "https://images.unsplash.com/photo-1447752875215-b2761acb3c5d?q=80&w=400&auto=format&fit=crop",
  "https://images.unsplash.com/photo-1443890923422-7819ed4101c0?q=80&w=400&auto=format&fit=crop",
  "https://images.unsplash.com/photo-1507525428034-b723cf961d3e?q=80&w=400&auto=format&fit=crop",
  "https://images.unsplash.com/photo-1493246507139-91e8fad9978e?q=80&w=400&auto=format&fit=crop",
  "https://images.unsplash.com/photo-1469474968028-56623f02e42e?q=80&w=400&auto=format&fit=crop",
  "https://images.unsplash.com/photo-1451187580459-43490279c0fa?q=80&w=400&auto=format&fit=crop",
  "https://images.unsplash.com/photo-1418065460487-3e41a6c84dc5?q=80&w=400&auto=format&fit=crop",
  "https://images.unsplash.com/photo-1502082553048-f009c37129b9?q=80&w=400&auto=format&fit=crop",
  "https://images.unsplash.com/photo-1448375240586-882707db888b?q=80&w=400&auto=format&fit=crop",
  "https://images.unsplash.com/photo-1472396961693-142e6e269027?q=80&w=400&auto=format&fit=crop",
  "https://images.unsplash.com/photo-1465146344425-f00d5f5c8f07?q=80&w=400&auto=format&fit=crop",
  "https://images.unsplash.com/photo-1431794062232-2a99a5431c6c?q=80&w=400&auto=format&fit=crop",
  "https://images.unsplash.com/photo-1500534314209-a25ddb2bd429?q=80&w=400&auto=format&fit=crop",
  "https://images.unsplash.com/photo-1458668383970-8ddd3927deed?q=80&w=400&auto=format&fit=crop",
  "https://images.unsplash.com/photo-1472214103451-9374bd1c798e?q=80&w=400&auto=format&fit=crop",
  "https://images.unsplash.com/photo-1501594907352-04cda38ebc29?q=80&w=400&auto=format&fit=crop",
  "https://images.unsplash.com/photo-1449034446853-66c86144b0ad?q=80&w=400&auto=format&fit=crop",
  "https://images.unsplash.com/photo-1465189684280-6a8fa9b19a7a?q=80&w=400&auto=format&fit=crop",
  "https://images.unsplash.com/photo-1504208434309-cb69f4fe52b0?q=80&w=400&auto=format&fit=crop",
  "https://images.unsplash.com/photo-1505761671935-60b3a7427bad?q=80&w=400&auto=format&fit=crop",

  "https://images.unsplash.com/photo-1505761671935-60b3a7427bad?q=80&w=400&auto=format&fit=crop",
  "https://images.unsplash.com/photo-1500530855697-b586d89ba3ee?q=80&w=400&auto=format&fit=crop",
  "https://images.unsplash.com/photo-1493246507139-91e8fad9978e?q=80&w=400&auto=format&fit=crop",
  "https://images.unsplash.com/photo-1506744038136-46273834b3fb?q=80&w=400&auto=format&fit=crop",
  "https://images.unsplash.com/photo-1494526585095-c41746248156?q=80&w=400&auto=format&fit=crop",
  "https://images.unsplash.com/photo-1470770841072-f978cf4d019e?q=80&w=400&auto=format&fit=crop",
  "https://images.unsplash.com/photo-1501785888041-af3ef285b470?q=80&w=400&auto=format&fit=crop",
  "https://images.unsplash.com/photo-1519608487953-e999c86e7455?q=80&w=400&auto=format&fit=crop",
  "https://images.unsplash.com/photo-1441974231531-c6227db76b6e?q=80&w=400&auto=format&fit=crop",
  "https://images.unsplash.com/photo-1433086966358-54859d0ed716?q=80&w=400&auto=format&fit=crop",

  "https://images.unsplash.com/photo-1472214103451-9374bd1c798e?q=80&w=400&auto=format&fit=crop",
  "https://images.unsplash.com/photo-1465146344425-f00d5f5c8f07?q=80&w=400&auto=format&fit=crop",
  "https://images.unsplash.com/photo-1469474968028-56623f02e42e?q=80&w=400&auto=format&fit=crop",
  "https://images.unsplash.com/photo-1448375240586-882707db888b?q=80&w=400&auto=format&fit=crop",
  "https://images.unsplash.com/photo-1431794062232-2a99a5431c6c?q=80&w=400&auto=format&fit=crop",
  "https://images.unsplash.com/photo-1447752875215-b2761acb3c5d?q=80&w=400&auto=format&fit=crop",
  "https://images.unsplash.com/photo-1473448912268-2022ce9509d8?q=80&w=400&auto=format&fit=crop",
  "https://images.unsplash.com/photo-1426604966848-d7adac402bff?q=80&w=400&auto=format&fit=crop",
  "https://images.unsplash.com/photo-1464822759023-fed622ff2c3b?q=80&w=400&auto=format&fit=crop",
  "https://images.unsplash.com/photo-1502082553048-f009c37129b9?q=80&w=400&auto=format&fit=crop",

  "https://images.unsplash.com/photo-1507525428034-b723cf961d3e?q=80&w=400&auto=format&fit=crop",
  "https://images.unsplash.com/photo-1449034446853-66c86144b0ad?q=80&w=400&auto=format&fit=crop",
  "https://images.unsplash.com/photo-1464820453369-31d2c0b651af?q=80&w=400&auto=format&fit=crop",
  "https://images.unsplash.com/photo-1451187580459-43490279c0fa?q=80&w=400&auto=format&fit=crop",
  "https://images.unsplash.com/photo-1482192596544-9eb780fc7f66?q=80&w=400&auto=format&fit=crop",
  "https://images.unsplash.com/photo-1469854523086-cc02fe5d8800?q=80&w=400&auto=format&fit=crop",
  "https://images.unsplash.com/photo-1482192505345-5655af888cc4?q=80&w=400&auto=format&fit=crop",
  "https://images.unsplash.com/photo-1500534314209-a25ddb2bd429?q=80&w=400&auto=format&fit=crop",
  "https://images.unsplash.com/photo-1454496522488-7a8e488e8606?q=80&w=400&auto=format&fit=crop",
  "https://images.unsplash.com/photo-1501594907352-04cda38ebc29?q=80&w=400&auto=format&fit=crop",

  "https://images.unsplash.com/photo-1482192505345-5655af888cc4?q=80&w=400&auto=format&fit=crop",
  "https://images.unsplash.com/photo-1462331940025-496dfbfc7564?q=80&w=400&auto=format&fit=crop",
  "https://images.unsplash.com/photo-1491553895911-0055eca6402d?q=80&w=400&auto=format&fit=crop",
  "https://images.unsplash.com/photo-1500048993953-d23a436266cf?q=80&w=400&auto=format&fit=crop",
  "https://images.unsplash.com/photo-1499092346589-b9b6be3e94b2?q=80&w=400&auto=format&fit=crop",
  "https://images.unsplash.com/photo-1504384308090-c894fdcc538d?q=80&w=400&auto=format&fit=crop",
  "https://images.unsplash.com/photo-1500534623283-312aade485b7?q=80&w=400&auto=format&fit=crop",
  "https://images.unsplash.com/photo-1495567720989-cebdbdd97913?q=80&w=400&auto=format&fit=crop",
  "https://images.unsplash.com/photo-1504208434309-cb69f4fe52b0?q=80&w=400&auto=format&fit=crop",
  "https://images.unsplash.com/photo-1507149833265-60c372daea22?q=80&w=400&auto=format&fit=crop",
]; //FIXME images based on UUID until backend supports saving images

export const Search = styled("div")(({ theme }) => ({
  position: "relative",
  borderRadius: theme.shape.borderRadius,
  backgroundColor: alpha(theme.palette.common.white, 0.15),
  "&:hover": {
    backgroundColor: alpha(theme.palette.common.white, 0.25),
  },
  marginRight: theme.spacing(2),
  marginLeft: 0,
  width: "100%",
  [theme.breakpoints.up("sm")]: {
    marginLeft: theme.spacing(3),
    width: "auto",
  },
}));

export const SearchIconWrapper = styled("div")(({ theme }) => ({
  padding: theme.spacing(0, 2),
  height: "100%",
  position: "absolute",
  pointerEvents: "none",
  display: "flex",
  alignItems: "center",
  justifyContent: "center",
}));

export const StyledInputBase = styled(InputBase)(({ theme }) => ({
  color: "inherit",
  "& .MuiInputBase-input": {
    padding: theme.spacing(1, 1, 1, 0),
    // vertical padding + font size from searchIcon
    paddingLeft: `calc(1em + ${theme.spacing(4)})`,
    transition: theme.transitions.create("width"),
    width: "100%",
    [theme.breakpoints.up("md")]: {
      width: "20ch",
    },
  },
}));

const searchFilter = (search: string) => {
  return (Macro: MacroDTO) => {
    return (
      Macro.name.toLowerCase().includes(search.toLowerCase()) ||
      Macro.description.toLowerCase().includes(search.toLowerCase())
    );
  };
};

export function MacroGrid({
  macros,
  actions,
  executionState,
  onRequestExecution,
  onDeleteMacro,
}: {
  macros?: MacroDTO[];
  actions?: ActionDTO[];
  executionState?: ExecutionStateDTO;
  onRequestExecution: MacroExecuteRequester;
  onDeleteMacro: (uuid: uuid) => void;
}) {
  if (!macros || !actions || !executionState) {
    return <PageLoadingSpinner />;
  }

  const [search, setSearch] = useState("");
  const [hideNested, setHideNested] = useState(false);
  const [viewedMacro, setViewedMacro] = useState<runnableMacro | undefined>(undefined);
  const [viewAction, setViewAction] = useState<ActionDTO | undefined>(undefined);

  const nestedMacroUIDs = useMemo(() => {
    const nestedMacroUIDs = new Set<string>();
    const macroSet = new Set<string>();

    macros.forEach((m) => macroSet.add(m.uid));
    const uuidToMacroOrAction = new Map<string, MacroDTO | ActionDTO>();
    macros.forEach((macro) => uuidToMacroOrAction.set(macro.uid, macro)); //FIXME useMemo ? or sth?
    actions.forEach((action) => uuidToMacroOrAction.set(action.uid, action));
    macros.forEach((macro) =>
      macro.executionUUIDs
        .filter((uid) => macroSet.has(uid))
        .forEach((uid) => nestedMacroUIDs.add(uid)),
    ); //FIXME useMemo ? or sth?
    return nestedMacroUIDs;
  }, [macros]);

  const sortedFilteredMacros = useMemo(() => {
    const filterHideNested = (macro: MacroDTO) => {
      const isNested = nestedMacroUIDs.has(macro.uid);
      return hideNested ? !isNested : true;
    };

    return macros
      .filter(searchFilter(search))
      .filter(filterHideNested)
      .sort((a, b) => a.name.localeCompare(b.name));
  }, [macros, search]);

  const onShare = (macro: MacroDTO) => {
    console.log("USER WANTS TO SHARE THE MARCO", macro.name);
  };

  const onEdit = (macro: MacroDTO) => {
    console.log("USER WANTS TO EDIT THE MARCO", macro.name);
  };

  const onView = (macro: MacroDTO) => {
    const runnable = toRunnable(macro, actions, macros);
    setViewedMacro(runnable);
  };

  const uidToImageURL = (uid: string): string => {
    function uuidToShortNumber(uuid: string): number {
      return parseInt(uuid.replace(/-/g, "").slice(0, 12), 16);
    }
    const numberVal: number = uuidToShortNumber(uid);
    return imageURLs[numberVal % imageURLs.length];
  };

  return (
    <Box
      sx={{
        display: "flex",
        flexDirection: "column",
        height: "95vh", // FIXME ugly hack to make the fucking flexbox work
      }}
      p={1}
    >
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
      <Tooltip title="Hide all macros, that are used by another macro.">
        <FormControlLabel
          control={
            <Switch
              checked={hideNested}
              onChange={(event) => {
                setHideNested(event.target.checked);
              }}
            />
          }
          label={"Hide " + nestedMacroUIDs.size + " nested macro(s)"}
        />
      </Tooltip>
      <Box
        sx={{
          flex: 1,
          minHeight: 0,
          overflowY: "auto",
          p: 2,
        }}
      >
        {(!macros || macros.length === 0) && (
          <Typography>
            You dont have any macros yet. Create some or import existing ones.
          </Typography>
        )}
        <Grid container spacing={{ xs: 1, md: 4 }}>
          {sortedFilteredMacros.map((macro) => (
            <Grid
              key={macro.uid}
              size={{
                xs: 12,
                sm: 6,
                md: 4,
                lg: 3,
              }}
            >
              <Item>
                <MacroCard
                  macro={macro}
                  execution={executionState}
                  imageURL={uidToImageURL(macro.uid)}
                  onRequestExecution={() => onRequestExecution(macro.uid, false)}
                  onView={() => onView(macro)}
                  onShare={() => onShare(macro)}
                  onEdit={() => onEdit(macro)}
                  onDelete={() => onDeleteMacro(macro.uid)}
                  onSetFavorite={alert}
                />
              </Item>
            </Grid>
          ))}
        </Grid>

        <MacroDetailsDialog
          open={!!viewedMacro}
          macro={viewedMacro}
          onClose={() => setViewedMacro(undefined)}
          onViewItem={(item: MacroDTO | ActionDTO) => {
            if (isMacroDTO(item)) {
              onView(item);
            } else {
              setViewAction(item);
            }
          }}
        />
        <ActionDetailsDialog
          open={!!viewAction}
          action={viewAction}
          setAction={(_action) => {}}
          onClose={() => setViewAction(undefined)}
          onViewItem={() => {}}
        />
      </Box>
    </Box>
  );
}
