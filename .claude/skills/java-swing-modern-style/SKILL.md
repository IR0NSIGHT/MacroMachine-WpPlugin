---
name: java-swing-modern-style
description: Give a Java Swing dialog, panel or component the plugin's modern dark look (charcoal, flat, lavender accents) without touching WorldPainter's pinned look and feel. Use when styling or restyling any Swing UI in this plugin, adding controls to an already styled dialog, or when asked for "the style guide" / "modern style" / "dark look" on a component.
---

# Java Swing modern style

WorldPainter pins its look and feel; we cannot change it and must not change
global `UIManager` defaults (that restyles all of WorldPainter). Instead every
styled control gets **its own UI delegate and colours**, so the look stays
local to the component tree we style.

The implementation lives in:

- `src/main/java/org/ironsight/wpplugin/macromachine/Gui/OverlayStyle.java` –
  palette constants, `font()`, pill buttons (`button`, `primaryButton`),
  dark `spinner`, dark `popup` menus, surfaces.
- `src/main/java/org/ironsight/wpplugin/macromachine/Gui/DialogStyle.java` –
  dialog look built on it: `apply(Container)`, `group(title)` /
  `GroupBorder`, `checkBox`, `comboBox`, `spinner`, `outputPicker`,
  `scrollPane` / `scrollBar`, `FieldBorder`, `button` / `primaryButton`.
- Reference use: `Layers/PathBuilder/PathTool.java#openPathSettings` (the
  Apply as road / river / paint dialogs).

Reuse these; extend `DialogStyle` for new control types rather than styling
ad hoc.

## Style guide

- **Dark, neutral foundation:** window `#161616` (`DialogStyle.WINDOW`),
  panels `#222222` (`PANEL`), raised / hovered surfaces `#2B2B2B` (`RAISED`).
- **Restrained accents:** lavender `#CDC7FF` (`OverlayStyle.ACCENT`) for
  focus and selection; muted purple, green, cyan only to tell tools or
  operation types apart.
- **Readable text:** white primary (`TEXT`), gray `#B0B0B0` secondary
  (`TEXT_MUTED`). System sans-serif (`OverlayStyle.font`: Inter, else
  Segoe UI, else Dialog), ~14 px for controls (`DialogStyle.controlFont()`),
  larger semibold headings (`headingFont()`, bold 15).
- **Clear hierarchy:** emphasise the active tool and main working area;
  descriptions, metadata and secondary actions quieter (muted text, smaller).
- **Flat surfaces:** no gradients, glossy buttons, heavy bevels or decorative
  chrome.
- **Subtle borders:** thin, low-contrast outlines (`OUTLINE` = white at ~12 %,
  `OUTLINE_STRONG` on hover, `OverlayStyle.BORDER` for fields) – separate
  panels and controls without boxing in every element.
- **Moderate rounding:** ~5–8 px for controls and panels
  (`CONTROL_RADIUS` 6, `PANEL_RADIUS` 8); larger only for grouped toolbars.
  Buttons stay pills to match the map overlay.
- **Spacing:** 4/8 px rhythm, 12–16 px between related elements (`GAP` 12),
  20–24 px panel padding (`PADDING` 20).
- **Icon-led toolbars:** same-size icons (Lucide only, never WorldPainter
  icons), evenly spaced on one shared surface; tooltips on icon-only controls.
- **Distinct selected states:** fill the selected item with its accent and use
  a dark icon/text on it (`ON_ACCENT`); never a border alone.
- **Responsive feedback:** hover brightens surface and border slightly;
  keyboard focus gets a clear lavender outline. Transitions brief and optional.
- **Minimal shadows:** only where elevation helps (floating toolbars, previews,
  dialogs), not on every component.
- **Content-first:** large, undistorted previews, straightforward labels; a
  precise creative tool, not a promotional dashboard.

## Steps to style a dialog

1. **Groups:** build sections with `DialogStyle.group(title)` (BoxLayout Y,
   transparent, `GroupBorder`). `GroupBorder` extends `TitledBorder`, so the
   title stays readable/changeable (`((TitledBorder) g.getBorder()).setTitle`)
   and tests can find a control's group. It paints the `#222` rounded panel,
   hairline and heading *in `paintBorder`* (Swing paints the border before the
   children, so it works as the panel background) and includes the 12 px gap
   to the next group in its bottom inset.
