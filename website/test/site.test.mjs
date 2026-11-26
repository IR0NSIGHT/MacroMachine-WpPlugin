import { test } from 'node:test';
import assert from 'node:assert/strict';
import { readFile, access, mkdir, writeFile } from 'node:fs/promises';
import { join, resolve, sep } from 'node:path';
import { build, root, output, imageRoot, listImages, readCompatibility } from '../build.mjs';
import { features, projectFacts, sampleSchematic, reportTemplate, installerReport } from '../src/content.mjs';
import { escape, imageUrl, sectionAnchor } from '../src/templates.mjs';

// Simulate an incremental build over an older deployment artifact.
await mkdir(output, { recursive: true });
await writeFile(join(output, 'gallery.html'), '<p>Retired gallery</p>');
const { pages, images } = await build({ installerScan: null });
const html = new Map(await Promise.all(pages.map(async (name) => [name, await readFile(join(output, name), 'utf8')])));

test('builds seven core pages and two macro references, without the retired gallery', async () => {
  assert.equal(pages.length, 9);
  assert.equal(features.filter((feature) => !feature.parent).length, 4);
  assert.equal(features.filter((feature) => feature.parent).length, 2);
  for (const name of ['index.html', 'citylayer.html', 'paths.html', '3d-preview.html', 'macros.html', 'gradient-editor.html', 'filters.html', 'get-started.html', 'documentation.html']) assert.ok(html.has(name), name);
  assert.ok(!html.has('gallery.html'));
  await assert.rejects(access(join(output, 'gallery.html')), { code: 'ENOENT' });
  for (const page of html.values()) assert.doesNotMatch(page, /gallery\.html|In action|Image gallery/);
});

test('primary navigation has the agreed destinations and a distinct download action on every page', () => {
  for (const [name, page] of html) {
    const nav = page.match(/<nav class="main-nav"[\s\S]*?<\/nav>/)[0];
    assert.deepEqual([...nav.matchAll(/<a[^>]*>([^<]+)<\/a>/g)].map((match) => match[1]), ['Get started', 'Documentation', 'GitHub', 'Discord', 'Download'], name);
    assert.match(nav, /class="nav-download" href="get-started\.html#download"/);
    assert.match(nav, /href="https:\/\/github\.com\/IR0NSIGHT\/MacroMachine-WpPlugin"/);
    assert.match(nav, /href="https:\/\/discord\.gg\/9kRJEMfGeE"/);
    assert.match(page, /href="index\.html" aria-label="MacroMachine home"/);
  }
});

