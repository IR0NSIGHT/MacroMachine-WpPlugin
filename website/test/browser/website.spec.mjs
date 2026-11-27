import { test, expect } from '@playwright/test';
import AxeBuilder from '@axe-core/playwright';
import { installerReport, downloads } from '../../src/content.mjs';

const pages = ['index', 'citylayer', 'paths', '3d-preview', 'macros', 'gradient-editor', 'filters', 'get-started', 'documentation'];

test('documentation directly opens each tool and allows switching tools without returning to the index', async ({ page }) => {
  await page.goto('/documentation.html');
  for (const [name, slug] of [['3D Preview', '3d-preview'], ['Path Tool', 'paths'], ['CityLayer', 'citylayer'], ['Macros', 'macros']]) {
    const navigation = page.getByRole('navigation', { name: 'Browse by tool', exact: true });
    await navigation.getByRole('link', { name, exact: true }).click();
    await expect(page).toHaveURL(new RegExp(`${slug}\\.html$`));
    await expect(page.getByRole('heading', { level: 1 })).toHaveText(`${name} guide`);
    await expect(page.getByRole('navigation', { name: 'Browse by tool', exact: true }).getByRole('link', { name, exact: true })).toHaveAttribute('aria-current', 'page');
  }
  const navigation = page.getByRole('navigation', { name: 'Browse by tool', exact: true });
  await navigation.getByRole('link', { name: 'Gradient Editor reference', exact: true }).click();
  await expect(page.getByRole('navigation', { name: 'Browse by tool', exact: true }).getByRole('link', { name: 'Gradient Editor reference', exact: true })).toHaveAttribute('aria-current', 'page');
  await expect(page.locator('.doc-tool-current > .doc-tool-name')).toHaveText('Macros');
});

test('primary navigation exposes Get started, Documentation, GitHub, Discord, and Download', async ({ page }) => {
  await page.goto('/');
  const menu = page.getByRole('button', { name: 'Menu', exact: true });
  if (await menu.isVisible()) await menu.click();
  const nav = page.getByRole('navigation', { name: 'Main navigation', exact: true });
  await expect(nav.getByRole('link')).toHaveText(['Get started', 'Documentation', 'GitHub', 'Discord', 'Download']);
  await expect(nav.getByRole('link', { name: 'GitHub', exact: true })).toHaveAttribute('href', 'https://github.com/IR0NSIGHT/MacroMachine-WpPlugin');
  await expect(nav.getByRole('link', { name: 'Discord', exact: true })).toHaveAttribute('href', 'https://discord.gg/9kRJEMfGeE');
  await expect(nav.getByRole('link', { name: 'Download', exact: true })).toHaveAttribute('href', downloads.installer);
});

test('macro references are nested documentation with a breadcrumb back to the Macros guide', async ({ page }) => {
  await page.goto('/documentation.html#reference');
  const references = page.getByRole('list', { name: 'Macros references', exact: true });
  await references.getByRole('link', { name: 'Gradient Editor reference', exact: true }).click();
  await expect(page.getByRole('heading', { level: 1 })).toHaveText('Gradient Editor reference');
  await page.getByRole('navigation', { name: 'Breadcrumb', exact: true }).getByRole('link', { name: 'Macros', exact: true }).click();
  await expect(page.getByRole('heading', { level: 1 })).toHaveText('Macros guide');
  await page.locator('#macro-reference').getByRole('link', { name: 'Filters & Layers', exact: true }).click();
  await expect(page.getByRole('heading', { level: 1 })).toHaveText('Filters & Layers reference');
  await expect(page.locator('.feature-hero, .benefits, .related-section')).toHaveCount(0);
});

