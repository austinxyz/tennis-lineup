/**
 * Playwright global teardown — runs once after all tests.
 * Restores the real data file from backup.
 *
 * If tests crashed and left a stale backup, run manually:
 *   mv backend/data/tennis-data.backup.json backend/data/tennis-data.json
 */
import { fileURLToPath } from 'url'
import { dirname, resolve } from 'path'
import { copyFileSync, unlinkSync, existsSync } from 'fs'

const __dirname = dirname(fileURLToPath(import.meta.url))
const dataFile = resolve(__dirname, '../../backend/data/tennis-data.json')
const backupFile = resolve(__dirname, '../../backend/data/tennis-data.backup.json')

export default async function globalTeardown() {
  if (existsSync(backupFile)) {
    copyFileSync(backupFile, dataFile)
    unlinkSync(backupFile)
  }
}
