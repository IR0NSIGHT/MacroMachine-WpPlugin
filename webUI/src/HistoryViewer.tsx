import {
  Accordion,
  AccordionDetails,
  AccordionSummary,
  Box,
  Chip,
  Divider,
  Paper,
  Stack,
  Typography,
} from "@mui/material";
import { useExecutionHistoryQuery } from "./API/queries";
import { ExecutionStateDTO } from "./types/DTO";
import { ExecutionStateDTOStatusEnum } from "./generated/client/models/ExecutionStateDTO";
import ExpandMoreIcon from "@mui/icons-material/ExpandMore";
import { PageLoadingSpinner } from "./PageLoadingSpinner";

export const HistoryTab = () => {
  const { data: history, isLoading, isError, error } = useExecutionHistoryQuery();
  return <HistoryViewer data={history} isLoading={isLoading} isError={isError} error={error} />;
};

type HistoryViewerProps = {
  data?: ExecutionStateDTO[];
  isLoading: boolean;
  isError: boolean;
  error: Error | null;
};

export const HistoryViewer = ({ data: history, isLoading, isError, error }: HistoryViewerProps) => {
  const getStatusColor = (status: ExecutionStateDTOStatusEnum) => {
    switch (status) {
      case "COMPLETED":
        return "success.main";
      case "FAILED":
        return "error.main";
      case "PAUSED":
      case "RUNNING":
      case "PREPARING":
      case "QUEUED":
      default:
        return "info.main";
    }
  };

  if (isLoading) {
    return <PageLoadingSpinner />;
  }

  return (
    <Box sx={{ p: 2, maxWidth: 1200 }}>
      <Typography variant="h5" gutterBottom>
        Execution History
      </Typography>

      {isError && <Typography color="error">Error: {String(error)}</Typography>}

      {!isLoading && !isError && history?.length === 0 && (
        <Typography>No execution history yet.</Typography>
      )}

      {!isLoading && !isError && (
        <Stack spacing={2} sx={{ maxHeight: "1024px", overflowY: "auto", pr: 1 }}>
          {history?.map((execution) => {
            const execColor = getStatusColor(execution.status);

            return (
              <Accordion
                key={execution.executionId}
                disableGutters
                sx={{
                  mb: 1,
                  borderLeft: 6,
                  borderColor: execColor,
                  "&:before": { display: "none" },
                }}
              >
                {/* HEADER (always visible) */}
                <AccordionSummary
                  expandIcon={<ExpandMoreIcon />}
                  sx={{
                    minHeight: 56,
                    "& .MuiAccordionSummary-content": {
                      alignItems: "center",
                      justifyContent: "space-between",
                      width: "100%",
                      pr: 2,
                    },
                  }}
                >
                  <Typography fontWeight={600}>Execution {execution.executionId}</Typography>

                  <Chip
                    label={execution.status}
                    size="small"
                    color={
                      execution.status === "COMPLETED"
                        ? "success"
                        : execution.status === "FAILED"
                          ? "error"
                          : "info"
                    }
                  />
                </AccordionSummary>

                {/* STEPS (collapsed by default) */}
                <AccordionDetails>
                  <Divider sx={{ mb: 1 }} />

                  <Stack spacing={1}>
                    {execution.steps.map((step, index) => {
                      const stepStatus = getStatusColor(step.status);

                      return (
                        <Paper
                          key={step.actionId}
                          sx={{
                            p: 1.5,
                            borderLeft: 5,
                            borderColor: stepStatus,
                          }}
                        >
                          <Stack direction="row" justifyContent="space-between" alignItems="center">
                            <Typography variant="body2">Step {index + 1}</Typography>

                            <Chip label={step.status} size="small" />
                          </Stack>

                          <Typography variant="caption" color="text.secondary">
                            Action ID: {step.actionId}
                          </Typography>

                          <Typography variant="caption" display="block">
                            Progress: {step.percentComplete}%
                          </Typography>

                          {step.error && (
                            <Box
                              sx={{
                                mt: 1,
                                p: 1,
                                borderRadius: 1,
                                bgcolor: "error.light",
                                color: "error.contrastText",
                              }}
                            >
                              <Typography variant="caption">{step.error}</Typography>
                            </Box>
                          )}
                        </Paper>
                      );
                    })}
                  </Stack>
                </AccordionDetails>
              </Accordion>
            );
          })}
        </Stack>
      )}
    </Box>
  );
};
