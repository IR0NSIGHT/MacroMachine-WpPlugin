import { repository, playlist, projectFacts } from './content.mjs';

export const escape = (value) => String(value).replaceAll('&', '&amp;').replaceAll('<', '&lt;').replaceAll('>', '&gt;').replaceAll('"', '&quot;').replaceAll("'", '&#39;');
export const imageUrl = (src) => 'assets/images/' + src.split('/').map(encodeURIComponent).join('/');
export const documentationImages = (images) => images.filter((src) => src.startsWith('tutorial/') && !src.toLowerCase().endsWith('.svg'));

function header(active, features) {
  return `<a class="skip-link" href="#main">Skip to content</a>
  <header class="site-header"><div class="wrap header-inner">
    <a class="brand" href="index.html" aria-label="MacroMachine home"><span class="brand-icon"><img src="${imageUrl('icon.svg')}" width="34" height="34" alt=""></span><span>MacroMachine<span class="brand-sub">FOR WORLDPAINTER</span></span></a>
    <button class="menu-toggle" type="button" aria-expanded="false" aria-controls="main-nav">Menu <span aria-hidden="true">☰</span></button>
    <nav class="main-nav" id="main-nav" aria-label="Main navigation">
      <a href="index.html#tools" ${features.some((feature) => feature.slug === active) ? 'class="nav-active"' : ''}>Tools</a>
      <a href="gallery.html" ${active === 'gallery' ? 'aria-current="page"' : ''}>In action</a>
      <a href="get-started.html" ${active === 'get-started' ? 'aria-current="page"' : ''}>Get started</a>
      <a href="${repository}">GitHub</a>
      <a href="get-started.html#download">Download</a>
    </nav>
  </div></header>`;
}

function footer(features) {
  return `<footer class="site-footer"><div class="wrap">
    <div class="footer-top"><div><a class="brand" href="index.html"><span class="brand-icon"><img src="${imageUrl('MacroMachine_Icon.png')}" width="34" height="34" alt=""></span><span>MacroMachine</span></a><p>Free and open source. No account required.<br>Community-supported WorldPainter plugin.</p></div>
    <div><h2>Explore the tools</h2>${features.map((f) => `<a href="${f.slug}.html">${escape(f.name)}</a>`).join('')}</div>
    <div><h2>Help & resources</h2><a href="get-started.html">Getting started</a><a href="gallery.html">Image gallery</a><a href="${playlist}">Video tutorials</a><a href="https://discord.gg/9kRJEMfGeE">Discord community</a><a href="${repository}/issues">Report an issue</a></div></div>
    <div class="footer-bottom"><span>A project by <a href="${repository}">IR0NSIGHT</a> & contributors.</span><span>A WorldPainter plugin for Minecraft map editing.</span></div>
    <p class="disclaimer">MacroMachine is an independent WorldPainter plugin. Not affiliated with Mojang or Microsoft.</p>
  </div></footer>`;
}

function overlays() {
  return `<dialog class="image-dialog" aria-labelledby="image-title"><div class="dialog-top"><p id="image-title">Image preview</p><button type="button" class="dialog-close" aria-label="Close image preview">Close <span aria-hidden="true">×</span></button></div><img class="dialog-image" alt=""><p class="dialog-caption"></p></dialog>`;
}

function layout({ title, description, active = 'home', body, features, image = 'MacroMachineBanner.png' }) {
  return `<!doctype html>
<html lang="en"><head>
  <meta charset="UTF-8"><meta name="viewport" content="width=device-width, initial-scale=1">
  <meta name="theme-color" content="#161616"><meta name="description" content="${escape(description)}">
  <meta property="og:type" content="website"><meta property="og:title" content="${escape(title)}"><meta property="og:description" content="${escape(description)}"><meta property="og:image" content="${imageUrl(image)}">
  <title>${escape(title)}</title><link rel="icon" href="${imageUrl('icon.svg')}" type="image/svg+xml"><link rel="stylesheet" href="assets/styles.css"><script src="assets/app.js" defer></script>
</head><body class="page-${active}">${header(active, features)}<main id="main">${body}</main>${footer(features)}${overlays()}</body></html>`;
}

function picture(media, { eager = false, className = '' } = {}) {
  return `<figure class="screenshot ${className}"><button class="image-expand" type="button" data-image="${imageUrl(media.src)}" data-caption="${escape(media.caption)}" aria-label="Enlarge: ${escape(media.alt)}"><img src="${imageUrl(media.src)}" alt="${escape(media.alt)}" loading="${eager ? 'eager' : 'lazy'}" decoding="async"><span class="expand-mark" aria-hidden="true">↗</span></button><figcaption>${escape(media.caption)}</figcaption></figure>`;
}