test('tool pages are documentation and macro references are nested beneath Macros', () => {
  const docs = html.get('documentation.html');
  assert.match(docs, /aria-label="Macros references"/);
  const macro = html.get('macros.html');
  for (const feature of features) {
    const page = html.get(`${feature.slug}.html`);
    assert.match(page, /aria-label="Breadcrumb"/);
    assert.ok(page.includes(escape(feature.guideIntro)));
    assert.doesNotMatch(page, /class="feature-hero|class="benefits"|OTHER TOOLS|Also in MacroMachine|class="footer-resources"/);
    assert.ok(docs.includes(`href="${feature.slug}.html"`));
    if (feature.parent) {
      assert.equal(feature.parent, 'macros');
      assert.ok(page.includes(`<a href="macros.html">Macros</a>`));
      assert.ok(macro.includes(`href="${feature.slug}.html"`));
      assert.ok(!html.get('index.html').includes(`href="${feature.slug}.html"`));
    }
  }
});

test('documentation and every guide provide direct tool navigation before the contents', () => {
  for (const name of ['documentation.html', ...features.map((feature) => `${feature.slug}.html`)]) {
    const page = html.get(name);
    const navigation = page.match(/<nav class="doc-tool-nav[\s\S]*?<\/nav>/)?.[0];
    assert.ok(navigation, name);
    const toolLinks = [...navigation.matchAll(/class="doc-tool-name" href="([^"]+)"/g)].map((match) => match[1]);
    assert.deepEqual(toolLinks, features.filter((feature) => !feature.parent).map((feature) => `${feature.slug}.html`));
    assert.ok(page.indexOf('id="reference"') < page.indexOf(name === 'documentation.html' ? 'id="tasks"' : 'id="workflow"'));
    const current = features.find((feature) => `${feature.slug}.html` === name);
    if (current) {
      assert.ok(navigation.includes(`href="${current.slug}.html" aria-current="page"`));
      assert.equal([...navigation.matchAll(/aria-current="page"/g)].length, 1);
    } else {
      assert.doesNotMatch(navigation, /aria-current/);
    }
  }
  assert.doesNotMatch(html.get('index.html'), /doc-tool-nav/);
});

test('getting started derives requirements from the plugin build and starts with Auto Update', async () => {
  const compatibility = readCompatibility(await readFile(join(root, '../pom.xml'), 'utf8'));
  const start = html.get('get-started.html');
  assert.ok(start.includes(`WorldPainter ${compatibility.worldPainterVersion}`));
  assert.ok(start.includes(`Java ${compatibility.javaVersion} or newer`));
  assert.match(start, /installer does not install Java or switch WorldPainter’s runtime/);
  assert.match(start, /World Machine is not required/);
  assert.match(start, /installer contains the plugin only: no macros, layers, or schematics/);
  assert.match(start, /Linux Mint 22, including 3D Preview/);
  assert.match(start, /Windows release JAR contains OS-dependent native libraries/);
  assert.match(start, /macOS has not been tested/);
  assert.ok(start.indexOf('id="first-preview"') < start.indexOf('id="first-macro"'));
  assert.match(start, /enable Auto Update/i);
  assert.match(start, /click the M tool once to initialize/i);
  assert.throws(() => readCompatibility('<project/>'), /Website requirements/);
  assert.deepEqual(readCompatibility('<worldpainter.version> 2.26.3 </worldpainter.version><maven.compiler.release> 21 </maven.compiler.release>'), { worldPainterVersion: '2.26.3', javaVersion: '21' });
});

test('documentation is discoverable and every feature section has a direct contents link', () => {
  for (const content of html.values()) assert.match(content, /href="documentation\.html"/);
  const docs = html.get('documentation.html');
  for (const id of ['tasks', 'reference', 'backups', 'update', 'remove', 'help']) assert.ok(docs.includes(`id="${id}"`));
  for (const feature of features) {
    const page = html.get(`${feature.slug}.html`);
    assert.match(page, /aria-label="On this page"/);
    for (const section of feature.sections) {
      const anchor = sectionAnchor(section);
      assert.ok(page.includes(`id="${anchor}"`));
      assert.ok(page.includes(`href="#${anchor}"`));
      if (section.table) {
        assert.ok(page.includes(`aria-label="${escape(section.title)}"`));
        for (const row of section.table.rows) assert.equal(row.length, section.table.headings.length);
      }
    }
  }
});

test('the directly hosted sample schematic retains its bytes and creator attribution', async () => {
  assert.deepEqual(await readFile(join(output, sampleSchematic.href)), await readFile(join(root, 'public', sampleSchematic.href)));
  const city = html.get('citylayer.html');
  assert.ok(city.includes(`href="${sampleSchematic.href}" download`));
  assert.ok(city.includes(`href="${sampleSchematic.source}"`));
  assert.match(city, /demonstration schematic by Ektelion/);
  assert.match(city, /horizontal offset/);
  assert.match(city, /no automatic connector snapping/);
  assert.match(city, /90°/);
});

test('macro teaching and the repository note distinguish examples from verified presets', async () => {
  const macro = html.get('macros.html');
  const note = await readFile(join(root, 'CONTENT-NOTES.md'), 'utf8');
  assert.match(note, /IMPORTANT: default macros require updating and testing/);
  assert.match(note, /may be outdated/);
  assert.equal(features.find((feature) => feature.slug === 'macros').sections.find((section) => section.id === 'default-macros').table.rows.length, 12);
  assert.match(macro, /collection may be outdated and needs updating and testing/);
  assert.match(macro, /stone on steep slopes, grass on flatter land, and sand below water/);
  assert.match(macro, /Map Land \(0\) to skip and positive depths to Sand/);
  assert.match(macro, /selection do not automatically restrict every macro/);
  assert.match(macro, /stopping does not roll back earlier actions/);
  assert.match(macro, /diagnostic filter colors can change the Annotations layer/);
  assert.match(macro, /plugins\/macroMachine\/savefile\.macro/);
});

test('merged Path Tool has complete road, river, and paint guides instead of placeholders', () => {
  const paths = features.find((feature) => feature.slug === 'paths');
  assert.equal(paths.sections.filter((section) => section.planned).length, 0);
  for (const id of ['road-tutorial', 'river-tutorial', 'paint-along-path']) {
    assert.ok(paths.sections.find((section) => section.id === id).bullets.length >= 5);
    assert.ok(html.get('documentation.html').includes(`href="paths.html#${id}"`));
  }
  assert.equal(paths.name, 'Path Tool');
  assert.doesNotMatch(html.get('paths.html'), /Undo removes the last node|Tutorial:.*junction|Tutorial placeholder|planned-tutorial|incoming tool|brush radius to control/i);
  assert.doesNotMatch(html.get('documentation.html'), /tutorial — planned|walkthroughs are being updated/i);
});

test('Path Tool references cover the new editors, controls, operations, settings, and persistence', () => {
  const page = html.get('paths.html');
  for (const id of ['start-a-path', 'select-and-move', 'editors', 'numeric-entry', 'transform', 'height-profile', 'node-operations', 'operations-reference', 'downhill', 'road-settings', 'river-settings', 'blending', 'overlays', 'save-and-undo', 'controls']) {
    assert.ok(page.includes(`id="${id}"`), id);
  }
  for (const text of ['six nodes', '8-block radius, independent of the WorldPainter brush', 'highest-numbered selected node', 'double-tap Ctrl', '1/10 speed', 'without overshooting', 'height above the ground, not absolute', 'corner arrow scales uniformly', 'Double-click the pivot', 'middle mouse button', 'Reset zoom', 'Lowest, Highest, or Median', '1–32 cuts', 'neighbouring nodes land on the same map block', 'Paint riverbed with', 'Paint transition with', '0 disables the limit', 'Square Root / Logarithmic', 'Quadratic / Cubic', 'Only and All but', 'stored with the current dimension', 'curve strength stay as configured']) {
    assert.ok(page.includes(text), text);
  }
  const paths = features.find((feature) => feature.slug === 'paths');
  const section = (id) => paths.sections.find((item) => item.id === id);
  assert.match(section('paint-along-path').body, /without changing heights or water/);
  assert.match(section('operations-reference').table.rows.find(([label]) => label === 'Node → Snap to terrain')[1], /different from the Apply-dialog checkbox/);
  assert.match(section('downhill').body, /Snap to terrain runs first/);
  assert.match(section('height-profile').bullets.at(-1), /different final elevations/);
  assert.match(section('save-and-undo').table.rows.find(([label]) => label === 'Close / window close button')[1], /not a settings rollback/);
  assert.match(section('save-and-undo').table.rows.find(([label]) => label === 'WorldPainter undo / redo')[1], /clear the selection first/);
});

test('all eight Path Tool PR screenshots appear in the guide with original bytes', async () => {
  const screenshots = images.filter((src) => src.startsWith('pathtool/'));
  assert.equal(screenshots.length, 8);
  for (const src of screenshots) {
    assert.ok(html.get('paths.html').includes(`src="${imageUrl(src)}"`), src);
    assert.deepEqual(await readFile(join(output, 'assets/images', src)), await readFile(join(imageRoot, src)));
  }
});

test('Path Tool follow-up records source review without claiming plugin runtime testing', async () => {
  const note = await readFile(join(root, 'CONTENT-NOTES.md'), 'utf8');
  assert.match(note, /Path Tool documentation — PR #126/);
  assert.match(note, /still need a hands-on pass/);
  assert.doesNotMatch(note, /Keep tutorial placeholders|Incoming path-tool feature branch/);
});

test('support prioritizes Discord and provides a plain-language report without requiring metadata', () => {
  const docs = html.get('documentation.html');
  assert.match(docs, /Discord<\/a> for help, bug reports/);
  assert.match(docs, /no GitHub account is required/);
  assert.ok(docs.includes(escape(reportTemplate)));
  assert.match(docs, /Helpful extras, not requirements/);
});

test('backup and cleanup guidance distinguishes editable projects from exported Minecraft maps', () => {
  const docs = html.get('documentation.html');
  assert.match(docs, /normally keeps three previous saves/);
  assert.match(docs, /zero disables new backup rotation/);
  assert.match(docs, /accumulate across successive saves/);
  for (const name of ['Macro Selection', 'Heatmap', '3d Preview', 'My City Layer', 'PathPreviewLayer']) assert.ok(docs.includes(name));
  assert.match(docs, /dimension-wide, not automatically world-wide/);
  assert.match(docs, /Clearing painted data alone is not the same as deleting the custom layer/);
  assert.match(docs, /Restart WorldPainter without MacroMachine and verify that copy opens/);
  assert.match(docs, /Exported Minecraft maps do not depend on MacroMachine/);
});

test('Pages deployment publishes the verified static site from master only', async () => {
  const workflow = await readFile(join(root, '../.github/workflows/website-pages.yml'), 'utf8');
  assert.match(workflow, /working-directory: website/);
  assert.match(workflow, /cache-dependency-path: website\/package-lock\.json/);
  assert.match(workflow, /run: npm run verify/);
  assert.match(workflow, /run: npm run test:browser/);
  assert.match(workflow, /path: website\/dist/);
  assert.match(workflow, /deploy:\s+if: github\.event_name != 'pull_request' && github\.ref == 'refs\/heads\/master'/);
  assert.match(workflow, /name: github-pages/);
  assert.match(workflow, /uses: actions\/deploy-pages@v4/);
  assert.doesNotMatch(workflow, /webUI|build-storybook|pull_request_target/);
  await assert.doesNotReject(access(join(output, '.nojekyll')));
});

test('landing has one tool catalog and one visual demonstration area, not repeated tool introductions', () => {
  const home = html.get('index.html');
  assert.equal([...home.matchAll(/class="feature-grid"/g)].length, 1);
  assert.equal([...home.matchAll(/class="feature-card /g)].length, 4);
  assert.equal([...home.matchAll(/aria-roledescription="carousel"/g)].length, 1);
  assert.doesNotMatch(home, /tool-strip|tool-shortcut|hero-capabilities|showcase-|data-showcase|role="tablist"|See the tools in use/);
});

test('landing uses a plain introduction, project facts, one tool catalog, and practical information', () => {
  const home = html.get('index.html');
  assert.match(home, /<h1>Automate edits\. Place buildings\.<\/h1>/);
  assert.ok(home.indexOf('class="home-hero"') < home.indexOf('class="intro-facts wrap"'));
  assert.ok(home.indexOf('class="intro-facts wrap"') < home.indexOf('id="tools"'));
  assert.ok(home.indexOf('id="tools"') < home.indexOf('id="project"'));
  assert.match(home, /<div class="home-preview" role="group"/);
  assert.match(home, /id="download"><button[^>]*disabled/);
  assert.equal([...home.matchAll(/<button[^>]*disabled/g)].length, 1);
  for (const content of html.values()) assert.doesNotMatch(content, /closing-cta|cta-orbit|world-window|window-chrome|art-ribbon|floating-label|gradient-text|card-arrow/);
  for (const feature of features) {
    const page = html.get(`${feature.slug}.html`);
    assert.ok(page.includes(`<h1>${escape(feature.name)} ${feature.parent ? 'reference' : 'guide'}</h1>`));
    assert.ok(page.includes(escape(feature.guideIntro)));
    if (!feature.parent) assert.ok(home.includes(`Read the ${escape(feature.name)} guide`));
  }
  const firstGlance = home.split('id="tools"')[0];
  for (const fact of ['Free and open source', projectFacts.downloads, projectFacts.releases, projectFacts.developmentSince, 'Community-supported', 'Endorsed by WorldPainter']) assert.ok(firstGlance.includes(fact), fact);
  for (const text of ['Works inside WorldPainter.', 'Use your existing terrain, custom layers, and schematic files.', 'IR0NSIGHT started the project as a river tool', 'the 100-second overview']) assert.ok(home.includes(text), text);
});

test('landing keeps project facts once, removes repeated feature summaries, and has one closing action', () => {
  const home = html.get('index.html');
  const main = home.match(/<main id="main">([\s\S]*?)<\/main>/)[1];
  for (const phrase of ['In development since 2024', `${projectFacts.downloads} downloads`, `${projectFacts.releases} public releases`, projectFacts.license, 'Downloads count release assets, not unique users.', 'Vetted by WorldPainter’s developer', 'No account required']) {
    assert.equal(main.split(phrase).length - 1, 1, phrase);
  }
  assert.doesNotMatch(main, /class="workflow-list"|class="project-section|<h2>About the project<\/h2>|Read the setup guide/);
  assert.equal([...main.matchAll(/href="get-started\.html">Installation guide/g)].length, 2);
  assert.equal([...main.matchAll(/class="section wrap resources-section"/g)].length, 1);
  assert.doesNotMatch(home, /class="footer-resources"/);
  for (const page of html.values()) assert.doesNotMatch(page, /class="footer-resources"/);
  const paragraphs = [...main.matchAll(/<p(?:\s[^>]*)?>([\s\S]*?)<\/p>/g)]
    .map((match) => match[1].replace(/<[^>]*>/g, '').replace(/\s+/g, ' ').trim())
    .filter((text) => text.length > 80);
  assert.equal(paragraphs.length, new Set(paragraphs).size, 'Long prose should not repeat verbatim');
});

test('landing cards are concise without shortening the detailed feature pages', () => {
  const home = html.get('index.html');
  const cards = [...home.matchAll(/<a class="feature-card[^>]+href="([^"]+)"[^>]*>([\s\S]*?)<\/a>/g)];
  assert.equal(cards.length, 4);
  for (const [, href, card] of cards) {
    const feature = features.find((item) => `${item.slug}.html` === href);
    const summary = [...card.matchAll(/<p(?:\s[^>]*)?>([^<]+)<\/p>/g)].at(-1)[1];
    assert.notEqual(summary, escape(feature.description));
    assert.ok(summary.split(/\s+/).length <= 30, `${feature.name}: keep card summaries short`);
    assert.ok(html.get(href).includes(escape(feature.guideIntro)), 'Use the documentation introduction');
  }
});

test('hero carousel renders the five configured presentations on a scrollable track', () => {
  const carousel = html.get('index.html').split('<div class="home-preview"')[1].split('</section>')[0];
  assert.equal([...carousel.matchAll(/data-hero-slide/g)].length, 5);
  assert.equal([...carousel.matchAll(/data-active/g)].length, 1);
  assert.match(carousel, /data-hero-controls hidden/);
  assert.equal([...carousel.matchAll(/data-hero-detail/g)].length, 5);
  assert.doesNotMatch(carousel, /aria-label="Show (?:Gradient Editor|Filters &amp; Layers)"/);
  assert.doesNotMatch(carousel, /class="hero-slide-link"[^>]*>[\s\S]*?\d \/ \d/);
  assert.doesNotMatch(carousel, /data-hero-(?:play|previous|next)|hero-selectors/);
  for (const feature of features) {
    for (const presentation of feature.carousel ?? [{ id: feature.slug, name: feature.name, hero: feature.hero }]) {
      assert.ok(carousel.includes(`src="${imageUrl(presentation.hero.src)}"`));
      assert.ok(carousel.includes(`alt="${escape(presentation.hero.alt)}"`));
      assert.ok(carousel.includes(`href="${feature.slug}.html">${escape(presentation.name)}</a>`));
      assert.ok(carousel.includes(`aria-describedby="hero-caption-${presentation.id}"`));
      assert.ok(carousel.includes(`id="hero-caption-${presentation.id}">${escape(presentation.hero.caption)}</p>`));
    }
  }
});

test('feature showcases use the supplied GIFs with their original bytes preserved', async () => {
  for (const [slug, src] of [
    ['3d-preview', 'tutorial/macromachine_3d_preview_mountain-and-jungle.gif'],
    ['citylayer', 'tutorial/CityLayer/macromachine_citylayer_house-along-the-river.gif'],
    ['paths', 'tutorial/macromachine_roadtool_gravel-road-over-hill.gif'],
    ['macros', 'tutorial/macromachine_macros_paint-slopes-alpine.gif'],
  ]) {
    assert.equal(features.find((feature) => feature.slug === slug).hero.src, src);
    for (const page of ['index.html', `${slug}.html`]) {
      assert.ok(html.get(page).includes(`src="${imageUrl(src)}"`), page);
    }
    assert.deepEqual(await readFile(join(output, 'assets/images', src)), await readFile(join(imageRoot, src)));
  }
});

test('roads and rivers have separate carousel entries linking to the same tool', () => {
  const home = html.get('index.html');
  assert.match(home, /aria-label="Show Roads"/);
  assert.match(home, /aria-label="Show Rivers"/);
  assert.match(home, /href="paths\.html">Roads<\/a>/);
  assert.match(home, /href="paths\.html">Rivers<\/a>/);
  assert.match(home, /id="hero-caption-paths-roads"/);
  assert.match(home, /id="hero-caption-paths-rivers"/);
  assert.match(home, /src="assets\/images\/rivers_icon\.svg"/);
});

test('rivers use the supplied GIF in the carousel and guide without conversion', async () => {
  const src = 'tutorial/macromachine_rivertool_stream-down-the-hill-into-river.gif';
  const paths = features.find((feature) => feature.slug === 'paths');
  assert.equal(paths.carousel.find((presentation) => presentation.id === 'paths-rivers').hero.src, src);
  assert.equal(paths.sections.find((section) => section.title === 'Keep rivers downhill.').media.src, src);
  for (const page of ['index.html', 'paths.html']) {
    assert.ok(html.get(page).includes(`src="${imageUrl(src)}"`), page);
  }
  assert.deepEqual(await readFile(join(output, 'assets/images', src)), await readFile(join(imageRoot, src)));
});

test('landing describes missing capabilities, not a World Machine integration', () => {
  const home = html.get('index.html');
  const intro = [...home.matchAll(/<p class="hero-description">([^<]+)<\/p>/g)].map((match) => match[1]);
  assert.deepEqual(intro, ['MacroMachine provides capabilities missing from WorldPainter and World Machine—from precise manual edits to reusable global operations.']);
  assert.doesNotMatch(home, /complement/i);
  assert.doesNotMatch(html.get('get-started.html'), /complement/i);
  assert.doesNotMatch(home, /class="hero-capabilities"/);
  assert.match(home, /<meta name="description" content="[^"]*reusable global operations/);
});

test('download areas link to the exact version-specific installer report without fabricated scan counts', () => {
  assert.equal(installerReport.sha256, 'df719e2cfc8303f9e6b6c36093e860bf7c71cad016ed5d4c22420b4a46914f02');
  assert.ok(installerReport.url.includes(`/file/${installerReport.sha256}/`));
  assert.ok(installerReport.fileName.includes(installerReport.version));
  for (const page of ['index.html', 'get-started.html']) {
    const content = html.get(page);
    assert.ok(content.includes(`href="${installerReport.url}"`));
    assert.ok(content.includes(installerReport.sha256));
    assert.ok(content.includes(installerReport.fileName));
    assert.match(content, /Installer file and SHA-256/);
    assert.doesNotMatch(content, /class="installer-scan|100% safe|virus.free|x-apikey|VIRUSTOTAL_API_KEY/i);
  }
});

test('macros and landing describe reusable global operations and the debugger stepper', () => {
  const home = html.get('index.html');
  const macro = html.get('macros.html');
  assert.match(home, /Build reusable sequences of global operations without programming/);
  assert.match(home, /inspect each action with the debugger/);
  assert.match(macro, /<h1>Macros guide<\/h1>/);
  assert.match(macro, /stored sequence of global operations/);
  assert.match(macro, /debugger stepper to step through operations one at a time/);
  assert.match(macro, /log tab reports action statistics/);
});

test('macros page explains portable exports with included custom layers', () => {
  const macro = html.get('macros.html');
  assert.match(macro, /<h2>Export and share macros\.<\/h2>/);
  assert.match(macro, /Export a macro as a \.macro file to share it with others/);
  assert.match(macro, /file includes every custom layer used by the macro/);
  assert.match(macro, /recipients do not need to recreate those layers/);
  assert.match(macro, /Anyone with MacroMachine installed can import the file into any WorldPainter world on their own computer/);
});

test('first-glance facts and project context are separate semantic blocks', () => {
  const home = html.get('index.html');
  const facts = home.match(/<section class="intro-facts wrap"[\s\S]*?<div class="intro-context">/)?.[0];
  assert.ok(facts);
  const list = facts.match(/<ul class="fact-list">([\s\S]*?)<\/ul>/)?.[1];
  assert.ok(list);
  assert.equal([...list.matchAll(/<li>/g)].length, 5);
  for (const value of ['Free to use', 'Open source', projectFacts.license, projectFacts.developmentSince, projectFacts.downloads, projectFacts.releases]) assert.ok(list.includes(value), value);
  assert.match(facts, /<\/ul><p class="stats-note">Downloads count release assets, not unique users\. Figures checked 22 November 2026\.<\/p><div class="intro-context">/);
  const context = home.match(/<div class="intro-context">([\s\S]*?)<\/div>/)?.[1];
  assert.ok(context);
  assert.equal([...context.matchAll(/<section>/g)].length, 2);
  assert.match(context, /Community-supported<\/span><\/h2><p>Get help, report bugs, and share macros on [\s\S]*?Discord[\s\S]*?No GitHub account is needed/);
  assert.match(context, /Endorsed by WorldPainter<\/span><\/h2><p>Vetted by WorldPainter’s developer and previously built on the official WorldPainter build server\. Current builds run separately to support Java 21\.<\/p>/);
  const icons = [...(list + context).matchAll(/<span class="fact-icon" aria-hidden="true"><svg[^>]*>[\s\S]*?<\/svg><\/span>/g)];
  assert.equal(icons.length, 7);
  assert.equal([...(list + context).matchAll(/<svg/g)].length, 7);
  assert.match(list, /href="[^"]+\/blob\/master\/LICENCE"/);
  assert.equal([...list.matchAll(/href="[^"]+\/releases"/g)].length, 2);
});

test('all local links, fragments, scripts, stylesheets, and image sources resolve', async () => {
  for (const [page, content] of html) {
    for (const match of content.matchAll(/(?:href|src|data-image)="([^"]+)"/g)) {
      const reference = match[1];
      if (/^https?:/.test(reference)) continue;
      const [file, fragment] = reference.split('#');
      const target = file || page;
      const absolute = resolve(output, decodeURIComponent(target));
      assert.ok(absolute.startsWith(output + sep), `${page}: path must stay in build output`);
      await assert.doesNotReject(access(absolute), `${page}: missing ${reference}`);
      if (fragment) {
        const targetHtml = html.get(target);
        assert.ok(targetHtml?.includes(`id="${fragment}"`), `${page}: missing fragment ${reference}`);
      }
    }
  }
});

test('copies all source images without generating an automatic image gallery', async () => {
  assert.deepEqual(images, await listImages());
  assert.ok(images.length >= 30);
  for (const image of images) {
    await assert.doesNotReject(access(join(output, 'assets/images', image)));
  }
  for (const page of html.values()) assert.doesNotMatch(page, /gallery-grid|data-filter=|gallery\.html/);
});

test('each page has exactly one H1, unique IDs, a title, language, and a description', () => {
  for (const [page, content] of html) {
    assert.equal([...content.matchAll(/<h1[ >]/g)].length, 1, page);
    const ids = [...content.matchAll(/\bid="([^"]+)"/g)].map((m) => m[1]);
    assert.equal(ids.length, new Set(ids).size, `${page}: duplicate IDs`);
    assert.ok(content.includes('<html lang="en">'), page);
    assert.match(content, /<title>[^<]+<\/title>/);
    assert.match(content, /<meta name="description" content="[^"]+">/);
    for (const [image] of content.matchAll(/<img\b[^>]*>/g)) assert.match(image, /\balt="[^"]*"/, `${page}: image without alt`);
  }
});

test('download placeholders are explicit and disabled, never fake download links', () => {
  for (const page of ['index.html', 'get-started.html']) {
    const content = html.get(page);
    assert.match(content, /<button[^>]*disabled[^>]*aria-describedby="download-note"/);
    assert.match(content, /[Pp]laceholder/);
    assert.doesNotMatch(content, /href="#"/);
  }
});

test('project facts explain price, access, licensing, release history, and support', () => {
  const home = html.get('index.html');
  assert.match(home, /Free and open source/);
  assert.match(home, /No account required/);
  assert.match(home, /CC0 1\.0/);
  assert.match(home, /Community-supported/);
  assert.match(home, /20\+/);
  assert.match(home, /~500/);
  assert.match(home, /Downloads count release assets, not unique users/);
  assert.match(home, /Endorsed by WorldPainter/);
  assert.match(home, /Vetted by WorldPainter’s developer/);
  assert.match(home, /previously built on the official WorldPainter build server/);
  assert.match(home, /Current builds run separately to support Java 21/);
});

test('development history describes the continuous project since 2024', () => {
  for (const name of ['index.html', 'get-started.html']) {
    const content = html.get(name);
    assert.ok(content.includes(`In development since ${projectFacts.developmentSince}`));
    assert.match(content, /In development since 2024(?:\.|<)/);
    assert.doesNotMatch(content, /since August 2025|Macro development since|Earlier path-tool work/);
  }
});

test('website colors are centralized in the source-traced Swing palette', async () => {
  const stylesheet = await readFile(join(output, 'assets/styles.css'), 'utf8');
  const palette = await readFile(join(output, 'assets/palette.css'), 'utf8');
  assert.match(stylesheet, /@import url\("\.\/palette\.css"\)/);
  assert.doesNotMatch(stylesheet, /#[0-9a-f]{3,8}\b/i);
  assert.doesNotMatch(stylesheet, /(?:linear|radial|conic)-gradient\s*\(/i);
  for (const color of ['#cdc7ff', '#743e74', '#a349a4', '#558b57', '#84bc85', '#1b0073', '#5e37af', '#006073', '#559eb2', '#e8e8e8', '#b0b0b0', '#404040']) assert.ok(palette.includes(color), color);
  for (const feature of features) assert.ok(palette.includes(`.accent-${feature.accent}`), feature.slug);
});

test('CityLayer emphasizes deliberate city layouts and continuous schematic walls', () => {
  const city = html.get('citylayer.html');
  assert.match(city, /<h1>CityLayer guide<\/h1>/);
  assert.match(city, /instead of painting randomly placed buildings with a Custom Object Layer/);
  assert.match(city, /Design the city layout yourself/);
  assert.match(html.get('index.html'), /Choose each schematic’s position instead of random placement/);
  assert.match(html.get('index.html'), /Arrange individual buildings or assemble continuous walls/);
  assert.match(city, /full control over which building goes where/);
  assert.match(city, /Select buildings individually or in groups to move, rotate, mirror, or copy them/);
  assert.match(city, /<h2>Build continuous city walls\.<\/h2>/);
  assert.match(city, /Arrange wall schematics end to end/);
});

test('important CityLayer and preview limitations are retained', () => {
  const city = html.get('citylayer.html');
  assert.match(city, /same CityLayer/);
  assert.match(city, /clears the whole undo history of the map/);
  assert.match(city, /houses of the removed schematics and of every schematic listed below them/);
  assert.match(html.get('3d-preview.html'), /Large selections can cause lag/);
});

test('CityLayer guide describes the City Tool merged in PR #127', () => {
  const city = html.get('citylayer.html');
  assert.match(city, /PR #127/);
  assert.match(city, /undo \/ redo restores building placements/);
  assert.match(city, /Layer → Tutorial layer/);
  for (const id of ['layer-menu', 'picker', 'select-and-place', 'transform', 'randomize', 'overlays', 'undo', 'controls']) {
    assert.match(city, new RegExp(`id="${id}"`), id);
  }
  for (const image of ['picker.png', 'transform.png', 'randomize-rotation.png', 'overlays-menu.png', 'layer-menu.png', 'remove-schematics-warning.png']) {
    assert.ok(city.includes(`citytool/${image}`), image);
  }
  // the retired tool-settings panel and its controls are gone
  assert.doesNotMatch(city, /undo\/redo does not restore CityLayer building placements|Choose the house in the tool options|Right click to move the selection center|City Tool options|brush type with no selection/);
});

test('3D Preview explains the live export view and distinguishes its renderer from Minecraft', () => {
  const content = html.get('3d-preview.html');
  assert.match(content, /enable Auto Update/);
  assert.match(content, /roughly a second/);
  assert.match(content, /Times vary with selection size, map contents, and hardware/);
  assert.match(content, /background test exports/);
  assert.match(content, /custom Minecraft-style renderer, not a running Minecraft client/);
});

test('provided SVG tool icons appear on catalog cards and remain copied assets', () => {
  for (const slug of ['macros', 'citylayer', 'paths', '3d-preview']) {
    const feature = features.find((item) => item.slug === slug);
    assert.ok(feature.icon?.endsWith('.svg'));
    const source = `src="${imageUrl(feature.icon)}"`;
    const card = [...html.get('index.html').matchAll(/<a class="feature-card[^>]+href="([^"]+)"[^>]*>([\s\S]*?)<\/a>/g)].find((item) => item[1] === `${slug}.html`)?.[2];
    assert.ok(card?.includes(source), `${slug}: catalog card icon`);
    assert.ok(images.includes(feature.icon), `${slug}: icon in asset manifest`);
  }
});

test('HTML escaping and image URLs handle special characters and spaces', () => {
  assert.equal(escape('<a "x" & \'y\'>'), '&lt;a &quot;x&quot; &amp; &#39;y&#39;&gt;');
  assert.equal(imageUrl('tutorial/a b.PNG'), 'assets/images/tutorial/a%20b.PNG');
});
