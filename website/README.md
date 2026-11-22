# MacroMachine public website

Static marketing site for MacroMachine, separate from the plugin's `webUI`. Builds a landing page, six feature pages, an image gallery, and an installation guide using the project's existing images.

## Build and preview

Requires Node.js 20+. Run from `website/`:

```sh
npm run dev
```

Open <http://127.0.0.1:4173>. Set `PORT` to change the preview port. Re-run the build after editing sources; the preview server has no live reload.

```sh
npm run build
```

Deploy the contents of `dist/` to any static host, including a subdirectory. All internal links and assets use relative URLs. The build and preview have no npm dependencies and require no plugin backend.

## Verify

```sh
npm ci
npm run verify
npx playwright install chromium
npm run test:browser
```

Structural tests check links, fragments, metadata, and complete image coverage. Browser tests check desktop/mobile layouts, image loading, navigation, keyboard controls, gallery interactions, and automated WCAG A/AA rules.

## Edit

- Update tool copy and screenshots in [`src/content.mjs`](src/content.mjs).
- Update shared page templates in [`src/templates.mjs`](src/templates.mjs).
- Edit styles and browser interactions in [`public/assets/`](public/assets/).
- Keep website colors in [`public/assets/palette.css`](public/assets/palette.css). Comments trace the palette to the Swing dialog's selection colors and macro/action/input/output icons. The main stylesheet uses these tokens rather than separate color literals.
- Use the shared 16px body, 15px compact-link, and 14px caption/label sizes. Large headings retain their responsive scale.
- Add images to the repository's [`imgs/`](../imgs/) directory. The build copies every web-ready image. The gallery only shows raster screenshots and walkthrough GIFs from `imgs/tutorial/`; logos and SVG icons are excluded. The source `.xcf` is not a browser image and is excluded from the build.

The download buttons are deliberately disabled placeholders. Replace them in the home and getting-started templates with real release links when ready; update the placeholder tests at the same time.
