import { Button } from "@mui/material";
import DeleteIcon from '@mui/icons-material/Delete';

export const DeleteButton = (props: {
    onClick: () => void;
}) => {
    const { onClick } = props;

    return (
        <Button
            variant="contained"
            color="error"
            startIcon={<DeleteIcon />}
            onClick={onClick}
        >
            
        </Button>)
};