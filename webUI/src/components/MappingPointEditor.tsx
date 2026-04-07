import { MappingPoint } from "@/types/MappingPoint";
import { Dialog, DialogTitle, DialogContent, TextField, DialogActions, Button } from "@mui/material"
import { useState } from "react";
import { InputDropdownSelector } from "./InputSelector";

export const MappingPointEditor = (props: { editorActive: boolean, isNew:  boolean, onClose: () => void, oldPoint: MappingPoint, updatePoint: (oldPoint: MappingPoint, newPoint: MappingPoint) => void, addPoint: (point: MappingPoint) => void }) => {
    const isNew = props.oldPoint === null;
    const [draft, setDraft] = useState<MappingPoint>(props.oldPoint)
    return (
        <Dialog open={props.editorActive} onClose={props.onClose}>
            <DialogTitle>{isNew ? 'Add Point' : 'Edit Point'}</DialogTitle>
            <DialogContent sx={{ display: 'flex', gap: 2, mt: 1 }}>
                <InputDropdownSelector label="input" value={draft.x} input={ props.oldPoint.input } onChange={ newP => setDraft({ ...draft, x: newP.numericValue }) }></InputDropdownSelector>
                <InputDropdownSelector label="output" value={draft.y} input={ props.oldPoint.output } onChange={ newP => setDraft({ ...draft, y: newP.numericValue }) }></InputDropdownSelector>
            </DialogContent>
            <DialogActions>
                <Button onClick={props.onClose}>Cancel</Button>
                <Button
                    onClick={() => {
                        if (isNew) {
                            props.addPoint(draft)
                        } else {
                            props.updatePoint(props.oldPoint, draft)
                        }
                    }}
                >
                    Confirm
                </Button>
            </DialogActions>
        </Dialog>)
}
