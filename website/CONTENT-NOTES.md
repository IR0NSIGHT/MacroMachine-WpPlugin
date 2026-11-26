# Website content follow-up

## IMPORTANT: default macros require updating and testing

The default macros may be outdated. Review every macro in `defaultActions/`, update it where needed, and test it with the current plugin and WorldPainter before presenting the collection as verified examples.

- Check enabled/disabled actions, filters, nested dependencies, bundled layers, and expected results.
- Test the first recommended example, **Apply: Alpine mountains**, on existing slopes. Its current configuration enables **paint slopes**; the other included stages are disabled.
- Record the tested plugin/WorldPainter versions and prerequisites before removing the public review note.
- Do not treat website build, browser, or accessibility checks as tests of the plugin or the macro files themselves.

## CityLayer documentation — PR #127

The CityLayer guide now describes the City Tool merged in PR #127: on-map Schematics / Layer / overlay / Add controls, the schematic card and picker, add mode, the shared transform box, the Randomize panels, the bundled tutorial layer, and undo / redo of building placements (with the warning and undo-history clear when schematics are removed). References were checked against the City Tool help catalog, the menu code, and the PR's commit notes.

The four `imgs/citytool/` screenshots come from the `CityToolScreenshot` development harness (plain map, no terrain) and from the removal warning dialog. The old `imgs/tutorial/CityLayer/` GIFs show the retired tool-settings panel; only the riverside showcase GIF (results) and the layer-creation GIF (layer editor) are still used. Still wanted, on real terrain: an overview of the City Tool on the map, add mode with the next-house preview, a group in the transform box, Add random with ticked picker tiles, and new GIFs for selecting, transforming, copy / paste, and randomizing. Do not describe the walkthroughs as runtime-tested based on website tests alone.

## Path Tool documentation — PR #126

The road, river, and paint-along-path guides now describe the merged Path Tool update. References were checked against the PR's README, help catalog, PathTool, PathOperations, and backend code. All eight `imgs/pathtool/` screenshots appear in the guide sections they explain; the selected Roads and Rivers GIF showcases remain unchanged. No river-junction tutorial is included. The retired gallery is no longer part of the site.

Website checks verify rendering, navigation, image availability, accessibility, and documented content—not interactive WorldPainter behavior. The new walkthroughs still need a hands-on pass in the plugin; do not describe them as runtime-tested based on website tests alone. If controls change, update the guide, defaults, references, and screenshots together.
