import { Meta, StoryObj } from '@storybook/react-vite';
import { MappingPointEditor } from '@/components/SingleValues/MappingPointEditor';
import { MappingPoint } from '@/types/MappingPoint';
import { useState } from 'react';
import { annotationsIO, heightIO, perlinNoiseIO, terrainIO } from '@/mock/dummyIOs';

// Wrapper to manage state in story
const MappingPointEditorWrapper = (props: { oldPoint: MappingPoint }) => {
  const [editorActive, setEditorActive] = useState(true);
  const [point, setPoint] = useState<MappingPoint>(props.oldPoint);

  const updatePoint = (oldPoint: MappingPoint, newPoint: MappingPoint) => {
    setPoint(newPoint);
    console.log('Updated point', oldPoint, '->', newPoint);
  };

  const addPoint = (newPoint: MappingPoint) => {
    setPoint(newPoint);
    console.log('Added point', newPoint);
  };

  return (
    <MappingPointEditor
      editorActive={editorActive}
      isNew={point === null}
      oldPoint={point}
      onClose={() => setEditorActive(false)}
      updatePoint={updatePoint}
      addPoint={addPoint}
      type="increments"
    />
  );
};

export default {
  component: MappingPointEditor,
} as Meta;

type Story = StoryObj<typeof MappingPointEditor>;

// Default story: editing an existing point
export const Default: Story = {
  render: () => <MappingPointEditorWrapper oldPoint={{ x: 3, y: 50, input: annotationsIO, output: heightIO }} />
};

// Story for adding a new point
export const NewPoint: Story = {
  render: () => <MappingPointEditorWrapper oldPoint={{ x: 3, y: 50, input: perlinNoiseIO, output: terrainIO }} />
};