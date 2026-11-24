import { test } from 'node:test';
import assert from 'node:assert/strict';
import { readFile, access } from 'node:fs/promises';
import { join, resolve, sep } from 'node:path';
import { build, root, output, imageRoot, listImages } from '../build.mjs';
import { features, projectFacts } from '../src/content.mjs';
import { escape, imageUrl, documentationImages } from '../src/templates.mjs';

const { pages, images } = await build();
const html = new Map(await Promise.all(pages.map(async (name) => [name, await readFile(join(output, name), 'utf8')])));

test('builds a landing page, a dedicated page per major feature, a gallery, and a guide', () => {
  assert.equal(pages.length, features.length + 3);
  for (const name of ['index.html', 'citylayer.html', 'paths.html', '3d-preview.html', 'macros.html', 'gradient-editor.html', 'filters.html', 'gallery.html', 'get-started.html']) assert.ok(html.has(name), name);
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

test('included tools use four SVG icon links with descriptions instead of thumbnails or arrows', () => {
  const strip = html.get('index.html').match(/<nav class="tool-strip"[\s\S]*?<\/nav>/)?.[0];
  assert.ok(strip);
  assert.equal([...strip.matchAll(/class="tool-shortcut accent-/g)].length, 4);
  assert.equal([...strip.matchAll(/class="tool-shortcut-icon"/g)].length, 4);
  assert.equal([...strip.matchAll(/src="[^"]+\.svg"/g)].length, 4);
  assert.equal([...strip.matchAll(/class="tool-shortcut-summary"/g)].length, 4);
  assert.doesNotMatch(strip, /→|↗|tool-shortcut-image|tutorial\//);
});

test('landing uses a plain introduction, then tools, examples, and modest project information', () => {
  const home = html.get('index.html');
  assert.match(home, /<h1>Automate edits\. Place buildings\.<\/h1>/);
  assert.ok(home.indexOf('id="tools"') < home.indexOf('id="in-action"'));
  assert.ok(home.indexOf('id="in-action"') < home.indexOf('id="project"'));
  assert.match(home, /<div class="home-preview" role="group"/);
  assert.match(home, /id="download"><button[^>]*disabled/);
  assert.equal([...home.matchAll(/<button[^>]*disabled/g)].length, 1);
  for (const content of html.values()) assert.doesNotMatch(content, /closing-cta|cta-orbit|world-window|window-chrome|art-ribbon|floating-label|gradient-text|card-arrow/);
  for (const feature of features) {
    const page = html.get(`${feature.slug}.html`);
    assert.ok(page.includes(`<h1>${escape(feature.title.replaceAll('\n', ' '))}</h1>`));
    assert.ok(page.includes(escape(feature.tag)));
    for (const benefit of feature.benefits) assert.ok(page.includes(escape(benefit)));
    assert.ok(home.includes(`About ${escape(feature.name)}`));
  }
  const firstGlance = home.split('id="tools"')[0];
  for (const fact of ['Free and open source', projectFacts.downloads, projectFacts.releases, projectFacts.developmentSince, 'Community-supported', 'Endorsed by WorldPainter']) assert.ok(firstGlance.includes(fact), fact);
  for (const text of ['Works inside WorldPainter.', 'Use your existing layers and schematics.', 'Reuse global operations in a macro.', 'See the export result while you edit.', 'The 100-second overview']) assert.ok(home.includes(text), text);
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
    for (const page of ['index.html', `${slug}.html`, 'gallery.html']) {
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

test('rivers use the supplied GIF in the carousel, tool page, and gallery without conversion', async () => {
  const src = 'tutorial/macromachine_rivertool_stream-down-the-hill-into-river.gif';
  const paths = features.find((feature) => feature.slug === 'paths');
  assert.equal(paths.carousel.find((presentation) => presentation.id === 'paths-rivers').hero.src, src);
  assert.equal(paths.sections.find((section) => section.title === 'Keep rivers downhill.').media.src, src);
  for (const page of ['index.html', 'paths.html', 'gallery.html']) {
    assert.ok(html.get(page).includes(`src="${imageUrl(src)}"`), page);
  }
  assert.deepEqual(await readFile(join(output, 'assets/images', src)), await readFile(join(imageRoot, src)));
});

test('landing introduction positions the plugin within the WorldPainter and World Machine workflow', () => {
  const home = html.get('index.html');
  const intro = [...home.matchAll(/<p class="hero-description">([^<]+)<\/p>/g)].map((match) => match[1]).join(' ');
  assert.ok(intro);
  assert.match(intro, /MacroMachine extends WorldPainter/);
  assert.match(intro, /editing and visualization tools that complement a World Machine workflow/);
  const capabilities = home.match(/<ul class="hero-capabilities">([\s\S]*?)<\/ul>/)?.[1];
  assert.ok(capabilities);
  assert.deepEqual([...capabilities.matchAll(/<li>([^<]+)<\/li>/g)].map((match) => match[1]), ['Place and arrange individual schematics.', 'Build connected roads and rivers that blend into the terrain.', 'Reuse global operations in macros and step through them with the debugger.', 'See the exported result update in 3D as you edit.']);
  assert.match(home, /<meta name="description" content="[^"]*WorldPainter \+ World Machine workflow/);
});

test('macros and landing describe reusable global operations and the debugger stepper', () => {
  const home = html.get('index.html');
  const macro = html.get('macros.html');
  assert.match(home, /data-tool="macros"[\s\S]*?Reuse global operations<\/span>/);
  assert.match(home, /Reuse global operations in macros and step through them with the debugger/);
  assert.match(home, /step through operations with the debugger stepper/);
  assert.match(macro, /REUSABLE GLOBAL OPERATIONS/);
  assert.match(macro, /<h1>Reuse global operations\.<\/h1>/);
  assert.match(macro, /stored sequence of global operations/);
  assert.match(macro, /Debugger stepper/);
  assert.match(macro, /debugger stepper to step through operations one at a time/);
  assert.match(macro, /log tab reports action statistics/);
});

test('macros page explains portable exports with included custom layers', () => {
  const macro = html.get('macros.html');
  assert.match(macro, /<li>Shareable \.macro files<\/li>/);
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
  assert.match(context, /Community-supported<\/span><\/h2><p>Get help on [\s\S]*?report bugs on [\s\S]*?<\/p>/);
  assert.match(context, /Endorsed by WorldPainter<\/span><\/h2><p>Vetted by WorldPainter’s developer and previously built on the official WorldPainter build server\. Current builds run separately to support Java 21\.<\/p>/);
  const icons = [...(list + context).matchAll(/<span class="fact-icon" aria-hidden="true"><svg[^>]*>[\s\S]*?<\/svg><\/span>/g)];
  assert.equal(icons.length, 7);
  assert.equal([...(list + context).matchAll(/<svg/g)].length, 7);
  assert.match(list, /href="[^"]+\/blob\/master\/LICENCE"/);
  assert.equal([...list.matchAll(/href="[^"]+\/releases"/g)].length, 2);
});

test('all local links, fragments, scripts, stylesheets, and image sources resolve', async () => {
  for (const [page, content] of html) {
    for (const match of content.matchAll(/(?:href|src|data-image|data-showcase-src|data-showcase-link)="([^"]+)"/g)) {
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

test('copies all source images but displays only documentation images in the gallery', async () => {
  assert.deepEqual(images, await listImages());
  assert.ok(images.length >= 30);
  const gallery = html.get('gallery.html').split('<div class="gallery-grid">')[1].split('</section>')[0];
  const documented = documentationImages(images);
  assert.equal(documented.length, 30);
  for (const image of images) {
    assert.equal(gallery.includes(`src="${imageUrl(image)}"`), documented.includes(image), `Gallery inclusion: ${image}`);
    await assert.doesNotReject(access(join(output, 'assets/images', image)));
  }
  assert.doesNotMatch(html.get('gallery.html'), /Brand assets|data-filter="branding"/);
  assert.ok(html.get('index.html').includes(`View all ${documented.length} images`));
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
  assert.match(home, /No account, subscription, or activation key/);
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
    assert.ok(content.includes(`In development since ${projectFacts.developmentSince}.`));
    assert.match(content, /In development since 2024\./);
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
  assert.match(city, /<h1>Choose which building goes where\.<\/h1>/);
  for (const page of [city, html.get('index.html')]) {
    assert.match(page, /rather than relying on random placement from Custom Object Layers/);
    assert.match(page, /Design the city layout yourself/);
    assert.match(page, /continuous walls built from schematics/);
  }
  assert.match(city, /full control over which building goes where/);
  assert.match(city, /Select buildings individually or in groups to move, rotate, mirror, or copy them/);
  assert.match(city, /<h2>Build continuous city walls\.<\/h2>/);
  assert.match(city, /Arrange wall schematics end to end/);
});

test('important CityLayer and preview limitations are retained', () => {
  assert.match(html.get('citylayer.html'), /undo\/redo does not restore CityLayer building placements/);
  assert.match(html.get('citylayer.html'), /same CityLayer/);
  assert.match(html.get('3d-preview.html'), /Large selections can cause lag/);
});

test('3D Preview explains the live export view and distinguishes its renderer from Minecraft', () => {
  const content = html.get('3d-preview.html');
  assert.match(content, /With Auto Update enabled, the view follows your edits/);
  assert.match(content, /roughly a second/);
  assert.match(content, /Times vary with selection size, map contents, and hardware/);
  assert.match(content, /background test exports/);
  assert.match(content, /custom Minecraft-style renderer, not a running Minecraft client/);
});

test('provided SVG tool icons appear on cards, screenshot tabs, and feature headings', () => {
  for (const slug of ['macros', 'citylayer', 'paths', '3d-preview']) {
    const feature = features.find((item) => item.slug === slug);
    assert.ok(feature.icon?.endsWith('.svg'));
    const source = `src="${imageUrl(feature.icon)}"`;
    assert.ok(html.get('index.html').split(source).length >= 3, `${slug}: card and tab icon`);
    assert.ok(html.get(`${slug}.html`).includes(source), `${slug}: feature heading icon`);
    assert.ok(images.includes(feature.icon), `${slug}: icon in asset manifest`);
  }
});

test('HTML escaping and image URLs handle special characters and spaces', () => {
  assert.equal(escape('<a "x" & \'y\'>'), '&lt;a &quot;x&quot; &amp; &#39;y&#39;&gt;');
  assert.equal(imageUrl('tutorial/a b.PNG'), 'assets/images/tutorial/a%20b.PNG');
});
