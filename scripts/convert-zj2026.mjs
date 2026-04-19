// Convert ZJ 2026 CSV to per-team import CSVs
// Usage: node scripts/convert-zj2026.mjs
// Output: docs/import/<teamName>.csv

import fs from 'fs';
import path from 'path';
import { fileURLToPath } from 'url';

const __dirname = path.dirname(fileURLToPath(import.meta.url));
const CSV_PATH = path.join(__dirname, '../docs/ZJ 2026 - Internal - all.csv');
const OUT_DIR = path.join(__dirname, '../docs/import');

const content = fs.readFileSync(CSV_PATH, 'utf8');
const lines = content.split('\n').map(l => l.trim()).filter(l => l);

const teams = new Map();

for (const line of lines) {
  // Parse CSV respecting quoted fields
  const fields = [];
  let cur = '', inQ = false;
  for (const ch of line) {
    if (ch === '"') { inQ = !inQ; }
    else if (ch === ',' && !inQ) { fields.push(cur); cur = ''; }
    else { cur += ch; }
  }
  fields.push(cur);

  // Columns: [empty, Team, LastName, FirstName, Gender, DUTRStatus, MatchUTR]
  if (fields.length < 7) continue;
  const teamName = fields[1].trim();
  const lastName = fields[2].trim();
  const firstName = fields[3].trim();
  const genderRaw = fields[4].trim().toLowerCase();
  const utrStr = fields[6].trim();

  if (!teamName || teamName === 'Team' || !lastName || isNaN(parseFloat(utrStr))) continue;

  const gender = genderRaw === 'm' ? 'male' : 'female';
  const utr = parseFloat(utrStr);
  const name = `${firstName} ${lastName}`.trim();
  const status = fields[5].trim();
  const verified = status === 'Rated';

  if (!teams.has(teamName)) teams.set(teamName, []);
  teams.get(teamName).push({ name, gender, utr, verified });
}

fs.mkdirSync(OUT_DIR, { recursive: true });

for (const [teamName, players] of teams) {
  const rows = ['name,gender,utr,verified'];
  for (const p of players) {
    rows.push(`${p.name},${p.gender},${p.utr},${p.verified}`);
  }
  const fileName = `${teamName}.csv`;
  fs.writeFileSync(path.join(OUT_DIR, fileName), rows.join('\n'), 'utf8');
  console.log(`${teamName}: ${players.length} players → docs/import/${fileName}`);
}

console.log(`\nDone. ${teams.size} teams exported to ${OUT_DIR}`);
