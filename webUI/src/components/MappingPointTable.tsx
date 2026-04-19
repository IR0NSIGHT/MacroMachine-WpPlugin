import { MappingPoint } from "@/types/MappingPoint";
import {
    ButtonGroup,
    Button,
    useTheme,
    TextField,
    Table,
    TableBody,
    TableCell,
    TableContainer,
    TableHead,
    TableRow,
    Paper,
    Checkbox,
    FormControlLabel,
    TableSortLabel,
    Stack
} from "@mui/material";
import { useMemo, useState } from "react";
import { InputValueMenu } from "./SingleValues/InputValueEditor";
import { NamedValue } from "@/types/InputOutput";

type Props = {
    points: MappingPoint[],
    setPoints: (points: MappingPoint[]) => void
}

const getDisplayNameInput = (p: MappingPoint) => {
    const value = p.input.values.find(v => v.numericValue === p.x);
    return value ? p.input.displayName + " " + value.displayName : p.x + "?";
};

const getDisplayNameOutput = (point: MappingPoint) => {
    const value = point.output.values.find(iteratorPoint => iteratorPoint.numericValue === point.y);
    return value ? value.displayName : point.y + "?";
};

export const MappingPointTable = ({ points, setPoints }: Props) => {
    const theme = useTheme();
    const [search, setSearch] = useState("");
    const [hideIgnoreValues, setHideIgnoreValues] = useState(true);
    const [editingCell, setEditingCell] = useState<{ id: string, field: 'x' | 'y', anchor: HTMLElement | null } | null>(null);

    const [sortKey, setSortKey] = useState<'input' | 'output'>(points[0].input.discrete ? 'output' : 'input'); // discrete inputs care about output first, continuous inputs care about input first
    const [sortDirection, setSortDirection] = useState<'asc' | 'desc'>(points[0].input.discrete ? 'desc' : 'asc'); //ignore values are the first in asc

    const handleSort = (key: 'input' | 'output') => {
        if (sortKey === key) {
            setSortDirection(prev => (prev === 'asc' ? 'desc' : 'asc'));
        } else {
            setSortKey(key);
            setSortDirection('asc');
        }
    };

    const rows = useMemo(() => {
        const mapped = points.map(p => ({
            ...p,
            id: "row_" + p.x,
            inputDisplay: getDisplayNameInput(p),
            outputDisplay: getDisplayNameOutput(p)
        }));

        const sorted = mapped.sort((a, b) => {
            const aPrimary = sortKey === 'input' ? a.inputDisplay : a.outputDisplay;
            const bPrimary = sortKey === 'input' ? b.inputDisplay : b.outputDisplay;

            const primaryCompare = aPrimary.localeCompare(bPrimary);

            if (primaryCompare !== 0) {
                return sortDirection === 'asc' ? primaryCompare : -primaryCompare;
            }

            // tie-breaker (secondary sort)
            const aSecondary = sortKey === 'input' ? a.outputDisplay : a.inputDisplay;
            const bSecondary = sortKey === 'input' ? b.outputDisplay : b.inputDisplay;

            const secondaryCompare = aSecondary.localeCompare(bSecondary);

            return sortDirection === 'asc' ? secondaryCompare : -secondaryCompare;
        });

        return sorted;
    }, [points, sortKey, sortDirection]);

    const updatePoints = (updateValue: MappingPoint) => {
        const updated = points.map(point =>
            point.x === updateValue.x ? { ...updateValue } : { ...point }
        );
        setPoints(updated);
    };

    const filteredRows = rows
        .filter((row) =>
            Object.values(row).some((value) =>
                String(value).toLowerCase().includes(search.toLowerCase())
            )
        )
        .filter((row) =>
            hideIgnoreValues ? row.y !== row.output.ignoreValue : true
        );

    const handleSelect = (row: any, field: 'x' | 'y', val: NamedValue) => {
        const updatedRow = {
            ...row,
            [field]: val.numericValue
        };
        updatePoints(updatedRow);
        setEditingCell(null);
    };

    const usePaginationAndSearch = points.length > 5;

    return (
        <Paper variant="outlined" sx={{ p: 2 }}>
            <Stack direction="row" spacing={2} alignItems="center" sx={{ mb: 2 }}>
                {usePaginationAndSearch && (
                    <TextField
                        label="Search"
                        size="small"
                        value={search}
                        onChange={(e) => setSearch(e.target.value)}
                        sx={{ mb: 2 }}
                    />
                )}

                <FormControlLabel
                    control={
                        <Checkbox
                            checked={hideIgnoreValues}
                            onChange={(e) => setHideIgnoreValues(e.target.checked)}
                        />
                    }
                    label="Hide ignore values"
                />
            </Stack>


            <TableContainer>
                <Table>
                    <TableHead>
                        <TableRow>
                            <TableCell>
                                <TableSortLabel
                                    active={sortKey === 'input'}
                                    direction={sortKey === 'input' ? sortDirection : 'asc'}
                                    onClick={() => handleSort('input')}
                                >
                                    Input
                                </TableSortLabel>
                            </TableCell>

                            <TableCell>
                                <TableSortLabel
                                    active={sortKey === 'output'}
                                    direction={sortKey === 'output' ? sortDirection : 'asc'}
                                    onClick={() => handleSort('output')}
                                >
                                    Output
                                </TableSortLabel>
                            </TableCell>
                        </TableRow>
                    </TableHead>

                    <TableBody>
                        {filteredRows.map((row) => (
                            <TableRow key={row.id}>
                                <TableCell
                                    onClick={(e) =>
                                        setEditingCell({ id: row.id, field: 'x', anchor: e.currentTarget })
                                    }
                                    sx={{ cursor: 'pointer', color: theme.palette.primary.contrastText }}
                                >
                                    {row.inputDisplay}
                                </TableCell>

                                <TableCell
                                    onClick={(e) =>
                                        setEditingCell({ id: row.id, field: 'y', anchor: e.currentTarget })
                                    }
                                    sx={{ cursor: 'pointer', color: theme.palette.primary.contrastText }}
                                >
                                    {row.outputDisplay}
                                </TableCell>

                                {editingCell?.id === row.id && (
                                    <InputValueMenu
                                        input={editingCell.field === 'x' ? row.input : row.output}
                                        sortedValues={(editingCell.field === 'x' ? row.input : row.output).values}
                                        anchorEl={editingCell.anchor}
                                        open={true}
                                        onClose={() => setEditingCell(null)}
                                        onSelect={(val) => handleSelect(row, editingCell.field, val)}
                                    />
                                )}
                            </TableRow>
                        ))}
                    </TableBody>
                </Table>
            </TableContainer>
        </Paper>
    );
};