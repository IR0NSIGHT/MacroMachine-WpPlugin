# MacroMachine public website

Static public website and documentation for MacroMachine, separate from the plugin's `webUI`. Builds seven core pages: Home, Get started, Documentation, and four tool guides. Two longer macro references—Gradient Editor and Filters & Layers—remain separate documentation pages.

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

## GitHub Pages

The [public website workflow](../.github/workflows/website-pages.yml) replaces the former Storybook deployment at <https://ir0nsight.github.io/MacroMachine-WpPlugin/>.

Pushes to `public-website` and pull requests targeting `master` run the build, structural tests, and desktop/mobile browser checks. After merging into `master`, the workflow publishes `website/dist` through the existing `github-pages` environment. That environment permits deployments from `master` only; feature branches never replace the live site. Run the workflow manually from `master` to redeploy without another commit.

### Installer report

The download areas link to the version-specific VirusTotal report configured in `installerReport` in [`src/content.mjs`](src/content.mjs). Update the version, filename, SHA-256, and report URL together when replacing the reported installer; never reuse an older report for another build.

Optionally configure the repository Actions secret `VIRUSTOTAL_API_KEY` to fetch existing file-report statistics during trusted builds. The key is sent only by the build process, never embedded in the static site or exposed to pull-request builds. No file is uploaded. Results show their analysis date; missing credentials, invalid data, API errors, and rate limits fall back to the direct report link. Tests mock the API and do not consume the quota.

## Verify

```sh
npm ci
npm run verify
npx playwright install chromium
npm run test:browser
```

Structural tests check navigation, documentation hierarchy, links, fragments, metadata, image copying, and removal of the retired gallery. Browser tests check desktop/mobile layouts, image loading, navigation, keyboard controls, image enlargement, and automated WCAG A/AA rules.

## Edit

- Update tool copy and screenshots in [`src/content.mjs`](src/content.mjs).
- Add section anchors, links, reference tables, and planned-tutorial markers through the existing `Section` content model. Each tool page generates a contents index.
- Use `guideIntro` for documentation introductions. Set `parent: 'macros'` for macro references; only the four top-level guides appear in the landing catalog. Preserve guide URLs and section anchors when reorganizing documentation.
- Keep primary navigation to Get started, Documentation, GitHub, Discord, and the Download action. Link support directly to Discord and release history to GitHub Releases; do not add parallel gallery, support, or marketing pages.
- Requirements are read from the root `pom.xml` at build time; do not hardcode a separate WorldPainter or Java version in the guide.
- Keep the important [default macro review and testing note](CONTENT-NOTES.md) until the examples have been updated and tested with the plugin.
- The optional CityLayer sample is hosted in [`public/downloads/`](public/downloads/); keep its Ektelion credit and original-project link in the CityLayer guide. It is not bundled with the plugin installer.
- Update shared page templates in [`src/templates.mjs`](src/templates.mjs).
- Edit styles and browser interactions in [`public/assets/`](public/assets/).
- Keep website colors in [`public/assets/palette.css`](public/assets/palette.css). Comments trace the palette to the Swing dialog's selection colors and macro/action/input/output icons. The main stylesheet uses these tokens rather than separate color literals.
- Use the shared 16px body, 15px compact-link, and 14px caption/label sizes. Large headings retain their responsive scale.
- Add images to the repository's [`imgs/`](../imgs/) directory. The build copies every web-ready image; select screenshots explicitly in the guide sections they explain. The source `.xcf` is not a browser image and is excluded from the build. Incremental builds remove the retired generated `gallery.html` so it cannot be republished accidentally.

The download buttons are deliberately disabled placeholders. Replace them in the home and getting-started templates with real release links when ready; update the placeholder tests at the same time.