2. **Build the content** (rows as `FlowLayout(LEFT)` panels with `JLabel` +
   control, checkboxes, etc.; a row that can get wider than the dialog –
   pickers with names of any length – uses `GridBagLayout` with the last
   field taking the rest of the width (`weightx 1`, `fill HORIZONTAL`, a small
   minimum width) instead: the dialog tracks the viewport width, so a
   `FlowLayout` row would wrap onto a second line that is cut off), then call `DialogStyle.apply(content)` once it
   is complete. It walks the tree:
   - `JPanel` → transparent, white foreground
   - `JLabel` → white, control font
   - `JCheckBox` → `checkBox` (basic UI + custom icon: dark rounded box /
     lavender fill with dark check, hover, focus ring)
   - `JComboBox` → `comboBox` (basic UI; rounded field painted in
     `ui.paint`, chevron arrow button, dark popup list with lavender-tinted
     selection, renderer wrapped so cells take the dark colours)
   - `JSpinner` → `spinner` (overlay spinner + rounded `FieldBorder`, focus
     outline)
   - `JButton` → `button` (pill; `setFocusPainted(true)` → lavender focus ring)
   - `JScrollPane` → `scrollPane` (no border, window background, thin rounded
     thumb, no arrow buttons)
   - `IoSelectLabel` (shared output picker) → `outputPicker` (restyled from
     outside, see "Shared components")
3. **After `apply`:** mark the main action with `DialogStyle.primaryButton`
   (lavender fill, dark bold text) – `apply` makes every button a plain one, so
   this must come after it.
4. **Window:** content pane opaque `WINDOW`; padding `EmptyBorder(PADDING, …)`
   on the scrolling settings panel; footer `FlowLayout(RIGHT, 8, 0)` with
   `EmptyBorder(4, PADDING - 8, PADDING - 4, PADDING - 8)` (FlowLayout adds
   its gap at the edges too). Scroll vertically only: a `Scrollable` panel
   that tracks the viewport width + `HORIZONTAL_SCROLLBAR_NEVER`, so the dialog
   sets the width and wide content adapts.
5. **Custom-painted previews:** paint with palette colours (e.g. lavender on a
   dark checkerboard `#161616` / `#1C1C1C`); secondary labels in
   `TEXT_MUTED`, ~11 px.
6. **Verify:** render the dialog to a PNG (paint the root pane into a
   `BufferedImage` after `Thread.sleep` so deferred layout has settled) and
   look at it; run both builds. Tests that assert pixel distances may need
   the larger padding.

## Gotchas

- **Never** `UIManager.put(...)` or switch the LAF – it changes all of
  WorldPainter. Per-component `setUI(new Basic…UI())` + colours only.
- `setUI` on a `JComboBox` replaces a `UIResource` renderer: wrap the
  renderer *after* `setUI` (`StyledRenderer` checks it isn't wrapped twice).
- Combo current value is painted in `paintComponent` (by the UI), the border
  after it: fill the rounded field in `ui.paint` *before* `super.paint` and
  make `paintCurrentValueBackground` a no-op – a filling border would cover
  the text. For components whose content are child components (the output
  picker) a filling border works (`FieldBorder(…, fill = true)`).
- Overlay spinner installs a focus listener that sets a square line border:
  add a later focus listener that puts the `FieldBorder` back (AWT calls
  listeners in the order added).
- Shared components that hardcode light colours (`IoSelectLabel`,
  `DisplayUnitRenderer`, `MappingPointCellRenderer`) are restyled from the
  outside: listeners added after theirs win (hover), a `"background"`
  property listener re-applies the dark look after they re-render
  (`invokeLater`), renderer wrappers recolour every returned cell. Do not edit
  the shared classes for the dark look – the action editor still uses them in
  WorldPainter's style.
- Never let a vertical scroll bar come and go around content whose height
  depends on its width: the bar changes the width, the height follows, the
  bar flips again – the dialog flickers between two sizes. Use
  `VERTICAL_SCROLLBAR_ALWAYS` (the dialog scroll bar paints no thumb when
  there is nothing to scroll) and take `SCROLLBAR_WIDTH` off the right padding.
- Components whose height depends on their width (e.g. `CrossSectionView`)
  must not `revalidate()` synchronously inside `setBounds` (lost during
  layout) – `invokeLater(this::revalidate)`; the dialog validates twice before
  fitting its size.
- Tooltips: the tooltip *window* is WorldPainter's (dark grey), its default
  text black – unreadable. `apply` turns every tooltip into HTML carrying its
  own colours (`darkTooltip`: white on `#2B2B2B`, padded), and a
  `"ToolTipText"` property listener converts tooltips set later (dynamic
  status tooltips). Tests should check tooltips with `contains`, not equality.
- Not covered yet (stay in WorldPainter's look): dialogs opened by shared
  pickers.
