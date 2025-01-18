# WorldPainter Expand Layer Plugin

This plugin allows you to automatically select the edge of a layer and apply a gradient to it, growing outwards or inwards.

![grafik](https://github.com/user-attachments/assets/4359b455-85a7-4b47-93a2-837773a71523)

## Usage
1. The "Select Edge Operation" can be found in the tools section of worldpainter. Select the 2 blue circles.
2. Specify your wanted settings in the tool-settings section. You will probably need to adjust your layout to see all of the settings tab.
3. Press "run" in the settings tab to execute the plugin
4. The plugin will find all blocks that are the input layer, grow in the specified direction, apply the gradient and the paint the result as the output layer in the map

### Gradient Editor
If you press gradient: edit, a graphical editor for the applied gradient will appear.
It shows a preview of the output, where left is the beginning of your input layer and the gradient grows to the right.
The gradient is not to scale and will be rescaled by the operation using the selected "width" parameter.
![grafik](https://github.com/user-attachments/assets/1e1fa285-b97f-4ea9-a157-59474458b840)

## troubleshooting
Q: I dont see the "run" or "help" button
A: Your tool settings window is to small. Drag it wider

Q: If i click run, nothing on my map happens
A: You need to have blocks of your selected input layer painted on the map, otherwise nothing can and will happen.
