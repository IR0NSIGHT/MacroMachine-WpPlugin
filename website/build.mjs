import { cp, mkdir, readFile, readdir, rm, writeFile } from 'node:fs/promises';
import { dirname, join, resolve } from 'node:path';
import { fileURLToPath } from 'node:url';
import { features, repository, playlist, mountain } from './src/content.mjs';
import { renderHome, renderGuide, renderStart, renderDocumentation } from './src/templates.mjs';
import { getInstallerScan } from './src/virustotal.mjs';

export const root = dirname(fileURLToPath(import.meta.url));
export const output = join(root, 'dist');
export const imageRoot = join(root, '..', 'imgs');

/** @typedef {{worldPainterVersion: string, javaVersion: string}} Compatibility */
/** @param {string} pom @returns {Compatibility} */
export function readCompatibility(pom) {
  const worldPainterVersion = pom.match(/<worldpainter\.version>\s*([^<]+?)\s*<\/worldpainter\.version>/)?.[1];
  const javaVersion = pom.match(/<maven\.compiler\.release>\s*(\d+)\s*<\/maven\.compiler\.release>/)?.[1];
  if (!worldPainterVersion || !javaVersion) throw new Error('Website requirements need worldpainter.version and maven.compiler.release in pom.xml');
  return { worldPainterVersion, javaVersion };
}

export async function listImages(directory = imageRoot, prefix = '') {
  const entries = await readdir(directory, { withFileTypes: true });
  const result = await Promise.all(entries.map(async (entry) => {
    const relative = prefix + entry.name;
    if (entry.isDirectory()) return listImages(join(directory, entry.name), relative + '/');
    return /\.(png|jpe?g|svg|gif|webp|avif)$/i.test(entry.name) ? [relative] : [];
  }));
  return result.flat().sort();
}

/** @param {{installerScan?: import('./src/virustotal.mjs').InstallerScan | null}} [options] */
export async function build({ installerScan } = {}) {
  await mkdir(output, { recursive: true });
  // Remove only this retired, generated page so incremental builds cannot publish the old gallery.
  await rm(join(output, 'gallery.html'), { force: true });
  const images = await listImages();
  for (const image of images) {
    const destination = join(output, 'assets', 'images', image);
    await mkdir(dirname(destination), { recursive: true });
    await cp(join(imageRoot, image), destination);
  }
  await cp(join(root, 'public'), output, { recursive: true });
  const compatibility = readCompatibility(await readFile(join(root, '../pom.xml'), 'utf8'));
  const scan = installerScan === undefined ? await getInstallerScan() : installerScan;
  if (installerScan === undefined && process.env.VIRUSTOTAL_API_KEY && !scan) console.warn('VirusTotal results unavailable; showing the report link without scan counts.');
  const context = { features, repository, playlist, mountain, images, compatibility, installerScan: scan };
  const pages = [
    ['index.html', renderHome(context)],
    ['get-started.html', renderStart(context)],
    ['documentation.html', renderDocumentation(context)],
    ...features.map((feature) => [`${feature.slug}.html`, renderGuide(feature, context)]),
  ];
  await Promise.all(pages.map(([name, html]) => writeFile(join(output, name), html)));
  await writeFile(join(output, 'media-manifest.json'), JSON.stringify(images, null, 2) + '\n');
  console.log(`Built ${pages.length} static pages with all ${images.length} web-ready project images → website/dist`);
  return { pages: pages.map(([name]) => name), images };
}

if (process.argv[1] && fileURLToPath(import.meta.url) === resolve(process.argv[1])) {
  await build();
}
