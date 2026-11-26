import { test } from 'node:test';
import assert from 'node:assert/strict';
import { installerReport } from '../src/content.mjs';
import { getInstallerScan, parseInstallerScan } from '../src/virustotal.mjs';
import { renderInstallerReport } from '../src/templates.mjs';

const analysedAt = '2026-11-24T12:00:00.000Z';
function report({ malicious = 0, suspicious = 0, harmless = 1, undetected = 69 } = {}) {
  return { data: { id: installerReport.sha256, attributes: {
    sha256: installerReport.sha256,
    last_analysis_date: Date.parse(analysedAt) / 1000,
    last_analysis_stats: { malicious, suspicious, harmless, undetected, timeout: 2, failure: 1, 'type-unsupported': 4 },
  } } };
}

test('no API key means no network request', async () => {
  const result = await getInstallerScan({ apiKey: '', fetchReport: async () => { assert.fail('unexpected request'); } });
  assert.equal(result, null);
});

test('uses the authenticated file-info GET endpoint and returns only public report fields', async () => {
  const result = await getInstallerScan({ apiKey: 'test-build-only-secret', fetchReport: async (url, options) => {
    assert.equal(url, `https://www.virustotal.com/api/v3/files/${installerReport.sha256}`);
    assert.equal(options.method, 'GET');
    assert.equal(options.headers['x-apikey'], 'test-build-only-secret');
    assert.equal(options.redirect, 'error');
    assert.ok(options.signal instanceof AbortSignal);
    return { ok: true, json: async () => report() };
  } });
  assert.deepEqual(result, { sha256: installerReport.sha256, malicious: 0, suspicious: 0, completed: 70, analysedAt });
  assert.doesNotMatch(JSON.stringify(result), /secret|apikey/);
});

test('rejects reports for a different hash', () => {
  const differentId = report();
  differentId.data.id = 'a'.repeat(64);
  assert.equal(parseInstallerScan(differentId), null);
  const differentHash = report();
  differentHash.data.attributes.sha256 = 'b'.repeat(64);
  assert.equal(parseInstallerScan(differentHash), null);
});

test('rejects missing or invalid counts, missing dates, and empty analyses', () => {
  assert.equal(parseInstallerScan(null), null);
  assert.equal(parseInstallerScan({}), null);
  assert.equal(parseInstallerScan(report({ malicious: -1 })), null);
  assert.equal(parseInstallerScan(report({ malicious: '0' })), null);
  assert.equal(parseInstallerScan(report({ malicious: 0.5 })), null);
  assert.equal(parseInstallerScan(report({ harmless: 0, undetected: 0 })), null);
  const noDate = report();
  delete noDate.data.attributes.last_analysis_date;
  assert.equal(parseInstallerScan(noDate), null);
  const invalidDate = report();
  invalidDate.data.attributes.last_analysis_date = Number.MAX_SAFE_INTEGER;
  assert.equal(parseInstallerScan(invalidDate), null);
});

test('API failures, malformed JSON, and network timeouts fall back to a link-only result', async () => {
  for (const status of [401, 403, 404, 429, 500]) {
    assert.equal(await getInstallerScan({ apiKey: 'test', fetchReport: async () => ({ ok: false, status }) }), null);
  }
  assert.equal(await getInstallerScan({ apiKey: 'test', fetchReport: async () => ({ ok: true, json: async () => { throw new SyntaxError('invalid JSON'); } }) }), null);
  assert.equal(await getInstallerScan({ apiKey: 'test', fetchReport: async () => { throw new Error('network unavailable'); } }), null);
});

test('rendered results include their analysis date and do not imply a live or guaranteed-safe verdict', () => {
  const content = renderInstallerReport(parseInstallerScan(report()));
  assert.match(content, /0 malicious · 0 suspicious · 70 completed engine verdicts/);
  assert.ok(content.includes(`datetime="${analysedAt}"`));
  assert.match(content, /24 Nov 2026/);
  assert.match(content, /Results are a snapshot/);
  assert.doesNotMatch(content, /100% safe|virus.free|test-build-only-secret|x-apikey/i);
});

test('flagged results remain visible rather than showing a positive badge', () => {
  const content = renderInstallerReport(parseInstallerScan(report({ malicious: 2, suspicious: 1 })));
  assert.match(content, /scan-flagged/);
  assert.match(content, /2 malicious · 1 suspicious/);
});

test('absent or mismatched scans show only the versioned report and checksum', () => {
  for (const scan of [null, { sha256: 'a'.repeat(64) }]) {
    const content = renderInstallerReport(scan);
    assert.ok(content.includes(installerReport.url));
    assert.ok(content.includes(installerReport.fileName));
    assert.ok(content.includes(installerReport.sha256));
    assert.doesNotMatch(content, /installer-scan|malicious|suspicious/);
  }
});
