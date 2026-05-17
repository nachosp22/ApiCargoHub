const fs = require('fs');
const path = require('path');

const file = path.join(__dirname, 'TFG_CargoHub_ER_completo_A3_ovalos.drawio');
const raw = fs.readFileSync(file, 'utf8');

const rootMatch = raw.match(/(<root>)([\s\S]*?)(<\/root>)/);
if (!rootMatch) {
  throw new Error('No <root> section found');
}

const before = raw.slice(0, rootMatch.index) + rootMatch[1];
const inner = rootMatch[2];
const after = rootMatch[3] + raw.slice(rootMatch.index + rootMatch[0].length);

const cellRegex = /\s*<mxCell\b[\s\S]*?<\/mxCell>|\s*<mxCell\b[^>]*\/>/g;
const cells = inner.match(cellRegex) || [];

const system = [];
const edges = [];
const others = [];

for (const block of cells) {
  const idMatch = block.match(/id="([^"]+)"/);
  const id = idMatch ? idMatch[1] : '';
  if (id === '0' || id === '1') {
    system.push(block);
  } else if (/\bedge="1"/.test(block)) {
    edges.push(block);
  } else {
    others.push(block);
  }
}

const reordered = '\n' + [...system, ...edges, ...others].map(b => b.trimEnd()).join('\n') + '\n      ';
fs.writeFileSync(file, before + reordered + after, 'utf8');

console.log(`Reordered ${edges.length} edge cells behind ${others.length} non-edge cells in ${path.basename(file)}`);
