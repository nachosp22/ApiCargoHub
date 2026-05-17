const fs = require('fs');
const zlib = require('zlib');

const file = process.argv[2];
if (!file) {
  console.error('Usage: node validate_navigation_drawio.js <file.drawio>');
  process.exit(1);
}

const content = fs.readFileSync(file, 'utf8');
const match = content.match(/<diagram[^>]*>([\s\S]*?)<\/diagram>/);
if (!match) {
  console.error('No diagram payload found');
  process.exit(1);
}

const xml = decodeURIComponent(
  zlib.inflateRawSync(Buffer.from(match[1], 'base64')).toString('utf8')
);

const nodes = (xml.match(/vertex="1"/g) || []).length;
const edges = (xml.match(/edge="1"/g) || []).length;

console.log(`nodes=${nodes} edges=${edges}`);

if (nodes < 20 || edges < 20) {
  console.error('Unexpectedly small diagram');
  process.exit(1);
}