function toolIcon(feature) {
  return feature.icon ? `<img class="tool-icon" src="${imageUrl(feature.icon)}" width="30" height="30" alt="">` : '';
}

function projectHistory() {
  return `<p class="project-history">In development since ${projectFacts.developmentSince}.</p>`;
}

function featureCard(feature) {
  return `<a class="feature-card accent-${feature.accent}" href="${feature.slug}.html">${feature.icon ? `<span class="tool-mark" aria-hidden="true">${toolIcon(feature)}</span>` : ''}<h3>${escape(feature.name)}</h3><p>${escape(feature.description)}</p></a>`;
}

/** @param {import('./content.mjs').Feature} feature @param {string} summary */
function renderToolShortcut(feature, summary) {
  return `<a class="tool-shortcut accent-${feature.accent}" data-tool="${feature.slug}" href="${feature.slug}.html"><span class="tool-shortcut-icon" aria-hidden="true">${toolIcon(feature)}</span><span class="tool-shortcut-copy"><strong class="tool-shortcut-name">${escape(feature.name)}</strong><span class="tool-shortcut-summary">${escape(summary)}</span></span></a>`;
}

export function renderHome({ features, images }) {
  const preview = features.find((f) => f.slug === '3d-preview');
  const shortcuts = [
    ['macros', 'Run saved terrain edits'],
    ['citylayer', 'Position schematics'],
    ['paths', 'Draw roads and rivers'],
    ['3d-preview', 'Live 3D export view'],
  ];
  const body = `<section class="home-hero"><div class="wrap hero-grid">
    <div class="hero-copy"><h1>MacroMachine</h1>
      <p class="hero-description">MacroMachine is a plugin that runs inside WorldPainter and fills gaps in a WorldPainter + World Machine workflow. It adds individual schematic placement, terrain-aware connected roads and rivers, reusable layer-editing macros, and live 3D feedback based on exports.</p>
      <div class="intro-download" id="download"><button class="button button-primary" type="button" disabled aria-describedby="download-note">Download MacroMachine</button><p id="download-note">Download placeholder: the installer link is not connected yet.</p><a href="get-started.html">Installation guide</a></div>
    </div>
    ${picture(preview.hero, { eager: true, className: 'home-preview' })}
  </div></section>
  <nav class="tool-strip" aria-label="Included tools"><div class="wrap tool-shortcuts">${shortcuts.map(([slug, summary]) => renderToolShortcut(features.find((feature) => feature.slug === slug), summary)).join('')}</div></nav>
  <section class="section tools-section wrap" id="tools"><div class="section-heading"><h2>Tools</h2><p>Tools that complement WorldPainter and World Machine, from layer edits and individual building placement to connected paths and live export previews.</p></div><div class="feature-grid">${features.map(featureCard).join('')}</div></section>
  <section class="showcase-section section" id="in-action"><div class="wrap"><div class="section-heading"><h2>Examples</h2><a class="text-link" href="gallery.html">View all ${documentationImages(images).length} images</a></div>
    <div class="showcase-tabs" role="tablist" aria-label="Tool demonstrations">${[preview, features[1], features[2], features[0]].map((f, index) => `<button type="button" role="tab" id="tab-${f.slug}" aria-controls="showcase-panel" aria-selected="${index === 0}" tabindex="${index === 0 ? '0' : '-1'}" data-showcase-src="${imageUrl(f.hero.src)}" data-showcase-alt="${escape(f.hero.alt)}" data-showcase-name="${escape(f.name)}" data-showcase-description="${escape(f.description)}" data-showcase-link="${f.slug}.html">${toolIcon(f)}${escape(f.name)}</button>`).join('')}</div>
    <div id="showcase-panel" class="showcase-panel" role="tabpanel" aria-labelledby="tab-3d-preview"><div class="showcase-image"><img src="${imageUrl(preview.hero.src)}" alt="${escape(preview.hero.alt)}" loading="lazy" id="showcase-image"></div><div class="showcase-caption"><div><h3 id="showcase-name">3D Preview</h3><p id="showcase-description">${escape(preview.description)}</p></div><a id="showcase-link" href="3d-preview.html">About this tool</a></div></div>
  </div></section>
  <section class="project-section wrap" id="project"><h2>About the project</h2><p>Free and open source under ${projectFacts.license}. No account, subscription, or activation key is required. <a href="${repository}">Source code</a> and <a href="${repository}/blob/master/LICENCE">license</a>.</p>${projectHistory()}<p>IR0NSIGHT started the project as a river tool; macros grew from work on decorating riverbanks.</p><p>Community-supported: ask on <a href="https://discord.gg/9kRJEMfGeE">Discord</a> or report issues on <a href="${repository}/issues">GitHub</a>.</p><p>Endorsed by WorldPainter: Vetted by WorldPainter’s developer and previously built on the official WorldPainter build server. Current builds run separately to support Java 21.</p><p>GitHub lists ${projectFacts.downloads} release downloads and ${projectFacts.releases} public releases. Downloads count release assets, not unique users. Figures checked ${projectFacts.checkedOn}. <a href="${repository}/releases">View releases</a>.</p></section>`;
  return layout({ title: 'MacroMachine — Tools for WorldPainter', description: 'A WorldPainter plugin that complements a WorldPainter + World Machine workflow with schematic placement, connected paths, reusable macros, and live 3D export previews.', body, features });
}

