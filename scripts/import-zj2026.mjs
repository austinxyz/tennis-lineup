// Bulk import ZJ 2026 teams from docs/import/*.csv
// Usage: node scripts/import-zj2026.mjs

import fs from 'fs';
import path from 'path';
import { fileURLToPath } from 'url';

const __dirname = path.dirname(fileURLToPath(import.meta.url));
const IMPORT_DIR = path.join(__dirname, '../docs/import');
const API_BASE = 'http://localhost:8080';

async function post(url, body) {
  const res = await fetch(`${API_BASE}${url}`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(body),
  });
  if (!res.ok) throw new Error(`${res.status} ${await res.text()}`);
  return res.json();
}

async function uploadCSV(teamId, csvPath) {
  const blob = new Blob([fs.readFileSync(csvPath)], { type: 'text/csv' });
  const form = new FormData();
  form.append('file', blob, path.basename(csvPath));

  const res = await fetch(`${API_BASE}/api/teams/${teamId}/import`, {
    method: 'POST',
    body: form,
  });
  if (!res.ok) throw new Error(`${res.status} ${await res.text()}`);
  return res.json();
}

async function main() {
  const csvFiles = fs.readdirSync(IMPORT_DIR).filter(f => f.endsWith('.csv'));
  console.log(`Found ${csvFiles.length} team CSVs to import\n`);

  let totalTeams = 0, totalPlayers = 0, totalErrors = 0;

  for (const file of csvFiles) {
    const teamName = file.replace('.csv', '');
    const csvPath = path.join(IMPORT_DIR, file);

    // Create team
    let team;
    try {
      team = await post('/api/teams', { name: teamName });
    } catch (e) {
      console.error(`✗ Create team [${teamName}]: ${e.message}`);
      totalErrors++;
      continue;
    }

    // Upload CSV
    try {
      const result = await uploadCSV(team.id, csvPath);
      console.log(`✓ ${teamName}: ${result.successCount} players imported${result.failureCount > 0 ? `, ${result.failureCount} failed` : ''}`);
      if (result.errors?.length) result.errors.forEach(e => console.log(`    ! ${e}`));
      totalPlayers += result.successCount;
      totalErrors += result.failureCount;
    } catch (e) {
      console.error(`✗ Import [${teamName}]: ${e.message}`);
      totalErrors++;
    }

    totalTeams++;
  }

  console.log(`\nDone: ${totalTeams} teams, ${totalPlayers} players imported, ${totalErrors} errors`);
}

main().catch(e => { console.error(e); process.exit(1); });
