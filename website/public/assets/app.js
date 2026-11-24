const menuToggle = document.querySelector('.menu-toggle');
const navigation = document.querySelector('#main-nav');

function closeMenu() {
  menuToggle?.setAttribute('aria-expanded', 'false');
  navigation?.classList.remove('is-open');
}

menuToggle?.addEventListener('click', () => {
  const open = menuToggle.getAttribute('aria-expanded') !== 'true';
  menuToggle.setAttribute('aria-expanded', String(open));
  navigation?.classList.toggle('is-open', open);
});
navigation?.addEventListener('click', (event) => {
  if (event.target.closest('a')) closeMenu();
});
document.addEventListener('keydown', (event) => {
  if (event.key === 'Escape' && menuToggle?.getAttribute('aria-expanded') === 'true') {
    closeMenu();
    menuToggle.focus();
  }
});

const tabs = [...document.querySelectorAll('[data-showcase-src]')];
const hero = document.querySelector('.home-preview');
if (hero) {
  const slides = [...hero.querySelectorAll('[data-hero-slide]')];
  const selectors = [...hero.querySelectorAll('[data-hero-select]')];
  const details = [...hero.querySelectorAll('[data-hero-detail]')];
  const track = hero.querySelector('[data-hero-track]');
  const status = hero.querySelector('.hero-status');
  let current = 0;
  function updateSlide(index, announce = false) {
    current = index;
    slides.forEach((slide, i) => {
      slide.toggleAttribute('data-active', i === current);
      slide.inert = i !== current;
      slide.setAttribute('aria-hidden', String(i !== current));
    });
    selectors.forEach((button, i) => button.setAttribute('aria-pressed', String(i === current)));
    details.forEach((detail, i) => { detail.hidden = i !== current; });
    if (announce) status.textContent = slides[current].getAttribute('aria-label');
  }
  selectors.forEach((button, index) => button.addEventListener('click', () => {
    track.scrollTo({ left: index * track.clientWidth, behavior: matchMedia('(prefers-reduced-motion: reduce)').matches ? 'instant' : 'smooth' });
  }));
  track.addEventListener('scroll', () => {
    const index = Math.min(slides.length - 1, Math.max(0, Math.round(track.scrollLeft / track.clientWidth)));
    if (index !== current) updateSlide(index, true);
  });
  new ResizeObserver(() => { track.scrollTo({ left: current * track.clientWidth, behavior: 'instant' }); }).observe(track);
  updateSlide(0);
  hero.querySelector('[data-hero-controls]').hidden = false;
}
function selectTab(tab) {
  tabs.forEach((item) => {
    item.setAttribute('aria-selected', String(item === tab));
    item.tabIndex = item === tab ? 0 : -1;
  });
  const image = document.querySelector('#showcase-image');
  image.src = tab.dataset.showcaseSrc;
  image.alt = tab.dataset.showcaseAlt;
  document.querySelector('#showcase-name').textContent = tab.dataset.showcaseName;
  document.querySelector('#showcase-description').textContent = tab.dataset.showcaseDescription;
  document.querySelector('#showcase-link').href = tab.dataset.showcaseLink;
  document.querySelector('#showcase-panel').setAttribute('aria-labelledby', tab.id);
}
tabs.forEach((tab, index) => {
  tab.addEventListener('click', () => selectTab(tab));
  tab.addEventListener('keydown', (event) => {
    let next;
    if (event.key === 'ArrowRight') next = (index + 1) % tabs.length;
    if (event.key === 'ArrowLeft') next = (index - 1 + tabs.length) % tabs.length;
    if (event.key === 'Home') next = 0;
    if (event.key === 'End') next = tabs.length - 1;
    if (next !== undefined) {
      event.preventDefault();
      tabs[next].focus();
      selectTab(tabs[next]);
    }
  });
});

const filters = [...document.querySelectorAll('[data-filter]')];
filters.forEach((button) => button.addEventListener('click', () => {
  filters.forEach((filter) => filter.setAttribute('aria-pressed', String(filter === button)));
  let count = 0;
  document.querySelectorAll('[data-category]').forEach((item) => {
    item.hidden = button.dataset.filter !== 'all' && item.dataset.category !== button.dataset.filter;
    if (!item.hidden) count++;
  });
  document.querySelector('#gallery-count').textContent = count;
}));

const dialog = document.querySelector('.image-dialog');
document.querySelectorAll('[data-image]').forEach((button) => button.addEventListener('click', () => {
  const image = dialog.querySelector('.dialog-image');
  image.src = button.dataset.image;
  image.alt = button.querySelector('img').alt;
  dialog.querySelector('.dialog-caption').textContent = button.dataset.caption;
  dialog.showModal();
}));
dialog?.querySelector('.dialog-close').addEventListener('click', () => dialog.close());
dialog?.addEventListener('click', (event) => {
  if (event.target === dialog) {
    const bounds = dialog.getBoundingClientRect();
    if (event.clientX < bounds.left || event.clientX > bounds.right || event.clientY < bounds.top || event.clientY > bounds.bottom) dialog.close();
  }
});
