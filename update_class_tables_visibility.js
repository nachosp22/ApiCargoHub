const fs = require('fs');
const path = require('path');

const file = path.join(__dirname, 'TFG_CargoHub_clases_tablas.md');
const lines = fs.readFileSync(file, 'utf8').split(/\r?\n/);
const out = [];

for (let i = 0; i < lines.length; i++) {
  const line = lines[i];

  if (line.trim() === '| Atributo | Tipo |' && lines[i + 1]?.trim() === '|---|---|') {
    out.push('| Visibilidad UML | Atributo | Tipo |');
    out.push('|---|---|---|');
    i += 1;
    while (i + 1 < lines.length && /^\|.*\|$/.test(lines[i + 1].trim())) {
      const row = lines[++i].trim();
      const parts = row.split('|').map(p => p.trim()).filter(Boolean);
      if (parts.length === 2) {
        out.push(`| - privado | ${parts[0]} | ${parts[1]} |`);
      } else {
        out.push(row);
      }
    }
    continue;
  }

  if (line.trim() === '| Metodo | Descripcion |' && lines[i + 1]?.trim() === '|---|---|') {
    out.push('| Visibilidad UML | Metodo | Descripcion |');
    out.push('|---|---|---|');
    i += 1;
    while (i + 1 < lines.length && /^\|.*\|$/.test(lines[i + 1].trim())) {
      const row = lines[++i].trim();
      const parts = row.split('|').map(p => p.trim()).filter(Boolean);
      if (parts.length === 2) {
        const vis = /^Sin metodos/.test(parts[0]) ? 'No aplica' : '+ publico';
        out.push(`| ${vis} | ${parts[0]} | ${parts[1]} |`);
      } else {
        out.push(row);
      }
    }
    continue;
  }

  out.push(line);
}

let content = out.join('\n');
content = content.replace(
  'Este documento complementa los diagramas UML. El diagrama reducido queda limpio, sin atributos ni metodos; el detalle se recoge aqui clase por clase.',
  'Este documento complementa los diagramas UML. El diagrama reducido queda limpio, sin atributos ni metodos; el detalle se recoge aqui clase por clase. En UML, los atributos de las entidades se representan como privados (`-`) y los metodos de dominio como publicos (`+`).'
);

fs.writeFileSync(file, content, 'utf8');
console.log('Updated class tables with UML visibility columns');
