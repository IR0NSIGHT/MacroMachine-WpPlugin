# Agents — Frontend (webUI)

Repository layout (frontend)

- `webUI` — frontend app (React + TypeScript). See [webUI](webUI).
- Important files:
  - [webUI/src](webUI/src) — application source code.
  - [webUI/src/App.tsx](webUI/src/App.tsx) — main app component.
  - [webUI/package.json](webUI/package.json) — scripts and dependencies.

Key commands (from repo root)

```bash
cd webUI
npm install
npm run verify    # run format-check, lint and compile
# optional dev flow
npm run dev       # start dev server (if available)
npm run fmt       # run formatter (auto-fix)
npm run lint      # run linter
```

Agent operational guidelines

- Working directory: prefer running commands inside `webUI` (e.g. `cd webUI`). If an agent runs from repo root, always prefix with `cd webUI && ...`.
- Non-destructive first: ALWAYS run checks (`npm run verify`) and report results before doing any changes.
- After every task, run 'npm run verify' and fix upcoming problems
- When auto-fixing (format/lint), first run with the fixer, then run `npm run verify` again and include the changed file list in the agent report.
- Avoid changing backend or server-side code unless the request explicitly includes cross-repo updates.
- Never expose secrets or credentials in prompts or outputs.
