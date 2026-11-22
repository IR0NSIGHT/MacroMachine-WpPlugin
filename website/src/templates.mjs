import { repository, playlist, projectFacts } from './content.mjs';

export const escape = (value) => String(value).replaceAll('&', '&amp;').replaceAll('<', '&lt;').replaceAll('>', '&gt;').replaceAll('"', '&quot;').replaceAll("'", '&#39;');
export const imageUrl = (src) => 'assets/images/' + src.split('/').map(encodeURIComponent).join('/');
export const documentationImages = (images) => images.filter((src) => src.startsWith('tutorial/') && !src.toLowerCase().endsWith('.svg'));
const arrow = '<span aria-hidden="true">↗</span>';
const forward = '<span aria-hidden="true">→</span>';

function header(active, features) {
  return `<a class="skip-link" href="#main">Skip to content</a>
  <header class="site-header"><div class="wrap header-inner">
    <a class="brand" href="index.html" aria-label="MacroMachine home"><span class="brand-icon"><img src="${imageUrl('icon.svg')}" width="34" height="34" alt=""></span><span>MacroMachine<span class="brand-sub">FOR WORLDPAINTER</span></span></a>
    <button class="menu-toggle" type="button" aria-expanded="false" aria-controls="main-nav">Menu <span aria-hidden="true">☰</span></button>
    <nav class="main-nav" id="main-nav" aria-label="Main navigation">
      <a href="index.html#tools" ${features.some((feature) => feature.slug === active) ? 'class="nav-active"' : ''}>Tools</a>
      <a href="gallery.html" ${active === 'gallery' ? 'aria-current="page"' : ''}>In action</a>
      <a href="get-started.html" ${active === 'get-started' ? 'aria-current="page"' : ''}>Get started</a>
      <a href="${repository}">GitHub ${arrow}</a>
      <a class="button button-small button-outline" href="get-started.html#download">Get MacroMachine ${forward}</a>
    </nav>
  </div></header>`;
}