export function renderFeature(feature, { features }) {
  const body = `<div class="feature-subnav"><div class="wrap"><a href="index.html#tools">All tools</a><nav aria-label="Explore tools">${features.map((f) => `<a href="${f.slug}.html" ${f.slug === feature.slug ? 'aria-current="page"' : ''}>${escape(f.name)}</a>`).join('')}</nav></div></div>
  <section class="feature-hero accent-${feature.accent}"><div class="wrap"><div class="feature-heading">${feature.icon ? `<span class="tool-mark" aria-hidden="true">${toolIcon(feature)}</span>` : ''}<h1>${escape(feature.name)}</h1></div><p>${escape(feature.description)}</p>${picture(feature.hero, { eager: true, className: 'feature-hero-image' })}</div></section>
  <div class="wrap" id="workflow">${feature.notice ? `<aside class="notice"><strong>Before you build</strong><p>${escape(feature.notice)}</p></aside>` : ''}
    ${feature.sections.map((section) => `<section class="feature-story ${section.media ? 'with-media' : 'text-story'}"><div class="story-copy"><h2>${escape(section.title)}</h2><p>${escape(section.body)}</p>${section.bullets ? `<ol class="step-list">${section.bullets.map((bullet) => `<li>${escape(bullet)}</li>`).join('')}</ol>` : ''}</div>${section.media ? picture(section.media) : ''}</section>`).join('')}
  </div><section class="related-section wrap"><h2>Other tools</h2><ul class="related-links">${features.filter((f) => f.slug !== feature.slug).map((f) => `<li><a href="${f.slug}.html">${escape(f.name)}</a></li>`).join('')}</ul></section>`;
  return layout({ title: `${feature.name} — MacroMachine for WorldPainter`, description: feature.description, active: feature.slug, body, features, image: feature.hero.src });
}

function imageLabel(path) {
  const known = {
    'tutorial/macromachine_tool.PNG': 'The MacroMachine M tool in WorldPainter',
    'tutorial/main_color_texture_worldpainter.png': 'WorldPainter terrain color and texture reference',
  };
  return known[path] || path.split('/').pop().replace(/\.[^.]+$/, '').replaceAll('_', ' ');
}

function category(path) {
  if (/citylayer/i.test(path)) return 'citylayer';
  if (/PathTool/.test(path)) return 'paths';
  if (/preview/.test(path)) return 'preview';
  if (/pines|slope/.test(path)) return 'gradients';
  return 'macros';
}

export function renderGallery({ features, images }) {
  images = documentationImages(images);
  const categories = [['all', 'Everything'], ['citylayer', 'CityLayer'], ['paths', 'Paths'], ['preview', '3D Preview'], ['macros', 'Macros'], ['gradients', 'Gradients']];
  const body = `<section class="page-intro wrap"><h1>Screenshots and tutorials</h1><p>WorldPainter previews, editing examples, and animated tool walkthroughs.</p><a class="text-link" href="${playlist}">Watch the video tutorials</a></section>
  <section class="wrap gallery-section"><div class="gallery-controls" role="group" aria-label="Filter images">${categories.map(([value, label], index) => `<button class="filter-button" type="button" data-filter="${value}" aria-pressed="${index === 0}">${label}</button>`).join('')}</div><p class="gallery-count" aria-live="polite"><span id="gallery-count">${images.length}</span> images · Select an image to take a closer look</p><div class="gallery-grid">${images.map((src) => `<div class="gallery-item" data-category="${category(src)}">${picture({ src, alt: imageLabel(src), caption: imageLabel(src) })}${src.toLowerCase().endsWith('.gif') ? '<span class="gif-label">ANIMATED GIF</span>' : ''}</div>`).join('')}</div></section>`;
  return layout({ title: 'In action — MacroMachine gallery', description: 'Browse MacroMachine previews, documentation screenshots, and CityLayer walkthroughs.', active: 'gallery', body, features });
}

