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
