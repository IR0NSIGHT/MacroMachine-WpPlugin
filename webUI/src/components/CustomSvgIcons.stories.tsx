import { InputOutputDTOTypeEnum } from "@/generated/client";
import type { Meta } from "@storybook/react-vite";
import { GetIconForIoType } from "./CustomSvgIcons";
import { Divider } from "@mui/material";
const meta: Meta = {
  title: "Components/CustomSvgIcons",
};

export default meta;

export const DefaultList = {
  render: () => (
    <div>
      {Object.values(InputOutputDTOTypeEnum).map((ioType) => {
        return (
          <div>
            {ioType}
            <div
              style={{
                width: 64,
                height: 64,
                display: "flex",
                alignItems: "center",
                justifyContent: "center",
              }}
            >
              {GetIconForIoType(ioType)}
            </div>

            <Divider />
          </div>
        );
      })}
    </div>
  ),
};
