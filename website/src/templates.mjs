import { repository, playlist, projectFacts, discord, sampleSchematic, reportTemplate, installerReport } from './content.mjs';

export const escape = (value) => String(value).replaceAll('&', '&amp;').replaceAll('<', '&lt;').replaceAll('>', '&gt;').replaceAll('"', '&quot;').replaceAll("'", '&#39;');
export const imageUrl = (src) => 'assets/images/' + src.split('/').map(encodeURIComponent).join('/');
/** @param {import('./content.mjs').Section} section */
export const sectionAnchor = (section) => section.id ?? section.title.toLowerCase().replace(/[^a-z0-9]+/g, '-').replace(/^-|-$/g, '');

function header(active, features) {
  return `<a class="skip-link" href="#main">Skip to content</a>
  <header class="site-header"><div class="wrap header-inner">
    <a class="brand" href="index.html" aria-label="MacroMachine home"><span class="brand-icon"><img src="${imageUrl('icon.svg')}" width="34" height="34" alt=""></span><span>MacroMachine<span class="brand-sub">FOR WORLDPAINTER</span></span></a>
    <button class="menu-toggle" type="button" aria-expanded="false" aria-controls="main-nav">Menu <span aria-hidden="true">☰</span></button>
    <nav class="main-nav" id="main-nav" aria-label="Main navigation">
      <a href="get-started.html" ${active === 'get-started' ? 'aria-current="page"' : ''}>Get started</a>
      <a href="documentation.html" ${active === 'documentation' ? 'aria-current="page"' : features.some((feature) => feature.slug === active) ? 'class="nav-active"' : ''}>Documentation</a>
      <a href="${repository}">GitHub</a>
      <a href="${discord}">Discord</a>
      <a class="nav-download" href="get-started.html#download">Download</a>
    </nav>
  </div></header>`;
}

