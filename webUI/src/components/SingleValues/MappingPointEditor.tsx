import { MappingPoint } from "@/types/MappingPoint";
import { Dialog, DialogTitle, DialogContent, DialogActions, Button, Box } from "@mui/material";
import { useState } from "react";
import { InputValueEditor } from "./InputValueEditor";
import { ActionType } from "@/types/MMAction";

export const MappingPointEditor = (props: {
    editorActive: boolean,
    isNew: boolean,
    onClose: () => void,
    oldPoint: MappingPoint | null,
    type: ActionType,
    updatePoint: (oldPoint: MappingPoint, newPoint: MappingPoint) => void,
    addPoint: (point: MappingPoint) => void
}) => {
    const isNew = props.oldPoint === null;
    const [draft, setDraft] = useState<MappingPoint>(
        props.oldPoint ?? { x: 0, y: 0, input: null!, output: null! } // placeholder for new points
    );
    let typeConjugated: string;

    switch (props.type) {
        case "increment":
            typeConjugated = "incremented by";
            break;
        case "subtract":
            typeConjugated = "decreased by";
            break;
        case "multiply":
            typeConjugated = "multiplied by";
            break;
        case "divide":
            typeConjugated = "divided by";
            break;
        case "set":
            typeConjugated = "set to";
            break;
        case "limit":
            typeConjugated = "limited to";
            break;
        case "set minimum":
            typeConjugated = "set to minimum of";
            break;
        default:
            typeConjugated = props.type;
    }
    return (
        <Dialog open={props.editorActive} onClose={props.onClose} maxWidth="sm" fullWidth>
            <DialogTitle>{isNew ? 'Add Point' : 'Edit Point'}</DialogTitle>
            <DialogContent
                sx={{
                    display: 'flex',
                    flexWrap: 'wrap',
                    gap: 2,
                    mt: 1
                }}
            >
                {/* Input editor */}
                <Box sx={{ flex: '1 1 200px', minWidth: 150, display: 'flex', flexDirection: 'column', gap: 2 }}>
                    <InputValueEditor
                        includeIgnore={false}
                        label="Input"
                        value={draft.x}
                        input={draft.input}
                        onChange={newP => setDraft({ ...draft, x: newP.numericValue })}
                    />
                </Box>

                {/* Output editor */}
                <Box sx={{ flex: '1 1 200px', minWidth: 150, display: 'flex', flexDirection: 'column', gap: 2 }}>
                    <InputValueEditor
                        includeIgnore={true}
                        label="Output"
                        value={draft.y}
                        input={draft.output}
                        onChange={newP => setDraft({ ...draft, y: newP.numericValue })}
                    />
                </Box>
                <div>for all points where {draft.input.displayName}={draft.input.values.find(v => v.numericValue === draft.x)?.displayName ?? draft.x}, {draft.output.displayName} is  {typeConjugated} {draft.output.values.find(v => v.numericValue === draft.y)?.displayName ?? draft.y}</div>
            </DialogContent>

            <DialogActions>
                <Button onClick={props.onClose}>Cancel</Button>
                <Button
                    variant="contained"
                    onClick={() => {
                        if (isNew) props.addPoint(draft);
                        else props.updatePoint(props.oldPoint!, draft);
                    }}
                >
                    Confirm
                </Button>
            </DialogActions>
        </Dialog>
    );
};