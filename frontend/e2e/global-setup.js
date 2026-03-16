/**
 * Playwright global setup — runs once before all tests.
 * Backs up the real data file and replaces it with an empty one,
 * so E2E tests run against a clean slate without touching real data.
 */
import { fileURLToPath } from 'url'
import { dirname, resolve } from 'path'
import { copyFileSync, writeFileSync, existsSync } from 'fs'

const __dirname = dirname(fileURLToPath(import.meta.url))
const dataFile = resolve(__dirname, '../../backend/data/tennis-data.json')
const backupFile = resolve(__dirname, '../../backend/data/tennis-data.backup.json')

export default async function globalSetup() {
  if (existsSync(dataFile)) {
    copyFileSync(dataFile, backupFile)
  }
  writeFileSync(dataFile, JSON.stringify({ teams: [] }, null, 2), 'utf-8')
}
