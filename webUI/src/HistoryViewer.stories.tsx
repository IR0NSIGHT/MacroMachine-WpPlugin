import { Meta, StoryObj } from "@storybook/react-vite";
import { HistoryViewer } from "./HistoryViewer";

const meta: Meta<typeof HistoryViewer> = {
  title: "Components/HistoryViewer",
  component: HistoryViewer,
};

export default meta;

export const None: StoryObj<typeof HistoryViewer> = {
  args: {
    data: [],
    isLoading: false,
    isError: false,
    error: null,
  },
};

export const Error: StoryObj<typeof HistoryViewer> = {
  args: {
    data: [],
    isLoading: false,
    isError: true,
    error: { name: "Connection Error", message: "Failed to fetch execution history" },
  },
};

export const Loading: StoryObj<typeof HistoryViewer> = {
  args: {
    data: [],
    isLoading: true,
    isError: false,
    error: null,
  },
};

export const Some: StoryObj<typeof HistoryViewer> = {
  args: {
    data: [
      {
        status: "FAILED",
        executionId: "123456789-0",
        steps: [
          {
            actionId: "123456789-1",
            breakpoint: false,
            percentComplete: 100,
            status: "COMPLETED",
          },
          {
            actionId: "123456789-2",
            breakpoint: false,
            percentComplete: 100,
            status: "COMPLETED",
          },
          {
            actionId: "123456789-3",
            breakpoint: false,
            percentComplete: 50,
            error: "Could not find layer with id 'layer-123'",
            status: "FAILED",
          },
          {
            actionId: "123456789-4",
            breakpoint: false,
            percentComplete: 0,
            status: "QUEUED",
          },
          {
            actionId: "123456789-5",
            breakpoint: false,
            percentComplete: 0,
            status: "QUEUED",
          },
        ],
        currentStepIndex: 2,
      },
      {
        status: "FAILED",
        executionId: "222222222-0",
        steps: [
          {
            actionId: "222222222-1",
            breakpoint: false,
            percentComplete: 0,
            status: "FAILED",
            error: "Unexpected error during preparation",
          },
          {
            actionId: "222222222-2",
            breakpoint: false,
            percentComplete: 0,
            status: "PREPARING",
          },
        ],
        currentStepIndex: 0,
      },
      {
        status: "COMPLETED",
        executionId: "333333333-0",
        steps: [
          {
            actionId: "333333333-1",
            breakpoint: false,
            percentComplete: 100,
            status: "COMPLETED",
          },
        ],
        currentStepIndex: 0,
      },
    ],
    isLoading: false,
    isError: false,
    error: null,
  },
};