function footer(features) {
  return `<footer class="site-footer"><div class="wrap">
    <div class="footer-top"><div><a class="brand" href="index.html"><span class="brand-icon"><img src="${imageUrl('MacroMachine_Icon.png')}" width="34" height="34" alt=""></span><span>MacroMachine</span></a><p>Free and open source. No account required.<br>Community-supported WorldPainter plugin.</p></div>
    <div><h2>Explore the tools</h2>${features.map((f) => `<a href="${f.slug}.html">${escape(f.name)}</a>`).join('')}</div>
    <div><h2>Help & resources</h2><a href="get-started.html">Getting started</a><a href="gallery.html">Image gallery</a><a href="${playlist}">Video tutorials ${arrow}</a><a href="https://discord.gg/9kRJEMfGeE">Discord community ${arrow}</a><a href="${repository}/issues">Report an issue ${arrow}</a></div></div>
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

function cta() {
  return `<section class="closing-cta"><div class="wrap cta-inner"><p class="eyebrow">INSTALLATION & TUTORIALS</p><h2>Install the plugin.<br>Try a macro.</h2><div class="button-row"><a class="button button-light" href="get-started.html">Installation guide ${forward}</a><a class="text-link" href="${repository}">View the source ${arrow}</a></div><span class="cta-orbit" aria-hidden="true"></span></div></section>`;
}

function toolIcon(feature) {
  return feature.icon ? `<img class="tool-icon" src="${imageUrl(feature.icon)}" width="30" height="30" alt="">` : '';
}

function projectHistory() {
  return `<p class="project-history">In development since ${projectFacts.developmentSince}.</p>`;
}

function featureCard(feature) {
  return `<a class="feature-card accent-${feature.accent}" href="${feature.slug}.html"><div class="card-top"><span class="tool-mark" aria-hidden="true">${toolIcon(feature) || feature.number}</span><span class="card-arrow" aria-hidden="true">↗</span></div><p class="eyebrow">${escape(feature.tag)}</p><h3>${escape(feature.name)}</h3><p>${escape(feature.description)}</p><span class="card-link">About ${escape(feature.name)} ${forward}</span></a>`;
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
    <div class="hero-copy"><p class="eyebrow"><span class="status-dot"></span> MACROMACHINE FOR WORLDPAINTER</p>
      <h1>Automate edits.<br><span class="gradient-text">Place buildings.</span></h1>
      <p class="hero-description">Run terrain macros, place individual schematics, draw roads and rivers, and see the export result in 3D as you edit.</p>
      <div class="button-row"><a class="button button-primary" href="#download">Download MacroMachine <span aria-hidden="true">↓</span></a><a class="text-link" href="#tools">Browse the tools ${forward}</a></div>
      <p class="hero-note">Free and open source · No account required</p>
    </div>
    <div class="hero-art" aria-label="A glimpse of MacroMachine’s 3D terrain preview"><div class="art-ribbon ribbon-one"></div><div class="art-ribbon ribbon-two"></div><div class="art-dots"></div>
      <div class="world-window"><div class="window-chrome"><span class="window-dots" aria-hidden="true">● ● ●</span><span>your-world / 3D Preview</span><span aria-hidden="true">↗</span></div><div class="world-crop"><img src="${imageUrl(preview.hero.src)}" alt="Minecraft terrain with a village, forests, and fields in the 3D export preview" fetchpriority="high" width="1695" height="553"></div><div class="window-bottom"><span><i class="status-dot"></i> Selected-area test export</span><span>3D PREVIEW</span></div></div>
      <a class="floating-label" href="citylayer.html"><span class="label-icon" aria-hidden="true">⌂</span><span>CityLayer<br><strong>Place and move schematics.</strong></span>${arrow}</a>
      <span class="art-coordinate" aria-hidden="true">TERRAIN · SCHEMATICS · LAYERS</span>
    </div>
  </div></section>
  <nav class="tool-strip" aria-label="Included tools"><div class="wrap tool-shortcuts">${shortcuts.map(([slug, summary]) => renderToolShortcut(features.find((feature) => feature.slug === slug), summary)).join('')}</div></nav>
  <section class="project-section wrap" id="project"><div class="project-intro"><p class="eyebrow">ABOUT THE PROJECT</p><h2>Free to use.<br>Open source.</h2><p>No account, subscription, or activation key. Download the plugin and use it in WorldPainter. The source is published under ${projectFacts.license}.</p><div class="button-row"><a class="text-link" href="${repository}">Read the source ${arrow}</a><a class="text-link" href="${repository}/blob/master/LICENCE">View the license ${arrow}</a></div>${projectHistory()}</div><div class="project-facts"><a class="project-stat" href="${repository}/releases"><strong>${projectFacts.downloads}</strong><span>Release downloads</span><small>GitHub release assets</small></a><a class="project-stat" href="${repository}/releases"><strong>${projectFacts.releases}</strong><span>Public releases</span><small>Current GitHub release list</small></a><div class="project-detail"><h3>Community-supported</h3><p>Get help on <a href="https://discord.gg/9kRJEMfGeE">Discord</a> and report bugs on <a href="${repository}/issues">GitHub</a>.</p></div><div class="project-detail"><h3>Endorsed by WorldPainter</h3><p>Vetted by WorldPainter’s developer and previously built on the official WorldPainter build server. Current builds run separately to support Java 21.</p></div><p class="project-stats-note">GitHub release figures checked ${projectFacts.checkedOn}. Downloads count release assets, not unique users.</p></div></section>
  <section class="section tools-section wrap" id="tools"><div class="section-heading"><div><p class="eyebrow">TOOLS & FEATURES</p><h2>What the<br>plugin adds.</h2></div><p>Automate terrain edits, position buildings,<br>and check the result before export.</p></div><div class="feature-grid">${features.map(featureCard).join('')}</div></section>
  <section class="showcase-section section" id="in-action"><div class="wrap"><div class="section-heading"><div><p class="eyebrow">SCREENSHOTS FROM WORLDPAINTER</p><h2>See the tools<br>in use.</h2></div><a class="text-link" href="gallery.html">View all ${documentationImages(images).length} images ${forward}</a></div>
    <div class="showcase-tabs" role="tablist" aria-label="Tool demonstrations">${[preview, features[1], features[2], features[0]].map((f, index) => `<button type="button" role="tab" id="tab-${f.slug}" aria-controls="showcase-panel" aria-selected="${index === 0}" tabindex="${index === 0 ? '0' : '-1'}" data-showcase-src="${imageUrl(f.hero.src)}" data-showcase-alt="${escape(f.hero.alt)}" data-showcase-name="${escape(f.name)}" data-showcase-description="${escape(f.description)}" data-showcase-link="${f.slug}.html">${toolIcon(f)}${escape(f.name)}</button>`).join('')}</div>
    <div id="showcase-panel" class="showcase-panel" role="tabpanel" aria-labelledby="tab-3d-preview"><div class="showcase-image"><img src="${imageUrl(preview.hero.src)}" alt="${escape(preview.hero.alt)}" loading="lazy" id="showcase-image"></div><div class="showcase-caption"><div><h3 id="showcase-name">3D Preview</h3><p id="showcase-description">${escape(preview.description)}</p></div><a id="showcase-link" class="button button-outline" href="3d-preview.html">Explore the tool ${arrow}</a></div></div>
  </div></section>
  <section class="section wrap split-section"><div><p class="eyebrow">USING THE PLUGIN</p><h2>Works inside<br><span class="muted">WorldPainter.</span></h2><p class="body-large">Use your existing terrain, custom layers, and schematic files. Open the tools from WorldPainter’s toolbar.</p><a class="text-link" href="get-started.html">Read the setup guide ${forward}</a></div><div class="workflow-list"><article><span>01 /</span><div><h3>Use your existing layers and schematics.</h3><p>Macros can read and write custom layers. CityLayer places buildings from your schematic collection.</p></div></article><article><span>02 /</span><div><h3>Save a sequence of edits as a macro.</h3><p>Run actions in order across your map, with filters to exclude blocks you do not want to change.</p></div></article><article><span>03 /</span><div><h3>See the export result while you edit.</h3><p>Keep the live 3D window beside WorldPainter. Auto Update shows changes without manually exporting and reloading Minecraft each time.</p></div></article></div></section>
  <section class="download-section wrap" id="download"><div><p class="eyebrow">DOWNLOAD & INSTALLATION</p><h2>Install MacroMachine<br>for WorldPainter.</h2><p>Free to use. No account or activation key required.</p></div><div class="download-action"><button class="button button-light" type="button" disabled aria-describedby="download-note">Download MacroMachine <span aria-hidden="true">↓</span></button><p id="download-note">Download placeholder · Installer link coming soon.</p><a class="text-link" href="get-started.html">Installation guide ${forward}</a></div></section>
  <section class="community-section wrap"><p>Tutorials, questions, and bug reports.</p><div><a href="https://youtu.be/Q5R2G-uRatc">The 100-second overview ${arrow}</a><a href="https://discord.gg/9kRJEMfGeE">Ask on Discord ${arrow}</a><a href="${repository}">View on GitHub ${arrow}</a></div></section>`;
  return layout({ title: 'MacroMachine — Tools for WorldPainter', description: 'Run terrain macros, place individual schematics with CityLayer, draw roads and rivers, and preview selected areas in 3D before exporting from WorldPainter.', body, features });
}