function footer(features) {
  return `<footer class="site-footer"><div class="wrap">
    <div class="footer-top"><div><a class="brand" href="index.html"><span class="brand-icon"><img src="${imageUrl('MacroMachine_Icon.png')}" width="34" height="34" alt=""></span><span>MacroMachine</span></a><p>Free and open source. No account required.<br>Community-supported WorldPainter plugin.</p></div>
    <div><h2>Tool guides</h2>${features.filter((feature) => !feature.parent).map((f) => `<a href="${f.slug}.html">${escape(f.name)}</a>`).join('')}</div>
    <div><h2>Help & resources</h2><a href="get-started.html">Getting started</a><a href="documentation.html">Documentation</a><a href="${repository}">GitHub</a><a href="${playlist}">Video tutorials</a><a href="${discord}">Help & bug reports on Discord</a><a href="${repository}/issues">GitHub Issues (optional)</a></div></div>
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

function picture(media, { eager = false, className = '', caption = true } = {}) {
  return `<figure class="screenshot ${className}"><button class="image-expand" type="button" data-image="${imageUrl(media.src)}" data-caption="${escape(media.caption)}" aria-label="Enlarge: ${escape(media.alt)}"><img src="${imageUrl(media.src)}" alt="${escape(media.alt)}" loading="${eager ? 'eager' : 'lazy'}" decoding="async"><span class="expand-mark" aria-hidden="true">↗</span></button>${caption ? `<figcaption>${escape(media.caption)}</figcaption>` : ''}</figure>`;
}

function toolIcon(feature) {
  return feature.icon ? `<img class="tool-icon" src="${imageUrl(feature.icon)}" width="30" height="30" alt="">` : '';
}

function carouselIcon(feature) {
  if (feature.icon) return toolIcon(feature);
  const shape = feature.slug === 'gradient-editor' ? '<path d="M4 19V5m0 14h16M5 17c5 0 5-10 14-10"/><circle cx="9" cy="15" r="1.5"/><circle cx="17" cy="7.5" r="1.5"/>' : '<path d="M3 5h18l-7 8v5l-4 2v-7z"/>';
  return `<svg viewBox="0 0 24 24" aria-hidden="true" fill="none" stroke="currentColor" stroke-width="1.7" stroke-linecap="round" stroke-linejoin="round">${shape}</svg>`;
}

function projectHistory() {
  return `<p class="project-history">In development since ${projectFacts.developmentSince}.</p>`;
}

/** @param {import('./virustotal.mjs').InstallerScan | null} [scan] */
export function renderInstallerReport(scan = null) {
  const matches = scan?.sha256 === installerReport.sha256;
  const date = matches ? new Date(scan.analysedAt).toLocaleDateString('en-GB', { day: 'numeric', month: 'short', year: 'numeric', timeZone: 'UTC' }) : '';
  return `<div class="installer-report"><a href="${installerReport.url}">VirusTotal report for installer ${installerReport.version}</a>${matches ? `<p class="installer-scan${scan.malicious || scan.suspicious ? ' scan-flagged' : ''}">${scan.malicious} malicious · ${scan.suspicious} suspicious · ${scan.completed} completed engine verdicts<br>Analysis: <time datetime="${escape(scan.analysedAt)}">${date}</time>. Results are a snapshot; open the report for current details.</p>` : ''}<details><summary>Installer file and SHA-256</summary><p>${installerReport.fileName}</p><code>${installerReport.sha256}</code></details></div>`;
}

function factIcon(shape) {
  const shapes = {
    free: '<circle cx="12" cy="12" r="9"/><path d="m8 12 3 3 5-6"/>',
    source: '<path d="m8 8-4 4 4 4m8-8 4 4-4 4m-3-11-2 14"/>',
    calendar: '<rect x="4" y="5" width="16" height="15" rx="2"/><path d="M8 3v4m8-4v4M4 10h16"/>',
    download: '<path d="M12 3v12m-4-4 4 4 4-4M5 17v3h14v-3"/>',
    releases: '<path d="M4 7h16v13H4zM7 4h10M4 11h16m-11 4h6"/>',
    community: '<circle cx="9" cy="8" r="3"/><path d="M3 20v-2a6 6 0 0 1 12 0v2m1-14a3 3 0 0 1 0 6m1 2a5 5 0 0 1 4 5v1"/>',
    endorsement: '<path d="m12 3 3 2 4 .5v6c0 4-3 7-7 9-4-2-7-5-7-9v-6L9 5z"/><path d="m9 12 2 2 4-4"/>',
  };
  return `<span class="fact-icon" aria-hidden="true"><svg viewBox="0 0 24 24" width="24" height="24" fill="none" stroke="currentColor" stroke-width="1.7" stroke-linecap="round" stroke-linejoin="round">${shapes[shape]}</svg></span>`;
}

function featureCard(feature, description = feature.description) {
  return `<a class="feature-card accent-${feature.accent}" href="${feature.slug}.html">${feature.icon ? `<span class="tool-mark" aria-hidden="true">${toolIcon(feature)}</span>` : ''}<p class="feature-tag">${escape(feature.tag)}</p><h3>${escape(feature.name)}</h3><p>${escape(description)}</p><span class="card-link">Read the ${escape(feature.name)} guide</span></a>`;
}

/** @param {import('./content.mjs').Feature[]} features @param {string} [active] */
function renderToolNavigation(features, active = '') {
  const current = features.find((feature) => feature.slug === active);
  return `<nav class="doc-tool-nav wrap" id="reference" aria-label="Browse by tool"><h2>Browse by tool</h2><ul class="doc-tool-list">${features.filter((feature) => !feature.parent).map((feature) => {
    const references = features.filter((item) => item.parent === feature.slug);
    return `<li${feature.slug === active || feature.slug === current?.parent ? ' class="doc-tool-current"' : ''}><a class="doc-tool-name" href="${feature.slug}.html"${feature.slug === active ? ' aria-current="page"' : ''}>${escape(feature.name)}</a><a class="doc-tool-controls" href="${feature.slug}.html#controls">Controls reference</a>${references.length ? `<ul aria-label="${escape(feature.name)} references">${references.map((item) => `<li><a href="${item.slug}.html"${item.slug === active ? ' aria-current="page"' : ''}>${escape(item.name)} reference</a></li>`).join('')}</ul>` : ''}</li>`;
  }).join('')}</ul></nav>`;
}

/**
 * The home carousel's slides for one feature: its own presentations, or the
 * feature itself with its hero. A tool guide shows the first slide's GIF on top.
 * @param {import('./content.mjs').Feature} feature
 */
export function carouselSlides(feature) {
  return (feature.carousel ?? [{ id: feature.slug, name: feature.name, hero: feature.hero }]).map((presentation) => ({ ...feature, ...presentation }));
}

export function renderHome({ features, installerScan }) {
  const preview = features.find((f) => f.slug === '3d-preview');
  const slides = [preview, ...features.filter((feature) => feature !== preview)].flatMap(carouselSlides);
  const cardDescriptions = {
    macros: 'Build reusable sequences of global operations without programming. Run them across your map or inspect each action with the debugger.',
    citylayer: 'Choose each schematic’s position instead of random placement. Arrange individual buildings or assemble continuous walls.',
    paths: 'Edit connected nodes to shape roads, rivers, or painted paths, with control over width, height, and terrain blending.',
    '3d-preview': 'See an export-based 3D view update as you edit, without exporting manually or reloading Minecraft.',
  };
  const body = `<section class="home-hero"><div class="wrap hero-grid">
    <div class="hero-copy"><p>MACROMACHINE FOR WORLDPAINTER</p><h1>Automate edits. Place buildings.</h1>
      <p class="hero-description">MacroMachine provides capabilities missing from WorldPainter and World Machine—from precise manual edits to reusable global operations.</p>
      <div class="intro-download" id="download"><button class="button button-primary" type="button" disabled aria-describedby="download-note">Download MacroMachine</button><p id="download-note">Download placeholder: the installer link is not connected yet.</p>${renderInstallerReport(installerScan)}<a href="get-started.html">Installation guide</a> · <a href="#tools">Browse the tools</a></div>
      <p class="hero-note">Free and open source · No account required</p>
    </div>
    <div class="home-preview" role="group" aria-roledescription="carousel" aria-label="Tool images">
      <div class="hero-track" data-hero-track>${slides.map((feature, index) => `<div data-hero-slide${index === 0 ? ' data-active' : ''} role="group" aria-roledescription="slide" aria-label="${index + 1} of ${slides.length}: ${escape(feature.name)}" aria-describedby="hero-caption-${feature.id}">${picture(feature.hero, { eager: index === 0, caption: false })}</div>`).join('')}</div>
      <div class="hero-controls" data-hero-controls hidden role="group" aria-label="Choose a tool image">${slides.map((feature, index) => `<button type="button" class="accent-${feature.accent}" data-hero-select="${index}" aria-label="Show ${escape(feature.name)}" title="Show ${escape(feature.name)}" aria-pressed="${index === 0}">${carouselIcon(feature)}</button>`).join('')}</div><span class="hero-status" role="status" aria-live="polite" aria-atomic="true"></span>
      <div class="hero-details">${slides.map((feature, index) => `<div class="accent-${feature.accent}" data-hero-detail${index ? ' hidden' : ''}><p class="hero-tool-tag">${escape(feature.tag)}</p><h2 class="hero-slide-link"><a href="${feature.slug}.html">${escape(feature.name)}</a></h2><p class="hero-media-caption" id="hero-caption-${feature.id}">${escape(feature.hero.caption)}</p></div>`).join('')}</div>
    </div>
  </div></section>
  <section class="intro-facts wrap" aria-label="About the project"><ul class="fact-list"><li>${factIcon('free')}<span>Free to use</span></li><li>${factIcon('source')}<span><a href="${repository}">Open source</a> under <a href="${repository}/blob/master/LICENCE">${projectFacts.license}</a></span></li><li>${factIcon('calendar')}<span>In development since ${projectFacts.developmentSince}</span></li><li>${factIcon('download')}<span><a href="${repository}/releases">${projectFacts.downloads} downloads</a></span></li><li>${factIcon('releases')}<span><a href="${repository}/releases">${projectFacts.releases} public releases</a></span></li></ul><p class="stats-note">Downloads count release assets, not unique users. Figures checked ${projectFacts.checkedOn}.</p><div class="intro-context"><section><h2>${factIcon('community')}<span>Community-supported</span></h2><p>Get help, report bugs, and share macros on <a href="${discord}">Discord</a>. No GitHub account is needed; <a href="${repository}/issues">GitHub Issues</a> is also available.</p></section><section><h2>${factIcon('endorsement')}<span>Endorsed by WorldPainter</span></h2><p>Vetted by WorldPainter’s developer and previously built on the official WorldPainter build server. Current builds run separately to support Java 21.</p></section></div></section>
  <section class="section tools-section wrap" id="tools"><div class="section-heading"><div><p>TOOLS & FEATURES</p><h2>What the plugin adds.</h2></div></div><div class="feature-grid">${features.filter((feature) => !feature.parent).map((feature) => featureCard(feature, cardDescriptions[feature.slug])).join('')}</div></section>
  <section class="section wrap workflow-section" id="project"><h2>Works inside WorldPainter.</h2><p>Use your existing terrain, custom layers, and schematic files. Open the tools from WorldPainter’s toolbar.</p><p>IR0NSIGHT started the project as a river tool; macros grew from work on decorating riverbanks.</p></section>
  <section class="section wrap resources-section"><h2>Install MacroMachine for WorldPainter.</h2><a href="get-started.html">Installation guide</a> · <a href="documentation.html">Guides and reference</a><p>Prefer a quick introduction? Watch <a href="https://youtu.be/Q5R2G-uRatc">the 100-second overview</a>, or <a href="${discord}">ask on Discord</a>.</p></section>`;
  return layout({ title: 'MacroMachine — Tools for WorldPainter', description: 'A WorldPainter plugin for precise schematic placement, small-scale roads and rivers, reusable global operations, and live 3D export feedback.', body, features });
}

/** @param {import('./content.mjs').Feature} feature @param {{features: import('./content.mjs').Feature[]}} context */
export function renderGuide(feature, { features }) {
  const parent = features.find((item) => item.slug === feature.parent);
  const references = features.filter((item) => item.parent === feature.slug);
  const title = `${feature.name} ${parent ? 'reference' : 'guide'}`;
  const showcase = carouselSlides(feature)[0]?.hero;
  const body = `<section class="page-intro wrap guide-intro"><nav class="breadcrumbs" aria-label="Breadcrumb"><a href="documentation.html">Documentation</a>${parent ? `<span aria-hidden="true">/</span><a href="${parent.slug}.html">${escape(parent.name)}</a>` : ''}<span aria-hidden="true">/</span><span aria-current="page">${escape(feature.name)}</span></nav><h1>${escape(title)}</h1><p>${escape(feature.guideIntro)}</p><p class="feature-jump-links"><a href="#workflow">Contents</a>${feature.sections.some((section) => section.id === 'controls') ? '<a href="#controls">Controls reference</a>' : ''}</p></section>
  ${showcase ? `<section class="wrap guide-showcase" id="showcase" aria-label="Showcase">${picture(showcase, { eager: true })}</section>` : ''}
  ${renderToolNavigation(features, feature.slug)}
  <div class="wrap guide-content" id="workflow">${feature.notice ? `<aside class="notice"><strong>Before you begin</strong><p>${escape(feature.notice)}</p></aside>` : ''}
    <nav class="feature-contents" aria-label="On this page"><h2>On this page</h2><ul>${feature.sections.map((section) => `<li><a href="#${sectionAnchor(section)}">${escape(section.title)}</a></li>`).join('')}${references.length ? '<li><a href="#macro-reference">Macro references</a></li>' : ''}${showcase ? '' : '<li><a href="#example">Example</a></li>'}</ul></nav>
    ${references.length ? `<section class="macro-reference" id="macro-reference"><h2>Macro references</h2><p>Use these references while configuring an action or restricting where a macro applies.</p><ul class="doc-links">${references.map((item) => `<li><a href="${item.slug}.html">${escape(item.name)}</a></li>`).join('')}</ul></section>` : ''}
    ${feature.sections.map(renderFeatureSection).join('')}
    ${showcase ? '' : `<section class="guide-example" id="example"><h2>Example</h2>${picture(feature.hero)}</section>`}
  </div>`;
  return layout({ title: `${title} — MacroMachine`, description: feature.guideIntro, active: feature.slug, body, features, image: (showcase ?? feature.hero).src });
}

/** @param {import('./content.mjs').Section} section */
function renderFeatureSection(section) {
  return `<section class="feature-story ${section.media ? 'with-media' : 'text-story'}${section.planned ? ' planned-tutorial' : ''}" id="${sectionAnchor(section)}"><div class="story-copy">${section.planned ? '<p class="doc-status">Planned tutorial</p>' : ''}<h2>${escape(section.title)}</h2><p>${escape(section.body)}</p>${section.bullets ? `<ol class="step-list">${section.bullets.map((bullet) => `<li>${escape(bullet)}</li>`).join('')}</ol>` : ''}${section.table ? `<table class="reference-table" aria-label="${escape(section.title)}"><thead><tr>${section.table.headings.map((heading) => `<th scope="col">${escape(heading)}</th>`).join('')}</tr></thead><tbody>${section.table.rows.map((row) => `<tr>${row.map((cell, index) => index ? `<td>${escape(cell)}</td>` : `<th scope="row">${escape(cell)}</th>`).join('')}</tr>`).join('')}</tbody></table>` : ''}${section.links ? `<ul class="doc-links">${section.links.map((link) => `<li><a href="${escape(link.href)}"${link.download ? ' download' : ''}>${escape(link.label)}</a></li>`).join('')}</ul>` : ''}</div>${section.media ? picture(section.media) : ''}</section>`;
}

/** @param {{features: import('./content.mjs').Feature[], compatibility: import('../build.mjs').Compatibility, installerScan?: import('./virustotal.mjs').InstallerScan | null}} context */
export function renderStart({ features, compatibility, installerScan }) {
  const body = `<section class="page-intro wrap"><p>GETTING STARTED</p><h1>Install the plugin. See your world in 3D.</h1><p>Install MacroMachine, open a world, and enable 3D Preview with Auto Update. You do not need macros, schematics, or World Machine to begin.</p></section>
  <section class="wrap start-layout"><aside class="start-aside"><p>On this page</p><a href="#requirements">Requirements</a><a href="#download">Download</a><a href="#install">Windows installation</a><a href="#first-preview">Your first 3D preview</a><a href="#first-macro">Optional macros</a><a href="#other-platforms">Linux & macOS</a><a href="#help">Need a hand?</a></aside><div class="start-content">
    <section id="requirements"><h2>What you need</h2><p>MacroMachine runs inside WorldPainter. WorldPainter must run Java ${escape(compatibility.javaVersion)} or newer; having a newer Java installed elsewhere is not enough. The installer does not install Java or switch WorldPainter’s runtime for you.</p><p>Current builds compile against WorldPainter ${escape(compatibility.worldPainterVersion)}. If you run into a compatibility problem, contact IR0NSIGHT on <a href="${discord}">Discord</a>.</p><p>World Machine is not required. MacroMachine addresses gaps those applications leave in precise manual editing—small rivers, roads, and individual buildings—and adds visual macros for reusable global operations such as stone on steep cliffs and grass on flat plains. It runs inside WorldPainter, not as a World Machine integration.</p><p>Free and open source under ${projectFacts.license}. No account, subscription, or activation key required.</p></section>
    <section class="download-panel" id="download"><h2>Download MacroMachine</h2><button class="button button-primary" type="button" disabled aria-describedby="download-note">Download for Windows</button><p id="download-note" class="small-note">Placeholder — the installer download will be connected here.</p>${renderInstallerReport(installerScan)}<p>For current files, visit the <a href="${repository}/releases/latest">official GitHub releases</a> and choose the Windows setup .exe. That is the only download needed to install the tools.</p><p>The installer contains the plugin only: no macros, layers, or schematics. The default macros ZIP is optional, and you can use 3D Preview, roads, and rivers without it.</p><p class="small-note">${projectFacts.releases} public releases listed on GitHub. Help and bug reports are welcome on <a href="${discord}">Discord</a>; a GitHub account is not required.</p>${projectHistory()}</section>
    <section id="install"><h2>Install on Windows</h2><p>The Windows installer locates WorldPainter and installs the plugin for you. First make sure WorldPainter uses Java ${escape(compatibility.javaVersion)} or newer.</p><ol class="step-list"><li>Download the Windows installer from the official releases page.</li><li>Save your work and close WorldPainter.</li><li>Open the installer and follow the installation prompts.</li><li>Finish setup. WorldPainter opens with MacroMachine.</li></ol><p class="small-note">Use installers from the official project only. If Windows displays a security warning, verify the source before proceeding. See <a href="documentation.html#update">updating and uninstalling</a> for an existing installation.</p></section>
    <section id="first-preview"><h2>Start with 3D Preview and Auto Update</h2><p>Use a small area of an existing world to see your edits in 3D without manually exporting and reloading Minecraft each time. Save a copy before experimenting; see <a href="documentation.html#backups">world backups</a>.</p>${picture({ src: 'tutorial/macromachine_tool.PNG', alt: 'The M tool that opens the MacroMachine panel', caption: 'Open M once, then use the separate 3D Preview toolbar tool to choose an area.' })}<ol class="step-list"><li>Open a world and click the M tool once to initialize the MacroMachine window. Return to WorldPainter.</li><li>Select the 3D Preview toolbar tool. Use a small brush radius and left-click existing terrain.</li><li>The tool highlights the chosen tiles, exports the area in the background, and opens the renderer.</li><li>Enable Auto Update in the 3D Preview tool options.</li><li>Switch to a terrain or painting tool and edit inside the highlighted area. Keep the 3D window alongside WorldPainter to inspect the result.</li></ol><p>This is an export-based surface preview, not a complete final-export guarantee. Underground terrain is hidden and some export-processing passes are skipped. A normal exported Minecraft map needs neither WorldPainter nor MacroMachine to play.</p><p><a href="3d-preview.html#controls">3D Preview controls and settings</a> · <a href="3d-preview.html#preview-limits">Preview limits</a></p></section>
    <section id="first-macro"><h2>Optional: import and run a macro</h2><p>Download the optional default-macros.zip from the <a href="${repository}/releases/latest">releases page</a> and extract it. These examples may be outdated and need updating and testing; inspect them and work on a copy rather than treating them as verified presets.</p><ol class="step-list"><li>Click M to open the MacroMachine window.</li><li>Click Import, open the extracted defaultActions folder, select the .macro files, and click Open.</li><li>For slope painting, inspect Apply: Alpine mountains on an existing hilly map. Its supplied configuration enables paint slopes; the other included stages are disabled.</li><li>Check its actions and filters, then press ▶ to run on the current dimension. A WorldPainter selection does not automatically limit every macro.</li></ol><p><a href="macros.html#default-macros">Example prerequisites</a> · <a href="macros.html#first-macro">Create your own macro</a> · <a href="macros.html#limit-to-selection">Limit the affected area</a></p><a class="text-link" href="https://youtu.be/Q5R2G-uRatc">Watch the 100-second overview</a></section>
    <section id="other-platforms"><h2>Linux and macOS</h2><h3>Linux</h3><p>Source builds have been tested on Linux Mint 22, including 3D Preview; part of the renderer was developed there. The Windows release JAR contains OS-dependent native libraries, including LWJGL, and is not a Linux build. Contact IR0NSIGHT on <a href="${discord}">Discord for help building and running on Linux</a>.</p><h3>macOS</h3><p>macOS has not been tested. Manual installation is expected to involve placing the JAR in the plugins folder and configuring WorldPainter’s VM options, but a verified macOS procedure is not available yet. Contact IR0NSIGHT for help and share your findings.</p><p>The <a href="https://youtu.be/FtfwxDTzWgk">manual-install video</a> is available as background, not a verified macOS walkthrough. If you used MacroMachine 0.4.14 or older, check Tools → Open plugins folder for an old MacroMachine JAR before installing another version.</p></section>
    <section id="help"><h2>Common problems</h2><div class="faq"><details><summary>WorldPainter is still running.</summary><p>Save your worlds, close WorldPainter, and click Retry in the installer.</p></details><details><summary>The installer cannot find WorldPainter.</summary><p>Click Browse and select the folder containing worldpainter.exe.</p></details><details><summary>Which Java version do I need?</summary><p>WorldPainter itself must use Java ${escape(compatibility.javaVersion)} or newer. Install a current Java from <a href="https://adoptium.net/">Adoptium</a> if needed, and configure WorldPainter to use it. MacroMachine does not make that change for you. If WorldPainter still uses an old bundled Java, reinstalling WorldPainter may be necessary.</p></details><details><summary>My macro runs, but nothing changes.</summary><p>Check the log. The macro’s conditions might not exist on your map—for example, a mountain macro on flat terrain. Step through its actions with the debugger and make sure annotations are visible. Debugging applies real edits and diagnostic colors; it is not a dry run.</p></details><details><summary>WorldPainter stopped opening after an update.</summary><p>Check Tools → Open plugins folder for duplicate MacroMachine JARs. Mixing older and newer plugin files can cause problems. Keep only the intended version. The Windows installer handles its own installation cleanup.</p></details><details><summary>How do I report a problem?</summary><p>Ask for help or report bugs on <a href="${discord}">Discord</a>. <a href="${repository}/issues">GitHub Issues</a> is optional. Use the <a href="documentation.html#help">short report template</a>; include the latest logfile0.txt if available, usually in the parent folder of the WorldPainter plugins directory.</p></details></div></section>
  </div></section>`;
  return layout({ title: 'Get started — MacroMachine for WorldPainter', description: 'Install MacroMachine, enable 3D Preview with Auto Update, and check WorldPainter, Java, Windows, Linux, and macOS requirements.', active: 'get-started', body, features });
}

export function renderDocumentation({ features }) {
  const tasks = [
    ['Get started', [['get-started.html#requirements', 'Check requirements and downloads'], ['get-started.html#first-preview', 'Preview my world with Auto Update'], ['3d-preview.html#preview-limits', 'Understand preview limits']]],
    ['Place and shape', [['citylayer.html#place-a-building', 'Place a house'], ['citylayer.html#sample-schematic', 'Download the example schematic'], ['citylayer.html#city-walls', 'Build continuous city walls'], ['paths.html#road-tutorial', 'Build a road'], ['paths.html#river-tutorial', 'Build a river'], ['paths.html#paint-along-path', 'Paint along a path']]],
    ['Edit a path', [['paths.html#start-a-path', 'Start and place path nodes'], ['paths.html#transform', 'Move, rotate, and scale a node group'], ['paths.html#height-profile', 'Inspect heights along the route'], ['paths.html#operations-reference', 'Randomize, snap, or subdivide nodes'], ['paths.html#road-settings', 'Road settings and slope limits'], ['paths.html#river-settings', 'River settings, water, and bank paint'], ['paths.html#save-and-undo', 'Apply, undo, and save a path']]],
    ['Reuse global operations', [['macros.html#default-macros', 'Inspect the optional example macros'], ['macros.html#first-macro', 'Paint stone, grass, and underwater sand'], ['macros.html#limit-to-selection', 'Restrict a macro to a selection'], ['macros.html#debugger', 'Step through a macro'], ['macros.html#share-macros', 'Export and share macros'], ['macros.html#save-and-back-up', 'Back up my macro library']]],
  ];
  const body = `<section class="page-intro wrap"><p>DOCUMENTATION</p><h1>Guides and reference.</h1><p>Start with <a href="get-started.html#first-preview">3D Preview and Auto Update</a>, follow a tool guide, or jump straight to a control reference. MacroMachine is used while world painting; your exported Minecraft world is a normal map that does not require the plugin.</p></section>
  ${renderToolNavigation(features)}
  <div class="wrap start-layout"><aside class="start-aside"><p>On this page</p><a href="#reference">Browse by tool</a><a href="#tasks">Task index</a><a href="#backups">Backups</a><a href="#update">Update & uninstall</a><a href="#remove">Remove project dependencies</a><a href="#help">Help & bug reports</a></aside><div class="start-content">
    <section id="tasks"><h2>What do you want to do?</h2><div class="doc-task-grid">${tasks.map(([title, links]) => `<section><h3>${title}</h3><ul>${links.map(([href, label]) => `<li><a href="${href}">${label}</a></li>`).join('')}</ul></section>`).join('')}</div></section>
    <section id="backups"><h2>Try changes on a copy</h2><p>Back up important worlds before experimenting, especially before whole-dimension macros or CityLayer layout work. WorldPainter normally keeps three previous saves beside Example.world as Example.1.world, Example.2.world, and Example.3.world. This is configurable in Edit → Preferences → General under the number of .world backups to keep; zero disables new backup rotation.</p><p>Backups accumulate across successive saves. They are not three copies created on the first save, and they are not a substitute for keeping an independent copy before a major change. Undo history is finite; CityLayer building placements have a separate undo/redo limitation.</p><p><a href="citylayer.html#controls">CityLayer controls and undo limitation</a> · <a href="macros.html#save-and-back-up">Back up macros separately</a></p></section>
    <section id="update"><h2>Update or uninstall</h2><h3>Update on Windows</h3><p>Back up your world and important macros, close WorldPainter, then run the newer Windows installer. Your saved macro library is separate from the plugin JAR. Keep only the plugin version you intend to load; duplicate old JARs can prevent startup. The installer does not update WorldPainter’s Java runtime.</p><h3>Uninstall on Windows</h3><p>Use Windows Settings → Apps → MacroMachine for WorldPainter → Uninstall. Your macros stay. Before uninstalling, follow the project cleanup instructions below for .world files that contain MacroMachine-specific layers.</p><p>Linux source builds and untested macOS installations need platform-specific help; contact <a href="${discord}">IR0NSIGHT on Discord</a> rather than applying the Windows JAR instructions unchanged.</p></section>
    <section id="remove"><h2>Remove MacroMachine dependencies from a project</h2><p>Exported Minecraft maps do not depend on MacroMachine. An editable .world project can depend on its layer types. Remove them while MacroMachine is still installed, and keep an untouched backup first.</p><ul class="doc-list"><li>Macro Selection</li><li>Heatmap</li><li>3d Preview — the preview highlight layer</li><li>Every CityLayer instance, regardless of its name; the default is My City Layer</li><li>PathPreviewLayer, if present in an older project</li></ul><p>Do not remove ordinary WorldPainter custom layers simply because a macro used or included them. Deleting a CityLayer removes its building placements from subsequent exports of that project; it does not preserve them without the plugin.</p><ol class="step-list"><li>Save a separate backup. Turn off Auto Update and switch away from MacroMachine tools before cleaning the project.</li><li>For each dimension, open Edit → Global Operations → remove a layer. Remove each relevant MacroMachine layer without selection, height, slope, or other restrictions. This operation is dimension-wide, not automatically world-wide.</li><li>Delete custom CityLayer instances as well: right-click their layer buttons → Remove, then confirm. Clearing painted data alone is not the same as deleting the custom layer and its placements.</li><li>If you use Edit → Delete unused layers, select only the intended MacroMachine layers; the dialog initially selects unrelated unused layers too. Hiding a layer or disabling export is not removal.</li><li>Save a separate cleaned .world copy. Restart WorldPainter without MacroMachine and verify that copy opens before relying on it. If it does not, reopen the original with MacroMachine installed and ask for help.</li></ol><p>Older .world backups may still contain the removed layers. Keep them with the original plugin-dependent project rather than treating them as cleaned copies.</p></section>
    <section id="help"><h2>Get help or report a bug</h2><p>Ask on <a href="${discord}">Discord</a> for help, bug reports, Linux build assistance, and sharing macros. Contact IR0NSIGHT there; no GitHub account is required. <a href="${repository}/issues">GitHub Issues</a> is an optional alternative. Support is community-based, with no guaranteed response time.</p><h3>A short report is enough to start</h3><pre class="report-template"><code>${escape(reportTemplate)}</code></pre><p>Helpful extras, not requirements: a screenshot or GIF, MacroMachine and WorldPainter versions, your OS and WorldPainter’s Java version, and the relevant logfile0.txt if available. Share only files you are comfortable making available to others.</p></section>
  </div></div>`;
  return layout({ title: 'Documentation — MacroMachine for WorldPainter', description: 'Task guides, tool controls, backups, updating, project cleanup, and Discord help for MacroMachine users.', active: 'documentation', body, features });
}