export function renderStart({ features }) {
  const body = `<section class="page-intro wrap"><h1>Installation</h1><p>Install MacroMachine in WorldPainter, import the ready-made macros, and run your first terrain edit.</p></section>
  <section class="wrap start-layout"><aside class="start-aside"><p>On this page</p><a href="#download">Download</a><a href="#install">Windows installation</a><a href="#first-macro">Your first macro</a><a href="#other-platforms">Mac & Linux</a><a href="#help">Need a hand?</a></aside><div class="start-content">
    <section class="download-panel" id="download"><h2>Download MacroMachine</h2><p>Free and open source under ${projectFacts.license}. No account, subscription, or activation key required.</p><button class="button button-primary" type="button" disabled aria-describedby="download-note">Download for Windows</button><p id="download-note" class="small-note">Placeholder — the installer download will be connected here.</p><p>For current files, visit the <a href="${repository}/releases/latest">official GitHub releases</a>.</p><p class="small-note">${projectFacts.releases} public releases listed on GitHub. Community support is available on <a href="https://discord.gg/9kRJEMfGeE">Discord</a> and <a href="${repository}/issues">GitHub</a>.</p>${projectHistory()}</section>
    <section id="install"><h2>Install on Windows</h2><p>The Windows installer locates WorldPainter and installs the plugin for you.</p><ol class="step-list"><li>Download the installer from the official releases page.</li><li>Save your work and close WorldPainter.</li><li>Open the installer and follow the installation prompts.</li><li>Finish setup. WorldPainter opens with MacroMachine.</li></ol><p class="small-note">Use installers from the official project only. If Windows displays a security warning, verify the source before proceeding.</p></section>
    <section id="first-macro"><h2>Import and run a macro</h2><p>Download the ready-made macros ZIP from the <a href="${repository}/releases/latest">releases page</a> and extract it.</p>${picture({ src: 'tutorial/macromachine_tool.PNG', alt: 'The M tool that opens the MacroMachine panel', caption: 'Find the M tool in WorldPainter to open MacroMachine.' })}<ol class="step-list"><li>Click the M tool to open the MacroMachine window.</li><li>Click Import, open the extracted defaultActions folder, select the macro files, and click Open.</li><li>Choose a macro. Its description explains what it does.</li><li>Press ▶ to run it on your map.</li></ol><a class="text-link" href="https://youtu.be/Q5R2G-uRatc">Watch the 100-second overview</a></section>
    <section id="other-platforms"><h2>Mac, Linux, or a manual install?</h2><p>Download the plugin JAR from the official release, install it with the WorldPainter plugin installer, then restart WorldPainter.</p><p>If you used MacroMachine 0.4.14 or older, first open Tools → Open plugins folder in WorldPainter and remove the old MacroMachine JAR. Keep only the version you intend to use.</p><a class="text-link" href="https://youtu.be/FtfwxDTzWgk">Watch the manual installation tutorial</a></section>
    <section id="help"><h2>Common problems</h2><div class="faq"><details><summary>WorldPainter is still running.</summary><p>Save your worlds, close WorldPainter, and click Retry in the installer.</p></details><details><summary>The installer cannot find WorldPainter.</summary><p>Click Browse and select the folder containing worldpainter.exe.</p></details><details><summary>Which Java version do I need?</summary><p>The installer requires Java 21 or newer. Install a current Java from <a href="https://adoptium.net/">Adoptium</a> if needed. If WorldPainter still uses an old Java, reinstall WorldPainter.</p></details><details><summary>My macro runs, but nothing changes.</summary><p>Check the log. The macro’s conditions might not exist on your map—for example, a mountain macro on flat terrain. Step through its actions with the debugger and make sure annotations are visible.</p></details><details><summary>WorldPainter stopped opening after an update.</summary><p>Check Tools → Open plugins folder for duplicate MacroMachine JARs. Mixing older and newer plugin files can cause problems. Keep only the intended version. The Windows installer handles this cleanup.</p></details><details><summary>How do I report a problem?</summary><p>Open a <a href="${repository}/issues">GitHub issue</a> or contact @IR0NSIGHT in the <a href="https://discord.gg/9kRJEMfGeE">Discord community</a>. Include the latest logfile0.txt, usually located in the parent folder of the WorldPainter plugins directory.</p></details></div></section>
  </div></section>`;
  return layout({ title: 'Get started — MacroMachine for WorldPainter', description: 'Install MacroMachine, run your first WorldPainter macro, and find help for Windows, Mac, and Linux.', active: 'get-started', body, features });
}