export function renderFeature(feature, { features }) {
  const body = `<div class="feature-subnav"><div class="wrap"><a href="index.html#tools">All tools</a><nav aria-label="Explore tools">${features.map((f) => `<a href="${f.slug}.html" ${f.slug === feature.slug ? 'aria-current="page"' : ''}>${escape(f.name)}</a>`).join('')}</nav></div></div>
  <section class="feature-hero accent-${feature.accent}"><div class="wrap"><div class="feature-heading">${feature.icon ? `<span class="tool-mark" aria-hidden="true">${toolIcon(feature)}</span>` : ''}<p class="eyebrow">${escape(feature.name.toUpperCase())} / ${feature.tag}</p></div><h1>${escape(feature.title).replace('\n', '<br>')}</h1><div class="feature-hero-bottom"><p>${escape(feature.description)}</p><a class="button button-primary" href="#workflow">How to use it <span aria-hidden="true">↓</span></a></div><div class="benefits">${feature.benefits.map((benefit) => `<span><span aria-hidden="true">✓</span> ${escape(benefit)}</span>`).join('')}</div>${picture(feature.hero, { eager: true, className: 'feature-hero-image' })}</div></section>
  <div class="wrap" id="workflow">${feature.notice ? `<aside class="notice"><strong>Before you build</strong><p>${escape(feature.notice)}</p></aside>` : ''}
    ${feature.sections.map((section, index) => `<section class="feature-story ${section.media ? 'with-media' : 'text-story'}"><div class="story-copy"><p class="eyebrow">${String(index + 1).padStart(2, '0')} / ${escape(feature.name.toUpperCase())}</p><h2>${escape(section.title)}</h2><p>${escape(section.body)}</p>${section.bullets ? `<ol class="step-list">${section.bullets.map((bullet) => `<li>${escape(bullet)}</li>`).join('')}</ol>` : ''}</div>${section.media ? picture(section.media) : ''}</section>`).join('')}
  </div><section class="related-section section"><div class="wrap"><p class="eyebrow">OTHER TOOLS</p><h2>Also in MacroMachine.</h2><div class="related-grid">${features.filter((f) => f.slug !== feature.slug).slice(0, 3).map(featureCard).join('')}</div></div></section>${cta()}`;
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
  const body = `<section class="page-intro wrap"><p class="eyebrow">IMAGE GALLERY</p><h1>Screenshots<br><span class="gradient-text">and tutorials.</span></h1><p>WorldPainter previews, editing examples, and animated tool walkthroughs.</p><a class="text-link" href="${playlist}">Watch the video tutorials ${arrow}</a></section>
  <section class="wrap gallery-section"><div class="gallery-controls" role="group" aria-label="Filter images">${categories.map(([value, label], index) => `<button class="filter-button" type="button" data-filter="${value}" aria-pressed="${index === 0}">${label}</button>`).join('')}</div><p class="gallery-count" aria-live="polite"><span id="gallery-count">${images.length}</span> images · Select an image to take a closer look</p><div class="gallery-grid">${images.map((src) => `<div class="gallery-item" data-category="${category(src)}">${picture({ src, alt: imageLabel(src), caption: imageLabel(src) })}${src.toLowerCase().endsWith('.gif') ? '<span class="gif-label">ANIMATED GIF</span>' : ''}</div>`).join('')}</div></section>${cta()}`;
  return layout({ title: 'In action — MacroMachine gallery', description: 'Browse MacroMachine previews, documentation screenshots, and CityLayer walkthroughs.', active: 'gallery', body, features });
}

