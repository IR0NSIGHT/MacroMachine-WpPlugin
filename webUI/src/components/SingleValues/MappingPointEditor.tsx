import { MappingPoint } from "@/types/MappingPoint";
import { Dialog, DialogTitle, DialogContent, DialogActions, Button } from "@mui/material"
import { useState } from "react";
import { InputValueEditor } from "./InputValueEditor";
import { ActionType } from "@/types/MMAction";

export const MappingPointEditor = (props: { editorActive: boolean, isNew: boolean, onClose: () => void, oldPoint: MappingPoint, type: ActionType, updatePoint: (oldPoint: MappingPoint, newPoint: MappingPoint) => void, addPoint: (point: MappingPoint) => void }) => {
    const isNew = props.oldPoint === null;
    const [draft, setDraft] = useState<MappingPoint>(props.oldPoint)
    const thenDoThis = <div> {props.type /* "increment" */} {props.oldPoint.output.displayName /* pine tree strength*/} by </div>;
    console.log(props.type, props.oldPoint.output.displayName, thenDoThis);
    return (
        <Dialog open={props.editorActive} onClose={props.onClose}>
            <DialogTitle>{isNew ? 'Add Point' : 'Edit Point'}</DialogTitle>
            <DialogContent sx={{ display: 'flex', gap: 2, mt: 1 }}>
                <div>
                    <div>
                        <div>for all points where {props.oldPoint.input.displayName /* annotations */} is </div>
                        <InputValueEditor label="input" value={draft.x} input={props.oldPoint.input} onChange={newP => setDraft({ ...draft, x: newP.numericValue })}></InputValueEditor>
                    </div>
                    <br />
                    <div>
                        {thenDoThis}
                        <InputValueEditor label="output" value={draft.y} input={props.oldPoint.output} onChange={newP => setDraft({ ...draft, y: newP.numericValue })}></InputValueEditor>
                    </div>
                </div>
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
