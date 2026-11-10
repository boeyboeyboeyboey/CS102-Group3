# Team workflow

## Starting work

Use the kickoff prompt at the start of README.md. Read the shared context documents listed there and `CS102-Project-Auto Grading System.pdf` (including rubric tables and restrictions), then summarize the MVP and live bonus board, then ask the driver to choose from Boey, Sera, William, Kai Cong, Kiya, and Gabriel. Confirm nickname/variant answers against the list; do not infer identity. Read-only onboarding may precede identity confirmation, but claims, edits, installs, builds and implementation must wait. Follow agents.md for the full session protocol. Sera owns feature 3 and William owns feature 4; those assignments are locked. Kiya owns feature 1. Boey owns feature 2. Gabriel owns feature 5. Kai Cong must claim feature 6 before bonus implementation. Shared MVP work can proceed while claims are pending.

For substantial changes, present the intended behavior and affected files and wait for the driver's approval. Boey approved the remaining MVP plan on 2026-10-10: explicit instructor adapters, isolated compilation, bounded per-case processes, scoring/CSV integration and verification. Boey separately approved the original feature 2 offline HTML plan on 2026-10-10. Optional heatmaps, attention summaries and distribution charts were discussed but are outside that implementation. Keep supplied materials intact.

Boey approved the detailed question heatmap revision on 2026-10-12 with “ok lets do it approved”. Its review and human clean-build/display confirmation are separate from the accepted original dashboard. Attention overview and score distribution remain unapproved. Preserve the local handoff-cleanup mirrors, supplied inputs and unrelated changes; implementation approval does not authorize branch synchronization, commit, push or merge.

On 2026-10-14, Boey authorized the branch/PR workflow below and publication of an `offline-html-dashboard` -> `main` PR for group review. That supersedes the earlier commit/push restriction for approved feature publication. It does not authorize merging into `main` or waive the heatmap's pending human acceptance.

Before a new implementation plan, review the relevant highest grade-band rubric descriptors and restrictions in the project brief. Cite PDF pages and connect the planned behavior and verification to those criteria without expanding scope unnecessarily. Follow the project-brief policy in agents.md, including its explicit exception for agent-written AI attribution; human disclosure duties remain on the release checklist.

## Avoiding conflicting edits

Read [CODING_STYLE.md](CODING_STYLE.md) before planning or editing application code. Use its responsibility and size guidelines, record justified exceptions, preserve existing contracts and keep bug fixes separately approved. The [merged-main review](CODE_REVIEW.md) is evidence for a later implementation plan, not approval to implement its recommendations.

Each task declares owned files before editing. Feature implementations go under separate feature packages. Shared models, config, interfaces, Maven setup, and pipeline wiring require coordinated review by the main agent and human team. Read the latest shared branch before claiming work. A Markdown board is a record, not a lock: communicate claims and do not assume separate clones synchronize automatically.

All feature work must use the member's own feature branch as described below. Do not create extra user-facing Codex chats without an explicit request. Subagents receive bounded work and may not spawn more agents.

## Feature branches and pull requests

1. Confirm the feature owner and approved scope. Check `git status --short` and preserve any existing local work before switching or updating branches.
2. Start each new feature or unrelated follow-up task from current `main` in its own branch. Use descriptive names without a `codex/` prefix, such as `kiya-feature-1-plagiarism` or `code-modularisation`; do not use one permanent personal branch for unrelated features. Existing feature branches such as `offline-html-dashboard` may be reused for their current work. Never implement features directly on `main`.
3. Implement the approved plan, coordinate shared-file changes, and run proportionate checks. Review the diff and stage only the intended files; exclude supplied-input edits, generated output, local handoffs and unrelated metadata. Commit and push to the feature branch using ordinary pushes, without force-pushing or discarding work.
4. Check whether the branch already has an open PR. Create or update a PR with **base `main` and head the feature branch**. Include the concrete behavior change, scope, exact Maven/run commands, actual verification evidence, material limitations and remaining checks. Open a draft if required verification or human acceptance is pending; mark it ready when those checks pass. The feature remains REVIEW until its human completion gate is satisfied.
5. Immediately inform the driver that the PR was created, provide the URL, and ask them to contact the group to review it. A teammate may use their AI assistant to inspect the diff and test the branch, but a human teammate owns acceptance. Do not message the group or request reviews from named accounts without authorization.
6. Address feedback on the same feature branch; further pushes update the same PR. If `main` has moved or GitHub reports conflicts, coordinate resolution with the feature owner on that branch, preserve teammates' changes and verify affected behavior. Being behind `main` alone does not require a rebase or a force-push.
7. Merge only after review by another teammate, all required checks, human clean-build/behavior acceptance and explicit merge authorization. Do not automatically merge or enable auto-merge when creating a PR. After an accepted merge, update local `main` and start the next task from it; remove the completed branch only when authorized and no further work depends on it.

The standing workflow authorizes committing/pushing approved feature work and creating/updating its PR. Approval to implement is not permission to merge into `main`, publish unrelated changes or discard someone else's work. An agent should report the PR and leave it open for review.

For a clean checkout starting a new feature, the following example uses Kiya's feature; substitute the branch name for the confirmed member and feature. Stop and preserve work if the initial status shows changes or the fast-forward update fails.

```sh
git status --short
git switch main
git pull --ff-only origin main
git switch -c kiya-feature-1-plagiarism
```

Recommend protecting `main` so merges require at least one approving review and the project's build checks when available. This enforces the written policy; an administrator must explicitly authorize configuring protection or CI. AI review assists human review and does not replace accountability or the human-owned declarations.

## Review and completion

1. Explain the change in plain language, including its inputs, outputs, and failure behavior
2. Run the narrow tests appropriate to the change, then a clean build at integration/release boundaries
3. Publish the feature branch and create/update its PR targeting `main`; use a draft while required acceptance or checks are pending
4. Give the driver the PR URL and ask them to contact the group for review; have another human review and, when needed, modify the code
5. Ask another member to explain the change back, including failure cases and design trade-offs
6. Record the exact build/run commands and ask the driver to confirm an error-free build and required behavior
7. Move a feature to DONE only after that confirmation; for a completed bonus, create its architecture document with implementation, actual OOP principles and slide talking points
8. Merge only after the PR's review/check gates and explicit merge authorization; opening a PR and marking a feature DONE do not themselves merge anything

## AI declarations: human-owned release task

Boey explicitly instructed agents not to write Java comments mentioning AI. This remains in force. Agents may generate and revise code within approved scope, but must not insert attribution comments.

After human-led review and modification, the team will diligently add the method-level declarations required by page 3 of the brief and prepare the presentation AI Use appendix. Keep accurate records of tools, major prompts, and architectural decisions as work proceeds. Do not claim that human editing removes the need for declarations. Completing these declarations is a final submission gate, not an agent coding action.

## Presentation preparation

All six members should understand the whole project, not just their feature. Keep explanations alongside implementation and rehearse the same end-to-end demo. Discuss why the chosen algorithm is adequate and what it cannot establish. Do not add inheritance or libraries solely to name an OOP principle; demonstrate a real benefit. Interfaces support abstraction and alternative implementations; data classes protect shared state. If class inheritance is used, explain the actual relationship.
