import { test, expect } from '@playwright/test';
import AxeBuilder from '@axe-core/playwright';

const pages = ['index', 'citylayer', 'paths', '3d-preview', 'macros', 'gradient-editor', 'filters', 'gallery', 'get-started'];

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

test('showcase switches content and supports arrow-key navigation', async ({ page }) => {
  await page.goto('/');
  await page.getByRole('tab', { name: 'CityLayer', exact: true }).click();
  await expect(page.locator('#showcase-name')).toHaveText('CityLayer');
  await expect(page.locator('#showcase-link')).toHaveAttribute('href', 'citylayer.html');
  await page.keyboard.press('ArrowRight');
  await expect(page.getByRole('tab', { name: 'Path Tool', exact: true })).toBeFocused();
  await expect(page.locator('#showcase-name')).toHaveText('Path Tool');
});

test('icon-led tool shortcuts navigate to their feature pages', async ({ page }) => {
  await page.goto('/');
  const tools = page.getByRole('navigation', { name: 'Included tools', exact: true });
  await expect(tools.locator('.tool-shortcut')).toHaveCount(4);
  await expect(tools.locator('.tool-shortcut-icon img[src$=".svg"]')).toHaveCount(4);
  await expect(tools.locator('.tool-shortcut-image')).toHaveCount(0);
  await tools.getByRole('link', { name: 'CityLayer Position schematics', exact: true }).click();
  await expect(page).toHaveURL(/citylayer\.html$/);
});

test('gallery filtering and image dialog work with focus restoration', async ({ page }) => {
  await page.goto('/gallery.html');
  await page.getByRole('button', { name: 'CityLayer', exact: true }).click();
  await expect(page.locator('.gallery-item:visible')).toHaveCount(8);
  await expect(page.locator('#gallery-count')).toHaveText('8');
  const image = page.locator('.gallery-item:visible .image-expand').first();
  await image.click();
  await expect(page.getByRole('dialog')).toBeVisible();
  await page.keyboard.press('Escape');
  await expect(page.getByRole('dialog')).not.toBeVisible();
  await expect(image).toBeFocused();
  await page.getByRole('button', { name: 'Everything', exact: true }).click();
  const manifest = await (await page.request.get('/media-manifest.json')).json();
  await expect(page.locator('.gallery-item:visible')).toHaveCount(manifest.filter((src) => src.startsWith('tutorial/') && !src.toLowerCase().endsWith('.svg')).length);
});

test('placeholder is honest and feature links lead to real pages', async ({ page }) => {
  await page.goto('/');
  await expect(page.locator('#download button')).toBeDisabled();
  await expect(page.locator('#download-note')).toContainText('placeholder');
  await page.locator('.feature-card[href="citylayer.html"]').click();
  await expect(page).toHaveURL(/citylayer\.html$/);
  await expect(page.getByRole('heading', { level: 1 })).toHaveText('CityLayer');
});

test('landing screenshot keeps its proportions and does not move on hover', async ({ page }) => {
  await page.goto('/');
  const image = page.locator('.home-preview .image-expand img');
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

test('review pages render at the current viewport', async ({ page }, testInfo) => {
  for (const slug of ['index', 'citylayer', '3d-preview', 'gallery', 'get-started']) {
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
  await page.locator('.feature-card[href="citylayer.html"]').click();
  await expect(page.getByRole('heading', { level: 1 })).toHaveText('CityLayer');
  await context.close();
});

test('responsive layouts fit narrow phones, breakpoint edges, tablets, and wide screens', async ({ page }, testInfo) => {
  const widths = testInfo.project.name === 'mobile' ? [320, 600, 601, 640] : [768, 1024, 1920];
  for (const width of widths) {
    await page.setViewportSize({ width, height: 1000 });
    for (const slug of pages) {
      await page.goto(`/${slug}.html`);
      expect(await page.evaluate(() => document.documentElement.scrollWidth <= innerWidth), `${slug} at ${width}px`).toBe(true);
    }
  }
  await page.setViewportSize(testInfo.project.use.viewport);
  await page.goto('/');
  await page.screenshot({ path: testInfo.outputPath('landing.png'), fullPage: true });
});