test('installer report links and expandable checksums are available without browser API requests', async ({ page }) => {
  const apiRequests = [];
  page.on('request', (request) => { if (request.url().startsWith('https://www.virustotal.com/api/')) apiRequests.push(request.url()); });
  for (const slug of ['index', 'get-started']) {
    await page.goto(`/${slug}.html`);
    const report = page.locator('.installer-report');
    await expect(report.getByRole('link', { name: `VirusTotal report for installer ${installerReport.version}`, exact: true })).toHaveAttribute('href', installerReport.url);
    await report.locator('summary').click();
    await expect(report.locator('code')).toBeVisible();
    await expect(report.locator('code')).toHaveText(installerReport.sha256);
    await expect(report.locator('details')).toContainText(installerReport.fileName);
  }
  expect(apiRequests).toEqual([]);
});

test('documentation tasks navigate to anchored guides and control references', async ({ page }) => {
  await page.goto('/');
  const menu = page.getByRole('button', { name: 'Menu' });
  if (await menu.isVisible()) await menu.click();
  await page.locator('#main-nav').getByRole('link', { name: 'Documentation', exact: true }).click();
  await expect(page).toHaveURL(/documentation\.html$/);
  await expect(page.locator('#main-nav a[href="documentation.html"]')).toHaveAttribute('aria-current', 'page');
  await page.getByRole('link', { name: 'Place a house', exact: true }).click();
  await expect(page).toHaveURL(/citylayer\.html#place-a-building$/);
  await expect(page.locator('#place-a-building')).toBeVisible();
  await page.getByRole('navigation', { name: 'On this page', exact: true }).getByRole('link', { name: 'CityLayer controls reference.', exact: true }).click();
  await expect(page).toHaveURL(/citylayer\.html#controls$/);
  await expect(page.getByRole('table', { name: 'CityLayer controls reference.', exact: true })).toBeVisible();
});

test('CityLayer sample downloads directly with its creator and source visible', async ({ page }) => {
  await page.goto('/citylayer.html#sample-schematic');
  const sample = page.locator('#sample-schematic');
  await expect(sample).toContainText('Ektelion');
  await expect(sample.getByRole('link', { name: 'Original project by Ektelion on Planet Minecraft', exact: true })).toHaveAttribute('href', 'https://www.planetminecraft.com/project/2-9-ukrainian-rural-house-by-ektelion/');
  const [download] = await Promise.all([
    page.waitForEvent('download'),
    sample.getByRole('link', { name: 'Download peasant_house_1.schem', exact: true }).click(),
  ]);
  expect(download.suggestedFilename()).toBe('peasant_house_1.schem');
  expect(await download.failure()).toBeNull();
  const response = await page.request.get('/downloads/peasant_house_1.schem');
  expect(response.ok()).toBe(true);
  expect((await response.body()).length).toBeGreaterThan(0);
});

test('first-use guide leads with Auto Update and support offers a reusable report', async ({ page }) => {
  await page.goto('/get-started.html#first-preview');
  await expect(page.locator('#first-preview')).toContainText(/enable Auto Update/i);
  await expect(page.locator('#requirements')).toContainText('WorldPainter must run Java');
  await page.goto('/documentation.html#help');
  await expect(page.locator('.report-template')).toContainText('When I [action or steps]');
  await expect(page.locator('.report-template')).toContainText('Instead, [what actually happens]');
  await expect(page.locator('#help')).toContainText('no GitHub account is required');
  await expect(page.locator('#help').getByRole('link', { name: 'Discord', exact: true })).toHaveAttribute('href', 'https://discord.gg/9kRJEMfGeE');
});

test('updated path tutorials and references are reachable from documentation', async ({ page }) => {
  for (const [label, id] of [['Build a road', 'road-tutorial'], ['Build a river', 'river-tutorial'], ['Paint along a path', 'paint-along-path']]) {
    await page.goto('/documentation.html');
    await page.getByRole('link', { name: label, exact: true }).click();
    await expect(page).toHaveURL(new RegExp(`paths\\.html#${id}$`));
    await expect(page.locator(`#${id} .step-list li`)).not.toHaveCount(0);
    await expect(page.locator('.planned-tutorial')).toHaveCount(0);
  }
  await page.getByRole('navigation', { name: 'On this page', exact: true }).getByRole('link', { name: 'Path Tool controls reference.', exact: true }).click();
  await expect(page).toHaveURL(/paths\.html#controls$/);
  await expect(page.getByRole('table', { name: 'Path Tool controls reference.', exact: true })).toBeVisible();
  await expect(page.locator('#save-and-undo')).toContainText('clear the selection first');
});

test('Path Tool screenshots enlarge beside the instructions they illustrate', async ({ page }) => {
  await page.goto('/paths.html#height-profile');
  const preview = page.locator('#height-profile .image-expand');
  await preview.click();
  await expect(page.getByRole('dialog')).toBeVisible();
  await expect(page.locator('.dialog-image')).toHaveAttribute('src', 'assets/images/pathtool/height-editor.png');
  await page.keyboard.press('Escape');
  await expect(preview).toBeFocused();
  await expect(page.locator('main img[src*="/pathtool/"]')).toHaveCount(8);
  const image = page.locator('#river-settings .image-expand');
  await image.click();
  await expect(page.locator('.dialog-image')).toHaveAttribute('src', 'assets/images/pathtool/apply-river.png');
});

for (const slug of pages) {
  test(`${slug}: no broken images, horizontal overflow, or accessibility violations`, async ({ page }) => {
    const errors = [];
    page.on('pageerror', (error) => errors.push(error.message));
    await page.goto(`/${slug}.html`);
    await page.locator('img').evaluateAll(async (images) => {
      await Promise.all(images.map((image) => {
        image.loading = 'eager';
        return image.decode().catch(() => {});
      }));
    });
    const broken = await page.locator('img[src]').evaluateAll((images) => images.filter((image) => !image.complete || image.naturalWidth === 0).map((image) => image.src));
    expect(broken).toEqual([]);
    expect(errors).toEqual([]);
    expect(await page.evaluate(() => document.documentElement.scrollWidth <= window.innerWidth)).toBe(true);
    const tinyText = await page.locator('body *').evaluateAll((elements) => elements
      .filter((element) => element.getClientRects().length && getComputedStyle(element).visibility !== 'hidden' && [...element.childNodes].some((node) => node.nodeType === Node.TEXT_NODE && node.textContent.trim()))
      .filter((element) => parseFloat(getComputedStyle(element).fontSize) < 14)
      .map((element) => ({ text: element.textContent.trim().slice(0, 60), size: getComputedStyle(element).fontSize })));
    expect(tinyText).toEqual([]);
    const results = await new AxeBuilder({ page }).withTags(['wcag2a', 'wcag2aa', 'wcag21aa']).analyze();
    expect(results.violations).toEqual([]);
  });
}

test('landing presents one introduction, prominent facts, and a single closing installation section', async ({ page }) => {
  await page.goto('/');
  await expect(page.locator('.hero-description')).toHaveCount(1);
  await expect(page.locator('.hero-capabilities, .tool-strip, .showcase-section')).toHaveCount(0);
  await expect(page.locator('.feature-grid')).toHaveCount(1);
  await expect(page.locator('.feature-card')).toHaveCount(4);
  await expect(page.getByRole('tablist')).toHaveCount(0);
  await expect(page.locator('.fact-list li')).toHaveCount(5);
  await expect(page.locator('.intro-context')).toContainText('Endorsed by WorldPainter');
  await expect(page.locator('.workflow-list')).toHaveCount(0);
  await expect(page.locator('#project')).toContainText('IR0NSIGHT started the project as a river tool');
  await expect(page.locator('.resources-section')).toHaveCount(1);
  await expect(page.locator('.resources-section').getByRole('link', { name: 'Guides and reference', exact: true })).toHaveAttribute('href', 'documentation.html');
  await expect(page.locator('.footer-resources')).toHaveCount(0);
  await page.locator('.resources-section').getByRole('link', { name: 'Installation guide', exact: true }).click();
  await expect(page).toHaveURL(/get-started\.html$/);
  await expect(page.locator('.footer-resources')).toHaveCount(0);
});

test('the four-tool catalog navigates directly to documentation', async ({ page }) => {
  await page.goto('/');
  const tools = page.locator('#tools');
  await expect(tools.locator('.feature-card')).toHaveCount(4);
  await expect(tools.locator('.tool-mark img[src$=".svg"]')).toHaveCount(4);
  await tools.locator('a[href="citylayer.html"]').click();
  await expect(page).toHaveURL(/citylayer\.html$/);
  await expect(page.getByRole('heading', { level: 1 })).toHaveText('CityLayer guide');
});

test('guide image dialogs restore focus when closed', async ({ page }) => {
  await page.goto('/citylayer.html');
  const image = page.locator('.feature-story .image-expand').first();
  await image.click();
  await expect(page.getByRole('dialog')).toBeVisible();
  await page.keyboard.press('Escape');
  await expect(page.getByRole('dialog')).not.toBeVisible();
  await expect(image).toBeFocused();
});

test('download buttons link to the latest installer and feature links lead to real pages', async ({ page }) => {
  await page.goto('/get-started.html');
  await expect(page.getByRole('link', { name: 'Download for Windows', exact: true })).toHaveAttribute('href', downloads.installer);
  await page.goto('/');
  await expect(page.getByRole('link', { name: 'Download MacroMachine', exact: true })).toHaveAttribute('href', downloads.installer);
  await page.locator('.feature-card[href="citylayer.html"]').click();
  await expect(page).toHaveURL(/citylayer\.html$/);
  await expect(page.getByRole('heading', { level: 1 })).toHaveText('CityLayer guide');
});

test('website works under the GitHub Pages repository subdirectory', async ({ page }) => {
  const prefix = '/MacroMachine-WpPlugin/';
  await page.route(`**${prefix}**`, async (route) => {
    const target = new URL(route.request().url());
    target.pathname = target.pathname.slice(prefix.length - 1);
    await route.fulfill({ response: await route.fetch({ url: target.href }) });
  });
  await page.goto(prefix);
  await expect(page.locator('.home-preview [data-active] img')).toBeVisible();
  await page.locator('.home-preview [data-active] img').evaluate((image) => image.decode());
  await page.getByRole('button', { name: 'Show Roads', exact: true }).click();
  await expect(page.locator('.home-preview [data-active] img')).toHaveAttribute('src', /macromachine_roadtool_dark-mode-basic-road-down-the-hill\.webp$/);
  await page.locator('.home-preview [data-active] img').evaluate((image) => image.decode());
  await page.locator('.feature-card[href="citylayer.html"]').click();
  await expect(page).toHaveURL(/\/MacroMachine-WpPlugin\/citylayer\.html$/);
  await expect(page.getByRole('heading', { level: 1 })).toHaveText('CityLayer guide');
  await page.locator('.guide-showcase img').scrollIntoViewIfNeeded();
  await page.locator('.guide-showcase img').evaluate((image) => image.decode());
  // Chromium cancels navigation downloads fulfilled by a route; verify bytes through the simulated mount instead.
  const sample = await page.getByRole('link', { name: 'Download peasant_house_1.schem', exact: true }).evaluate(async (link) => {
    const response = await fetch(link.href);
    return { url: response.url, ok: response.ok, length: (await response.arrayBuffer()).byteLength };
  });
  expect(sample.url).toContain('/MacroMachine-WpPlugin/downloads/peasant_house_1.schem');
  expect(sample.ok).toBe(true);
  expect(sample.length).toBeGreaterThan(0);
  await page.unrouteAll({ behavior: 'wait' });
});

test('landing screenshot keeps its proportions and does not move on hover', async ({ page }) => {
  await page.goto('/');
  const image = page.locator('.home-preview [data-hero-slide][data-active] .image-expand img');
  await expect(image).toBeVisible();
  const before = await image.evaluate((element) => ({
    ratio: element.getBoundingClientRect().width / element.getBoundingClientRect().height,
    naturalRatio: element.naturalWidth / element.naturalHeight,
    transform: getComputedStyle(element).transform,
  }));
  expect(before.ratio).toBeCloseTo(before.naturalRatio, 2);
  expect(before.transform).toBe('none');
  await image.hover();
  await expect(image).toHaveCSS('transform', 'none');
});

test('hero carousel navigates all tools and enlarges the selected image', async ({ page }) => {
  await page.goto('/');
  const hero = page.locator('.home-preview');
  const slides = hero.locator('[data-hero-slide]');
  const selectors = hero.locator('[data-hero-select]');
  const details = hero.locator('[data-hero-detail]');
  await expect(selectors).toHaveCount(5);
  await expect(selectors.locator('img[src$=".svg"], svg')).toHaveCount(5);
  await expect(hero.getByRole('button', { name: 'Show Gradient Editor', exact: true })).toHaveCount(0);
  await expect(hero.getByRole('button', { name: 'Show Filters & Layers', exact: true })).toHaveCount(0);
  await expect(hero.locator('[data-hero-previous], [data-hero-next], [data-hero-play], .hero-selectors')).toHaveCount(0);
  for (let i = 0; i < await selectors.count(); i++) {
    await selectors.nth(i).click();
    await expect(selectors.nth(i)).toHaveAttribute('aria-pressed', 'true');
    await expect(slides.nth(i)).toHaveAttribute('data-active', '');
    await expect(slides.nth(i)).toHaveAttribute('aria-hidden', 'false');
    await expect(details.nth(i)).toBeVisible();
    await expect(hero.locator('[data-hero-detail]:visible')).toHaveCount(1);
    await expect(details.nth(i).locator('.hero-slide-link a')).toHaveAttribute('href', /\.html$/);
    await expect(selectors.nth(i)).toHaveAttribute('aria-label', `Show ${await details.nth(i).locator('.hero-slide-link a').textContent()}`);
    await expect(slides.nth(i)).toHaveAttribute('aria-describedby', await details.nth(i).locator('.hero-media-caption').getAttribute('id'));
    await expect(selectors.nth(i)).toHaveText('');
    const image = slides.nth(i).locator('.image-expand img');
    await image.evaluate((element) => element.decode());
    const proportions = await image.evaluate((element) => ({
      rendered: element.getBoundingClientRect().width / element.getBoundingClientRect().height,
      natural: element.naturalWidth / element.naturalHeight,
    }));
    expect(proportions.rendered).toBeCloseTo(proportions.natural, 2);
  }
  const image = slides.last().locator('.image-expand');
  const src = await image.getAttribute('data-image');
  const alt = await image.locator('img').getAttribute('alt');
  await image.click();
  await expect(page.locator('.dialog-image')).toHaveAttribute('src', src);
  await expect(page.locator('.dialog-image')).toHaveAttribute('alt', alt);
  await page.keyboard.press('Escape');
  await expect(image).toBeFocused();
});

test('hero stays still and native scrolling synchronizes the selected icon', async ({ page }) => {
  await page.clock.install();
  await page.goto('/');
  const hero = page.locator('.home-preview');
  const slides = hero.locator('[data-hero-slide]');
  await page.clock.fastForward(16000);
  await expect(slides.first()).toHaveAttribute('data-active', '');
  await expect(hero.getByRole('status')).toBeEmpty();
  await hero.locator('[data-hero-track]').evaluate((track) => { track.scrollLeft = track.clientWidth; });
  await expect(slides.nth(1)).toHaveAttribute('data-active', '');
  await expect(hero.locator('[data-hero-select]').nth(1)).toHaveAttribute('aria-pressed', 'true');
  await expect(hero.locator('[data-hero-detail]').nth(1)).toBeVisible();
  await expect(hero.getByRole('status')).toHaveText(await slides.nth(1).getAttribute('aria-label'));
  await page.setViewportSize({ width: 800, height: 900 });
  await expect(slides.nth(1)).toHaveAttribute('data-active', '');
  await expect(hero.locator('[data-hero-detail]').nth(1)).toBeVisible();
});

test('hero icon controls have hover, selected and focus states', async ({ page }) => {
  await page.goto('/');
  const hero = page.locator('.home-preview');
  const button = hero.locator('[data-hero-select]').nth(1);
  const before = await button.evaluate((element) => getComputedStyle(element).borderColor);
  await button.hover();
  expect(await button.evaluate((element) => getComputedStyle(element).borderColor)).not.toBe(before);
  await button.focus();
  await expect(button).toHaveCSS('outline-style', 'solid');
  await page.keyboard.press('Enter');
  await expect(button).toHaveAttribute('aria-pressed', 'true');
});

test('hero is media-first with a centered dock and separate tool context', async ({ page }, testInfo) => {
  await page.goto('/');
  const hero = page.locator('.home-preview');
  const track = await hero.locator('[data-hero-track]').boundingBox();
  const controls = await hero.locator('[data-hero-controls]').boundingBox();
  const details = await hero.locator('.hero-details').boundingBox();
  const preview = await hero.boundingBox();
  expect(controls.y).toBeGreaterThanOrEqual(track.y + track.height);
  expect(details.y).toBeGreaterThan(controls.y + controls.height);
  expect(Math.abs(controls.x + controls.width / 2 - (preview.x + preview.width / 2))).toBeLessThan(1);
  await expect(hero).toHaveCSS('border-top-width', '0px');
  await expect(hero.locator('[data-hero-slide] .hero-slide-link')).toHaveCount(0);
  if (testInfo.project.name === 'desktop') {
    const copy = await page.locator('.hero-copy').boundingBox();
    expect(preview.width).toBeGreaterThan(copy.width * 1.5);
  }
});

for (const [name, slug, path] of [
  ['3D Preview', '3d-preview', 'tutorial/macromachine_3d_preview_mountain-and-jungle.gif'],
  ['CityLayer', 'citylayer', 'tutorial/CityLayer/macromachine_citylayer-dark-mode-house-fresh-start.webp'],
  ['Roads', 'paths', 'tutorial/macromachine_roadtool_dark-mode-basic-road-down-the-hill.webp'],
  ['Macros', 'macros', 'tutorial/macromachine_macros_paint-slopes-alpine.gif'],
]) {
  test(`${name} presents the GIF in the carousel, lightbox, and on top of its guide`, async ({ page }) => {
    await page.goto('/');
    const src = `assets/images/${path}`;
    await page.getByRole('button', { name: `Show ${name}`, exact: true }).click();
    const image = page.locator('.home-preview [data-hero-slide][data-active] img');
    await expect(image).toHaveAttribute('src', src);
    await image.evaluate((element) => element.decode());
    await expect(image).toHaveJSProperty('complete', true);
    expect(await image.evaluate((element) => element.naturalWidth)).toBeGreaterThan(0);
    await page.locator('.home-preview [data-hero-slide][data-active] .image-expand').click();
    await expect(page.locator('.dialog-image')).toHaveAttribute('src', src);
    await page.keyboard.press('Escape');
    await page.goto(`/${slug}.html`);
    await expect(page.locator('.guide-showcase img')).toHaveAttribute('src', src);
    await page.locator('.guide-showcase img').scrollIntoViewIfNeeded();
    await page.locator('.guide-showcase img').evaluate((element) => element.decode());
  });
}

test('roads and rivers select different presentations of the Path Tool', async ({ page }) => {
  await page.goto('/');
  const hero = page.locator('.home-preview');
  await hero.getByRole('button', { name: 'Show Roads', exact: true }).click();
  await expect(hero.locator('[data-hero-detail]:visible .hero-slide-link a')).toHaveAttribute('href', 'paths.html');
  await expect(hero.locator('[data-hero-slide][data-active] img')).toHaveAttribute('src', /macromachine_roadtool_dark-mode-basic-road-down-the-hill\.webp$/);
  await hero.getByRole('button', { name: 'Show Rivers', exact: true }).click();
  await expect(hero.getByRole('button', { name: 'Show Rivers', exact: true })).toHaveAttribute('aria-pressed', 'true');
  await expect(hero.locator('[data-hero-detail]:visible .hero-slide-link a')).toHaveAttribute('href', 'paths.html');
  const riverSrc = 'assets/images/tutorial/macromachine_rivertool_dark-mode-super-basic-river.webp';
  const riverImage = hero.locator('[data-hero-slide][data-active] img');
  await expect(riverImage).toHaveAttribute('src', riverSrc);
  await riverImage.evaluate((element) => element.decode());
  await expect(hero.locator('[data-hero-detail]:visible .hero-media-caption')).toContainText('Drawing a simple river with the Path Tool');
  await hero.locator('[data-hero-slide][data-active] .image-expand').click();
  await expect(page.locator('.dialog-image')).toHaveAttribute('src', riverSrc);
  await page.keyboard.press('Escape');
  await page.goto('/paths.html');
  const riverSection = page.locator('.feature-story').filter({ has: page.getByRole('heading', { name: 'Keep rivers downhill.', exact: true }) });
  await expect(riverSection.locator('img')).toHaveAttribute('src', riverSrc);
  await riverSection.locator('img').scrollIntoViewIfNeeded();
  await riverSection.locator('img').evaluate((element) => element.decode());
});

test('reduced motion navigates instantly without automatic advance', async ({ page }) => {
  await page.emulateMedia({ reducedMotion: 'reduce' });
  await page.clock.install();
  await page.goto('/');
  await page.clock.fastForward(16000);
  await expect(page.locator('.home-preview [data-hero-slide]').first()).toHaveAttribute('data-active', '');
  await page.getByRole('button', { name: 'Show Macros' }).click();
  await expect(page.getByRole('button', { name: 'Show Macros' })).toHaveAttribute('aria-pressed', 'true');
});

test('review pages render at the current viewport', async ({ page }, testInfo) => {
  for (const slug of ['index', 'citylayer', '3d-preview', 'macros', 'paths', 'gradient-editor', 'filters', 'get-started', 'documentation']) {
    await page.goto(`/${slug}.html`);
    await page.locator('img').evaluateAll(async (images) => {
      await Promise.all(images.map((image) => {
        image.loading = 'eager';
        return image.decode().catch(() => {});
      }));
    });
    if (slug === 'citylayer') await expect(page.locator('.feature-story.with-media').first()).toBeVisible();
    if (slug === '3d-preview') await expect(page.locator('.feature-story.text-story').first()).toBeVisible();
    await page.screenshot({ path: testInfo.outputPath(`${slug}.png`), fullPage: true });
    if (await page.locator('.guide-intro').count()) {
      await page.locator('.guide-intro').screenshot({ path: testInfo.outputPath(`${slug}-intro.png`) });
    }
    if (slug === 'documentation') {
      await page.locator('#reference').screenshot({ path: testInfo.outputPath('documentation-guides.png') });
    }
    if (slug === 'index') {
      await page.locator('.home-preview').screenshot({ path: testInfo.outputPath('hero-carousel.png') });
      await page.locator('.hero-copy').screenshot({ path: testInfo.outputPath('intro-detail.png') });
      await page.locator('.intro-facts').screenshot({ path: testInfo.outputPath('facts-detail.png') });
    }
    if (slug === 'paths') {
      for (const id of ['road-tutorial', 'river-tutorial', 'height-profile', 'river-settings', 'controls']) {
        await page.locator(`#${id}`).screenshot({ path: testInfo.outputPath(`paths-${id}.png`) });
      }
    }
  }
});

test('mobile menu opens, follows links, and closes with Escape', async ({ page }, testInfo) => {
  test.skip(testInfo.project.name !== 'mobile', 'Mobile navigation only');
  await page.goto('/');
  const menu = page.getByRole('button', { name: 'Menu' });
  await menu.click();
  await expect(menu).toHaveAttribute('aria-expanded', 'true');
  await page.keyboard.press('Escape');
  await expect(menu).toHaveAttribute('aria-expanded', 'false');
  await menu.click();
  await page.locator('#main-nav').getByRole('link', { name: 'Get started', exact: true }).click();
  await expect(page).toHaveURL(/get-started\.html$/);
  await expect(page.getByRole('button', { name: 'Menu' })).toHaveAttribute('aria-expanded', 'false');
});

test('landing page is usable without JavaScript and respects reduced motion', async ({ browser }, testInfo) => {
  const context = await browser.newContext({ javaScriptEnabled: false, reducedMotion: 'reduce', viewport: testInfo.project.use.viewport });
  const page = await context.newPage();
  await page.goto('http://127.0.0.1:4173/');
  await expect(page.locator('.home-preview [data-hero-detail]:visible a')).toHaveAttribute('href', '3d-preview.html');
  await expect(page.locator('[data-hero-controls]')).toBeHidden();
  await page.locator('.feature-card[href="citylayer.html"]').click();
  await expect(page.getByRole('heading', { level: 1 })).toHaveText('CityLayer guide');
  await context.close();
});

test('responsive layouts fit narrow phones, breakpoint edges, tablets, and wide screens', async ({ page }, testInfo) => {
  const widths = testInfo.project.name === 'mobile' ? [320, 600, 601, 640] : [768, 1024, 1920];
  for (const width of widths) {
    await page.setViewportSize({ width, height: 1000 });
    for (const slug of pages) {
      await page.goto(`/${slug}.html`);
      expect(await page.evaluate(() => document.documentElement.scrollWidth <= innerWidth), `${slug} at ${width}px`).toBe(true);
      if (slug === 'index') {
        const overflowing = await page.locator('.fact-list li, .intro-context section').evaluateAll((cards) => cards.filter((card) => card.scrollWidth > card.clientWidth).length);
        expect(overflowing, `fact cards at ${width}px`).toBe(0);
        const dock = await page.locator('[data-hero-controls]').evaluate((element) => {
          const frame = element.getBoundingClientRect();
          const buttons = [...element.querySelectorAll('button')].map((button) => button.getBoundingClientRect());
          return {
            overflowing: element.scrollWidth > element.clientWidth,
            buttonWidths: buttons.map((button) => button.width),
            enclosed: buttons.map((button) => button.left > frame.left && button.right < frame.right && button.top > frame.top && button.bottom < frame.bottom),
          };
        });
        expect(dock.overflowing, `carousel controls at ${width}px`).toBe(false);
        expect(dock.buttonWidths.every((buttonWidth) => buttonWidth >= 40)).toBe(true);
        expect(dock.enclosed, `all five icons inside the dock border at ${width}px`).toEqual(Array(5).fill(true));
        if (width === 320) {
          await page.locator('.intro-facts').screenshot({ path: testInfo.outputPath('facts-320.png') });
          await page.locator('.home-preview').screenshot({ path: testInfo.outputPath('hero-320.png') });
        }
      }
      if (slug === 'paths' && width === 320) {
        await page.locator('#road-tutorial img').evaluate(async (image) => {
          image.loading = 'eager';
          await image.decode();
        });
        await page.locator('#road-tutorial').screenshot({ path: testInfo.outputPath('paths-road-320.png') });
        await page.locator('#river-settings').screenshot({ path: testInfo.outputPath('paths-settings-320.png') });
      }
    }
  }
  await page.setViewportSize(testInfo.project.use.viewport);
  await page.goto('/');
  await page.screenshot({ path: testInfo.outputPath('landing.png'), fullPage: true });
});
