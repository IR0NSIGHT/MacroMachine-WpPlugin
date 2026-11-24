import { cp, mkdir, readdir, writeFile } from 'node:fs/promises';
import { dirname, join, resolve } from 'node:path';
import { fileURLToPath } from 'node:url';
import { features, repository, playlist, mountain } from './src/content.mjs';
import { renderHome, renderFeature, renderGallery, renderStart } from './src/templates.mjs';

export const root = dirname(fileURLToPath(import.meta.url));
export const output = join(root, 'dist');
export const imageRoot = join(root, '..', 'imgs');

export async function listImages(directory = imageRoot, prefix = '') {
  const entries = await readdir(directory, { withFileTypes: true });
  const result = await Promise.all(entries.map(async (entry) => {
    const relative = prefix + entry.name;
    if (entry.isDirectory()) return listImages(join(directory, entry.name), relative + '/');
    return /\.(png|jpe?g|svg|gif|webp|avif)$/i.test(entry.name) ? [relative] : [];
  }));
  return result.flat().sort();
}

export async function build() {
  await mkdir(output, { recursive: true });
  const images = await listImages();
  for (const image of images) {
    const destination = join(output, 'assets', 'images', image);
    await mkdir(dirname(destination), { recursive: true });
    await cp(join(imageRoot, image), destination);
  }
  await cp(join(root, 'public'), output, { recursive: true });
  const context = { features, repository, playlist, mountain, images };
  const pages = [
    ['index.html', renderHome(context)],
    ['gallery.html', renderGallery(context)],
    ['get-started.html', renderStart(context)],
    ...features.map((feature) => [`${feature.slug}.html`, renderFeature(feature, context)]),
  ];
  await Promise.all(pages.map(([name, html]) => writeFile(join(output, name), html)));
  await writeFile(join(output, 'media-manifest.json'), JSON.stringify(images, null, 2) + '\n');
  console.log(`Built ${pages.length} static pages with all ${images.length} web-ready project images → website/dist`);
  return { pages: pages.map(([name]) => name), images };
}

if (process.argv[1] && fileURLToPath(import.meta.url) === resolve(process.argv[1])) {
  await build();
}
