import { installerReport } from './content.mjs';

/** @typedef {{sha256: string, malicious: number, suspicious: number, completed: number, analysedAt: string}} InstallerScan */

/** @param {unknown} report @returns {InstallerScan | null} */
export function parseInstallerScan(report) {
  const data = report?.data;
  const attributes = data?.attributes;
  const stats = attributes?.last_analysis_stats;
  if (data?.id !== installerReport.sha256 || attributes?.sha256 !== installerReport.sha256 || !stats) return null;
  const counts = ['malicious', 'suspicious', 'harmless', 'undetected'].map((name) => stats[name]);
  if (!counts.every((count) => Number.isSafeInteger(count) && count >= 0)) return null;
  const completed = counts.reduce((sum, count) => sum + count, 0);
  const timestamp = attributes.last_analysis_date;
  if (!Number.isSafeInteger(completed) || completed === 0 || !Number.isSafeInteger(timestamp) || timestamp <= 0) return null;
  const analysis = new Date(timestamp * 1000);
  if (!Number.isFinite(analysis.getTime())) return null;
  return { sha256: installerReport.sha256, malicious: stats.malicious, suspicious: stats.suspicious, completed, analysedAt: analysis.toISOString() };
}

/**
 * Fetch only an existing report. Never upload a file, expose the API key, or fail the site build for an unavailable scan.
 * @param {{apiKey?: string, fetchReport?: typeof fetch}} [options]
 * @returns {Promise<InstallerScan | null>}
 */
export async function getInstallerScan({ apiKey = process.env.VIRUSTOTAL_API_KEY, fetchReport = fetch } = {}) {
  if (!apiKey) return null;
  try {
    const response = await fetchReport(`https://www.virustotal.com/api/v3/files/${installerReport.sha256}`, {
      method: 'GET',
      headers: { 'x-apikey': apiKey, accept: 'application/json' },
      signal: AbortSignal.timeout(10000),
      redirect: 'error',
    });
    if (!response.ok) return null;
    return parseInstallerScan(await response.json());
  } catch {
    return null;
  }
}
