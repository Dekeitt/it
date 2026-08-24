import { access, readFile } from 'node:fs/promises';

const manifestPath = new URL('../public/manifest.webmanifest', import.meta.url);
const manifest = JSON.parse(await readFile(manifestPath, 'utf8'));

const required = [
  ['name', manifest.name],
  ['short_name', manifest.short_name],
  ['start_url', manifest.start_url],
  ['display=standalone', manifest.display === 'standalone'],
  ['theme_color', manifest.theme_color],
];

for (const [name, value] of required) {
  if (!value) throw new Error(`PWA manifest missing ${name}`);
}

const icons = manifest.icons ?? [];
for (const size of ['192x192', '512x512']) {
  if (!icons.some((icon) => icon.sizes === size)) {
    throw new Error(`PWA manifest missing ${size} icon`);
  }
}

if (!icons.some((icon) => String(icon.purpose ?? '').includes('maskable'))) {
  throw new Error('PWA manifest missing maskable icon');
}

for (const icon of icons) {
  await access(new URL(`../public${icon.src}`, import.meta.url));
}

await access(new URL('../public/icons/apple-touch-icon.png', import.meta.url));
await access(new URL('../public/sw.js', import.meta.url));

const sw = await readFile(new URL('../public/sw.js', import.meta.url), 'utf8');
if (!sw.includes("'/api/'")) {
  throw new Error('Service worker must explicitly exclude /api/ from caching');
}

console.log('PWA manifest, icons and service-worker cache exclusions are valid.');
