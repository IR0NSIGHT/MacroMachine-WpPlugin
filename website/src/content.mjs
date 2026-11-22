/** @typedef {{src: string, alt: string, caption: string}} Media */
/** @typedef {{title: string, body: string, media?: Media, bullets?: string[]}} Section */
/** @typedef {{slug: string, name: string, title: string, description: string, accent: string, number: string, tag: string, hero: Media, benefits: string[], sections: Section[], notice?: string, icon?: string}} Feature */

export const repository = 'https://github.com/IR0NSIGHT/MacroMachine-WpPlugin';
export const playlist = 'https://youtube.com/playlist?list=PLyNqTiCLQP-gTNoBJlvTK3xeIs6RQjUkl';
export const projectFacts = {
  license: 'CC0 1.0',
  downloads: '~500',
  releases: '20+',
  developmentSince: '2024',
  checkedOn: '22 November 2026',
};
export const mountain = 'tutorial/WorldPainter - test_mountain_range - Surface 30.05.2025 10_57_37.png';

/** @returns {Media} */
export const media = (src, alt, caption = alt) => ({ src, alt, caption });

/** @type {Feature[]} */
export const features = [
  {
    slug: 'macros', name: 'Macros', number: '01', accent: 'macro', tag: 'REPEATABLE TERRAIN EDITS',
    icon: 'macros_icon.svg',
    title: 'Run terrain edits\nin sequence.',
    description: 'Combine actions into a macro and run them on your map. Use ready-made macros for snowy peaks, beaches, mountains, mesas, and swamps, or create your own.',
    hero: media(mountain, 'A WorldPainter mountain range before and after running a terrain macro'),
    benefits: ['Reusable workflows', 'Whole-map execution', 'Visual action designer'],
    sections: [
      { title: 'Run a saved macro.', body: 'A macro is a list of actions, executed from top to bottom. Start with ready-made macros, adapt one to your map, or create your own from scratch.', media: media('tutorial/apply_macro.PNG', 'MacroMachine macro tree with a macro selected'), bullets: ['Open the M tool in WorldPainter.', 'Import the ready-made macros or choose an existing macro.', 'Select a macro and press ▶ to run it on the whole map.'] },
      { title: 'Arrange actions in the designer.', body: 'Use the macro designer to arrange actions. Each action connects an input, an operation, and an output. Save your changes before running the macro.', media: media('tutorial/macro_designer.PNG', 'The visual macro designer and ordered action list') },
      { title: 'Add actions or call another macro.', body: 'Add a new action to your macro, then select it in the tree to open the action designer. You can also call macros from other macros; nested macros inherit the current filter state.', media: media('tutorial/tree_view_new_action.PNG', 'A new action nested underneath its parent macro') },
      { title: 'Example: select blocks by biome.', body: 'Use biome as the input, set as the operation, and selection block as the output. Map swamp and desert to ON to select those biomes.', media: media('tutorial/selection_block_table.PNG', 'Biome values mapped to selection block ON or OFF') },
      { title: 'Check the selection on the map.', body: 'Run the action to select swamp and desert blocks. Blocks in other biomes are deselected.', media: media('tutorial/macro_before_after.png', 'Before and after applying a biome-based selection macro') },
      { title: 'Check the log and debug actions.', body: 'The log tab reports action statistics. If a macro seems to do nothing, check whether its inputs match conditions on your map and use the debugger to step through the actions.', media: media('tutorial/macro_statistic.PNG', 'Action statistics in the MacroMachine log tab') },
    ],
  },
  {
    slug: 'citylayer', name: 'CityLayer', number: '02', accent: 'output', tag: 'INDIVIDUAL SCHEMATIC PLACEMENT',
    icon: 'citylayer_icon.svg',
    title: 'Place and edit\nindividual buildings.',
    description: 'Place schematics at exact positions in WorldPainter. Select buildings individually or in groups, then move, rotate, mirror, or copy them.',
    hero: media('tutorial/CityLayer_Showcase_Use.PNG', 'WorldPainter CityLayer with individually positioned schematic buildings'),
    benefits: ['Precise schematic placement', 'Group transformations', 'Copy & paste'],
    notice: 'WorldPainter undo/redo does not restore CityLayer building placements. Avoid undo/redo while editing CityLayer, and save your world before making large changes.',
    sections: [
      { title: 'Choose positions instead of random placement.', body: 'CityLayer works like a Custom Object Layer, but you decide exactly where each schematic goes instead of painting random placements. Center each schematic’s offset in its options for predictable placement.', media: media('tutorial/CityLayer_Showcase_Create.PNG', 'CityLayer creation and schematic configuration') },
      { title: 'Create a layer and place a building.', body: 'Create a custom CityLayer, add your schematics, then select the layer and the house-icon City Tool. Choose a schematic in the tool options and place it with Ctrl + left click.', media: media('tutorial/CityLayer/citylayer_createLayer.gif', 'Animated walkthrough of creating a CityLayer'), bullets: ['Add your schematic collection to the layer.', 'Select the layer and the house-icon City Tool.', 'Keep 3D Preview open with Auto Update to see your changes.'] },
      { title: 'Select one or more buildings.', body: 'Click a building to toggle its selection, or drag a rectangle to select buildings fully inside it. Ctrl + A selects all buildings; Escape clears the selection.', media: media('tutorial/CityLayer/citylayer_setup.gif', 'Animated building placement and rectangle selection') },
      { title: 'Change the selected schematic.', body: 'Hold Shift and scroll the mouse wheel to change the building type for the whole selection. With no selection, the same shortcut changes the schematic on your brush.', media: media('tutorial/CityLayer/citylayer_selectBuildingType.gif', 'Cycling selected buildings through different schematic types') },
      { title: 'Move, rotate, and mirror buildings.', body: 'Right click to move the selection center to the cursor. Use W, A, S, D for one-block adjustments, C to rotate around the selection center, X to mirror, and Delete to remove buildings.', media: media('tutorial/CityLayer/citylayer_transform.gif', 'Moving, rotating, and mirroring a selection of buildings') },
      { title: 'Copy and paste groups.', body: 'Copy with Ctrl + C, cut with Ctrl + X, and paste with Ctrl + V. Relative offsets are preserved. To paste, keep the cursor over the map and stay in the same CityLayer.', media: media('tutorial/CityLayer/citylayer_copypaste.gif', 'Copying and pasting groups of buildings while preserving their arrangement') },
      { title: 'Randomize type, rotation, or mirroring.', body: 'Enable random rotation, selection, or mirroring to vary buildings after each placement. Press Q to apply the enabled random options to your selection. The highlight-colors option shows the layer color instead of the schematic.', media: media('tutorial/CityLayer/citylayer_randomize.gif', 'Randomizing building types and orientations') },
    ],
  },
  {
    slug: 'paths', name: 'Path Tool', number: '03', accent: 'action', tag: 'ROADS, RIVERS, AND SLOPES',
    icon: 'paths_icon.svg',
    title: 'Draw roads\nand rivers.',
    description: 'Click to place connected path nodes. Adjust width, curvature, and slope, with smooth transitions into the surrounding terrain.',
    hero: media('tutorial/PathTool_Showcase.PNG', 'A winding road and the Path Tool settings in WorldPainter'),
    benefits: ['Smooth terrain transitions', 'Slope limits', 'Roads & rivers'],
    sections: [
      { title: 'Place connected path nodes.', body: 'Choose the road-icon Path Tool. Left click to place a node; your brush radius determines its size. Place another node to connect the path. Undo removes the last node, and right click starts a new path.', bullets: ['Choose a brush radius for the path width.', 'Click to place connected nodes.', 'Right click to reset and begin a new path.'] },
      { title: 'Follow the terrain and apply paint.', body: 'Enable snap to terrain to follow the terrain height as closely as possible without creating tall walls across depressions. Use paint applies your selected terrain or layer along the path.' },
      { title: 'Keep rivers downhill.', body: 'Only downhill ensures the path never rises in height—use it for rivers. Curve strength controls the shape: 0 creates straight segments with hard corners, while 1 produces a smooth Bézier curve.' },
      { title: 'Set a maximum slope.', body: 'Limit slope sets the maximum rise or fall per 16 blocks of path. Try 4/16 for roads; 8/16 is comparable to slabs, and 16/16 to stairs. A curve strength around 0.4 is a useful starting point for roads, and 1 for rivers.' },
    ],
  },
  {
    slug: '3d-preview', name: '3D Preview', number: '04', accent: 'output', tag: 'LIVE 3D EXPORT VIEW',
    icon: '3d_preview_icon.svg',
    title: 'Edit in WorldPainter.\nSee changes in 3D.',
    description: 'See the exported result in a Minecraft-style 3D window beside WorldPainter. With Auto Update enabled, the view follows your edits without manually exporting and reloading Minecraft each time.',
    hero: media('tutorial/preview_operation_showcase.PNG', 'A selected WorldPainter area alongside its rendered Minecraft export preview'),
    benefits: ['Auto-updating 3D window', 'Export-based rendering', 'No Minecraft reloads'],
    sections: [
      { title: 'See an edit in about a second, not a minute.', body: 'Normally, checking a change in Minecraft means manually exporting, reloading the world, and finding the result in-game. Auto Update replaces that repeated cycle: local edits can appear in roughly a second, instead of around a minute spent exporting and reloading. Times vary with selection size, map contents, and hardware.' },
      { title: 'Keep the 3D view beside WorldPainter.', body: 'Leave the renderer window open alongside the map. Adjust terrain, draw a path, or move a CityLayer building in WorldPainter, then check the updated result in 3D. You can keep editing without opening Minecraft after every change.' },
      { title: 'View the export result, not just the heightmap.', body: 'The view is built from background test exports, so you see the resulting terrain, trees, and placed buildings. It uses MacroMachine’s custom Minecraft-style renderer, not a running Minecraft client.' },
      { title: 'Enable the live view.', body: 'Open MacroMachine with the M tool, choose 3D Preview, and select the area you want to work on. Enable Auto Update to keep the 3D window in sync with your edits.', bullets: ['Open MacroMachine from the M tool.', 'Choose 3D Preview and select an area.', 'Enable Auto Update and keep the renderer beside WorldPainter.'] },
      { title: 'Use smaller areas for faster updates.', body: 'The preview exports the selected area in the background. Large selections can cause lag with Auto Update enabled; keep the selection focused on the area you are editing. Underground terrain is hidden in the rendered view.' },
    ],
  },
  {
    slug: 'gradient-editor', name: 'Gradient Editor', number: '05', accent: 'input', tag: 'CURVE-BASED VALUE MAPPING',
    title: 'Set layer strength\nwith a curve.',
    description: 'Map slope and other inputs to layer strength using control points. Values between points are interpolated automatically.',
    hero: media('tutorial/action_pines_result.png', 'A mountain with pine coverage controlled by slope using the gradient editor'),
    benefits: ['Editable control points', 'Automatic interpolation', 'Discrete value ranges'],
    sections: [
      { title: 'Example: pine density by slope.', body: 'Create an action with slope as the input, set as the operation, and pines as the output. The horizontal axis shows the input; the vertical axis shows the output strength.', media: media('tutorial/action_pines_start.PNG', 'Configuring a slope-to-pines action before editing its gradient') },
      { title: 'Add and adjust control points.', body: 'Left click the curve to add a point, drag to adjust it, and right click to remove it. Values between points are interpolated automatically. In the table, black values are editable control points; grey values are interpolated.', media: media('tutorial/action_pines_end.PNG', 'A gradient curve reducing pine density as the terrain gets steeper') },
      { title: 'Reduce pine density on steep slopes.', body: 'For example, set pine strength to 10 at 0° slope, taper to 7 at 60°, and drop to 0 at 70°. Run the action to see the transition on your mountain.' },
      { title: 'Map ranges to fixed values.', body: 'When an output uses fixed categories, such as annotation colors, the gradient editor lets you define input ranges instead.', media: media('tutorial/action_slope_painter.PNG', 'Mapping slope ranges to annotation colors') },
      { title: 'Color the map by slope.', body: 'Apply the slope-to-annotation action to highlight different gradients on your map. Use the result to inspect the terrain and guide your next edit.', media: media('tutorial/result_slope_painter.PNG', 'A WorldPainter map annotated with colors indicating terrain slope') },
    ],
  },
  {
    slug: 'filters', name: 'Filters & Layers', number: '06', accent: 'selection', tag: 'LIMIT WHERE ACTIONS APPLY',
    title: 'Filter which blocks\na macro changes.',
    description: 'Exclude blocks from later actions with a filter. Use custom layers as inputs and outputs, or apply layers with a per-block probability using spraypaint.',
    hero: media('tutorial/selection_block_table.PNG', 'Selecting biome values in the action designer'),
    benefits: ['Targeted actions', 'Custom layer support', 'Spraypaint output'],
    sections: [
      { title: 'Exclude blocks from later actions.', body: 'Create an action with the property you want to filter as the input, set as the operation, and Action Filter as the output. Set unwanted values to block and everything else to ignore. Later actions skip the filtered-out blocks.' },
      { title: 'Use Always within the current filter.', body: 'Use the Always input to apply an output throughout the eligible area. It still respects previous action filters. Nested macros inherit the current filter state and can also change it.' },
      { title: 'Use custom layers as inputs and outputs.', body: 'MacroMachine offers custom layers from your current .world project and layers already used in macros as inputs or outputs. If a layer is missing, close MacroMachine, save your world, and open MacroMachine again.' },
      { title: 'Apply layers with a per-block probability.', body: 'Spraypaint applies a layer with a probability for each block. More restrictive action filters can also improve performance by reducing the number of tiles a macro needs to touch.' },
    ],
  },
];
