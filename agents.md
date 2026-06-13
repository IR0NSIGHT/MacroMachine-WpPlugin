# Agents — Frontend (webUI)

Purpose

This document describes recommended agent usage, prompt templates, and conventions for the frontend application located in the `webUI` folder. It is intended to help engineers (and automated agents) perform common frontend tasks safely and consistently.

Repository layout (frontend)

- `webUI` — frontend app (React + TypeScript). See [webUI](webUI).
- Important files:
  - [webUI/src](webUI/src) — application source code.
  - [webUI/src/App.tsx](webUI/src/App.tsx) — main app component.
  - [webUI/package.json](webUI/package.json) — scripts and dependencies.

Agent use cases (recommended)

- Local development: start the dev server and open the app.
- Format & lint: run code formatting and lint checks and optionally apply fixes.
- Verify: run the full verification pipeline (`npm run verify`).
- Build: create a production build.
- Tests / stories: run unit and storybook checks.
- PR assistance: summarize diffs, generate PR titles / descriptions, list tests to run.

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
- Non-destructive first: run checks (`npm run verify`) and report results before applying fixes or committing changes.
- When auto-fixing (format/lint), first run with the fixer, then run `npm run verify` again and include the changed file list in the agent report.
- Avoid changing backend or server-side code unless the request explicitly includes cross-repo updates.
- Never expose secrets or credentials in prompts or outputs.

Prompt templates (examples)

- Run verification and report failures:

"Run `cd webUI && npm run verify`. If the command fails, return a concise list of failing files, the failing command or rule, and a suggested one-line fix for each failure."

- Auto-format and re-run verification:

"Run `cd webUI && npm run fmt` (or the project formatter). Then run `npm run verify` and report whether formatting/linting/compile pass now. List modified files." 

- Summarize proposed changes for a PR:

"Given the diff, produce a PR title, a short description (3–4 bullets) of what changed, and a checklist of tests/commands to run (e.g. `cd webUI && npm run verify`)."

Agent output format (recommended)

- Start with a one-line status (OK / FAIL).
- Provide a short summary paragraph (1–2 sentences).
- Bullet list of actionable items (failing files, commands to run, suggested fixes).
- If the agent made changes, include a precise list of modified files and the git commands needed to review and commit them.

Troubleshooting

- `ENOENT: Could not read package.json` — ensure the agent runs from `webUI` or use `cd webUI` before npm commands.
- Formatting issues reported by `oxfmt` — run the formatter without `--check` to auto-apply fixes or run `npm run fmt` if present.
- Lint failures — open the first error reported by the linter and suggest minimal code changes.

Adding new agents or tasks

- Store reusable agent prompts or templates in `docs/agents/` or `.agents/` (create these folders if desired).
- Keep prompts short, deterministic, and include the working directory and exact commands to run.

Contact / Ownership

- If you want me to create automated agent configurations (for a specific agent framework or CI), tell me which platform you use (e.g., GitHub Actions, internal agent runner) and I will scaffold config files.

---

This file focuses on frontend (`webUI`) tasks. If you want a separate section for backend/CI or a set of actual agent scripts/configs, tell me which agent platform to target and I will extend this file accordingly.
