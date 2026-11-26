# Website content follow-up

## IMPORTANT: default macros require updating and testing

The default macros may be outdated. Review every macro in `defaultActions/`, update it where needed, and test it with the current plugin and WorldPainter before presenting the collection as verified examples.

- Check enabled/disabled actions, filters, nested dependencies, bundled layers, and expected results.
- Test the first recommended example, **Apply: Alpine mountains**, on existing slopes. Its current configuration enables **paint slopes**; the other included stages are disabled.
- Record the tested plugin/WorldPainter versions and prerequisites before removing the public review note.
- Do not treat website build, browser, or accessibility checks as tests of the plugin or the macro files themselves.

## Path Tool documentation — PR #126

The road, river, and paint-along-path guides now describe the merged Path Tool update. References were checked against the PR's README, help catalog, PathTool, PathOperations, and backend code. All eight `imgs/pathtool/` screenshots appear in the guide sections they explain; the selected Roads and Rivers GIF showcases remain unchanged. No river-junction tutorial is included. The retired gallery is no longer part of the site.

Website checks verify rendering, navigation, image availability, accessibility, and documented content—not interactive WorldPainter behavior. The new walkthroughs still need a hands-on pass in the plugin; do not describe them as runtime-tested based on website tests alone. If controls change, update the guide, defaults, references, and screenshots together.
