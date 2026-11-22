import { test } from 'node:test';
import assert from 'node:assert/strict';
import { readFile, access } from 'node:fs/promises';
import { join, resolve, sep } from 'node:path';
import { build, output, listImages } from '../build.mjs';
import { features, projectFacts } from '../src/content.mjs';
import { escape, imageUrl, documentationImages } from '../src/templates.mjs';

const { pages, images } = await build();
const html = new Map(await Promise.all(pages.map(async (name) => [name, await readFile(join(output, name), 'utf8')])));

test('builds a landing page, a dedicated page per major feature, a gallery, and a guide', () => {
  assert.equal(pages.length, features.length + 3);
  for (const name of ['index.html', 'citylayer.html', 'paths.html', '3d-preview.html', 'macros.html', 'gradient-editor.html', 'filters.html', 'gallery.html', 'get-started.html']) assert.ok(html.has(name), name);
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
  for (const color of ['#cdc7ff', '#743e74', '#a349a4', '#558b57', '#84bc85', '#1b0073', '#5e37af', '#006073', '#559eb2', '#e8e8e8', '#b0b0b0', '#404040']) assert.ok(palette.includes(color), color);
  for (const feature of features) assert.ok(palette.includes(`.accent-${feature.accent}`), feature.slug);
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
