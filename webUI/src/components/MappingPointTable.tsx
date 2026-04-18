import { MappingPoint } from "@/types/MappingPoint";
import { ButtonGroup, Button, useTheme, TextField, Theme } from "@mui/material";
import { DataGrid, GridColDef, GridRenderEditCellParams, GridRowModel, GridToolbar } from '@mui/x-data-grid';
import { useState } from "react";
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

const cellSystem = (theme: Theme) => {
    const renderCellInput = (params: any) => {
        return (
            <text
                fill={theme.palette.primary.contrastText}
                pointerEvents="none"
            >
                {params.row.inputDisplay}
            </text>
        )
    }

    const renderCellOutput = (params: any) => {
        return (
            <text
                fill={theme.palette.primary.contrastText}
                pointerEvents="none"
            >
                {params.row.outputDisplay}
            </text>
        )
    }


    const inputCellEditor = (params: GridRenderEditCellParams) => {
        const onSelect = (val: NamedValue) => {
            params.api.setEditCellValue({
                id: params.id,
                field: params.field,
                value: val.numericValue,
            });

            // close edit mode after selection
            params.api.stopCellEditMode({
                id: params.id,
                field: params.field,
            });
        };

        const isInput = params.field == "x";
        const inputOutput = isInput ? params.row.input : params.row.output;
        return (
            <InputValueMenu
                input={inputOutput}
                sortedValues={inputOutput.values}
                onSelect={onSelect}
                anchorEl={params.api.getCellElement(params.id, params.field)}
                open={true}
                onClose={() =>
                    params.api.stopCellEditMode({
                        id: params.id,
                        field: params.field,
                    })
                }
            />
        );
    };
    return {
        renderCellInput: renderCellInput,
        renderCellOutput: renderCellOutput,
        inputCellEditor: inputCellEditor
    }
}

const paginationModel = { page: 0, pageSize: 5 };



export const MappingPointTable = ({ points, setPoints }: Props) => {
    const theme = useTheme();
    const { renderCellInput, renderCellOutput, inputCellEditor } = cellSystem(theme);
    const columns: GridColDef[] = [
        { field: 'x', headerName: 'input', width: 200, editable: true, renderCell: renderCellInput, renderEditCell: inputCellEditor, },
        { field: 'y', headerName: 'output', width: 200, editable: true, renderCell: renderCellOutput, renderEditCell: inputCellEditor },
    ];

    const rows = points.map(p => ({ ...p, id: "row_" + p.x, inputDisplay: getDisplayNameInput(p), outputDisplay: getDisplayNameOutput(p) }));
    const [search, setSearch] = useState("");

    const updatePoints = (updateValue: MappingPoint) => {
        const updated = points.map(point => point.x == updateValue.x ? { ...updateValue, id: "row_" + updateValue.x } : { ...point });
        setPoints(updated);
    }

    console.log("update table with rows:", rows);
    const filteredRows = rows.filter((row) =>
        Object.values(row).some((value) =>
            String(value).toLowerCase().includes(search.toLowerCase())
        )
    );
    const usePaginationAndSearch = points.length > 5;

    return (<div>
        {usePaginationAndSearch && <TextField
            label="Search"
            size="small"
            value={search}
            onChange={(e) => setSearch(e.target.value)}
        />}
        <DataGrid
            rows={filteredRows}
            getRowId={(row) => row.id}
            columns={columns}
            slots={{ toolbar: GridToolbar }}
            initialState={
                usePaginationAndSearch
                    ? {
                        pagination: { paginationModel },
                        sorting: {
                            sortModel: [{ field: 'x', sort: 'asc' }, { field: 'y', sort: 'asc' }],
                        },
                    }
                    : {
                        sorting: {
                            sortModel: [{ field: 'x', sort: 'asc' }, { field: 'y', sort: 'asc' }],
                        },
                    }}
            pageSizeOptions={[5, 10]}
            checkboxSelection
            sx={{ border: 0 }}
            editMode="cell"
            processRowUpdate={(newRow: GridRowModel) => {
                updatePoints(newRow as MappingPoint);
                return newRow;
            }}
            onProcessRowUpdateError={(err) => {
                console.error(err);
            }}
        />
        <ButtonGroup variant="contained" aria-label="Basic button group">
            <Button>Clear</Button>
            <Button>Add</Button>
            <Button>Three</Button>
        </ButtonGroup>
    </div>)
}