export function renderStart({ features }) {
  const body = `<section class="page-intro wrap"><p class="eyebrow">GETTING STARTED</p><h1>Install the plugin.<br><span class="gradient-text">Run a macro.</span></h1><p>Install MacroMachine in WorldPainter, import the ready-made macros, and run your first terrain edit.</p></section>
  <section class="wrap start-layout"><aside class="start-aside"><p class="eyebrow">ON THIS PAGE</p><a href="#download">Download</a><a href="#install">Windows installation</a><a href="#first-macro">Your first macro</a><a href="#other-platforms">Mac & Linux</a><a href="#help">Need a hand?</a></aside><div class="start-content">
    <section class="download-panel" id="download"><span class="tool-mark">MM</span><h2>Download MacroMachine.</h2><p>Free and open source under ${projectFacts.license}. No account, subscription, or activation key required.</p><button class="button button-primary" type="button" disabled aria-describedby="download-note">Download for Windows <span aria-hidden="true">↓</span></button><p id="download-note" class="small-note">Placeholder — the installer download will be connected here.</p><p>For current files, visit the <a href="${repository}/releases/latest">official GitHub releases ${arrow}</a>.</p><p class="small-note">${projectFacts.releases} public releases listed on GitHub. Community support is available on <a href="https://discord.gg/9kRJEMfGeE">Discord</a> and <a href="${repository}/issues">GitHub</a>.</p>${projectHistory()}</section>
    <section id="install"><p class="eyebrow">01 / INSTALL</p><h2>Install on Windows.</h2><p>The Windows installer locates WorldPainter and installs the plugin for you.</p><ol class="step-list"><li>Download the installer from the official releases page.</li><li>Save your work and close WorldPainter.</li><li>Open the installer and follow the installation prompts.</li><li>Finish setup. WorldPainter opens with MacroMachine.</li></ol><p class="small-note">Use installers from the official project only. If Windows displays a security warning, verify the source before proceeding.</p></section>
    <section id="first-macro"><p class="eyebrow">02 / YOUR FIRST MACRO</p><h2>Import and run a macro.</h2><p>Download the ready-made macros ZIP from the <a href="${repository}/releases/latest">releases page</a> and extract it.</p>${picture({ src: 'tutorial/macromachine_tool.PNG', alt: 'The M tool that opens the MacroMachine panel', caption: 'Find the M tool in WorldPainter to open MacroMachine.' })}<ol class="step-list"><li>Click the M tool to open the MacroMachine window.</li><li>Click Import, open the extracted defaultActions folder, select the macro files, and click Open.</li><li>Choose a macro. Its description explains what it does.</li><li>Press ▶ to run it on your map.</li></ol><a class="text-link" href="https://youtu.be/Q5R2G-uRatc">Watch the 100-second overview ${arrow}</a></section>
    <section id="other-platforms"><p class="eyebrow">03 / OTHER PLATFORMS</p><h2>Mac, Linux, or a manual install?</h2><p>Download the plugin JAR from the official release, install it with the WorldPainter plugin installer, then restart WorldPainter.</p><p>If you used MacroMachine 0.4.14 or older, first open Tools → Open plugins folder in WorldPainter and remove the old MacroMachine JAR. Keep only the version you intend to use.</p><a class="text-link" href="https://youtu.be/FtfwxDTzWgk">Watch the manual installation tutorial ${arrow}</a></section>
    <section id="help"><p class="eyebrow">04 / TROUBLESHOOTING</p><h2>Common problems.</h2><div class="faq"><details><summary>WorldPainter is still running.</summary><p>Save your worlds, close WorldPainter, and click Retry in the installer.</p></details><details><summary>The installer cannot find WorldPainter.</summary><p>Click Browse and select the folder containing worldpainter.exe.</p></details><details><summary>Which Java version do I need?</summary><p>The installer requires Java 21 or newer. Install a current Java from <a href="https://adoptium.net/">Adoptium</a> if needed. If WorldPainter still uses an old Java, reinstall WorldPainter.</p></details><details><summary>My macro runs, but nothing changes.</summary><p>Check the log. The macro’s conditions might not exist on your map—for example, a mountain macro on flat terrain. Step through its actions with the debugger and make sure annotations are visible.</p></details><details><summary>WorldPainter stopped opening after an update.</summary><p>Check Tools → Open plugins folder for duplicate MacroMachine JARs. Mixing older and newer plugin files can cause problems. Keep only the intended version. The Windows installer handles this cleanup.</p></details><details><summary>How do I report a problem?</summary><p>Open a <a href="${repository}/issues">GitHub issue</a> or contact @IR0NSIGHT in the <a href="https://discord.gg/9kRJEMfGeE">Discord community</a>. Include the latest logfile0.txt, usually located in the parent folder of the WorldPainter plugins directory.</p></details></div></section>
  </div></section>${cta()}`;
  return layout({ title: 'Get started — MacroMachine for WorldPainter', description: 'Install MacroMachine, run your first WorldPainter macro, and find help for Windows, Mac, and Linux.', active: 'get-started', body, features });
}
