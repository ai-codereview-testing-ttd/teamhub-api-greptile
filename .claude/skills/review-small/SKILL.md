---
name: review-small
description:
  Streamlined code review for small, straightforward PRs (< 200 lines, < 5
  files, low complexity)
---

Provide a focused code review for small, straightforward changes.

## When to Use This Skill

Use `/review-small` for:

- Small bug fixes (< 200 lines changed)
- Simple feature additions (1-2 new files, straightforward logic)
- Documentation updates
- Configuration changes
- Refactors with repetitive patterns across multiple files

Use `/review-changes` (full review) instead for:

- New architectural patterns or native modules
- Complex state management changes
- Multi-phase features with intricate logic
- Security-sensitive changes (auth, VPN, permissions)
- Changes that introduce new dependencies or APIs

## Pre-flight Check

Before starting, ensure you are working from the repository root directory:

- Your working directory must be the repository root (where `CLAUDE.md` and
  `package.json` exist)
- If unsure, run `ls CLAUDE.md package.json` to verify you're in the correct
  directory

**Agent assumptions (applies to all agents and subagents):**

- All tools are functional and will work without error. Do not test tools or
  make exploratory calls.
- Only call a tool if it is required to complete the task. Every tool call
  should have a clear purpose.

## Phase 1: Discover Changes

### Step 1.1: Gather Context

Check for changes in order of preference:

1. Current conversation context (what we've been working on)
2. Git staged changes: `git diff --cached --stat`
3. Committed changes on current branch vs main: `git log main..HEAD --oneline`
   and `git diff main...HEAD --stat`

If there are no changes to review (no staged changes and no commits ahead of
main), inform the user and stop.

### Step 1.2: Create Task List

Before proceeding, create a task list with TaskCreate:

- Task: "Summarize changes"
- Task: "Scan for bugs"
- Task: "Check CLAUDE.md compliance"
- Task: "Generate review report"
- Task: "Submit review to GitHub PR"

## Phase 2: Summarize Changes

### Step 2.1: Launch Summary Agent

Launch a **Sonnet agent** to view the changes and return a summary of what was
modified.

Use the appropriate git diff command based on what changes exist:

- Staged changes: `git diff --cached`
- Committed changes: `git diff main...HEAD`

The agent should return:

- List of modified files with change types (added, modified, deleted)
- Brief description of what changed in each file
- Overall purpose of the changeset

Mark the "Summarize changes" task as completed after receiving the summary.

## Phase 3: Parallel Code Review

### Step 3.1: Launch Review Agents

Launch **2 agents in parallel** to independently review the changes. Each agent
should return a list of issues with:

- File path
- Line number(s)
- Description of the issue
- Category (bug, CLAUDE.md compliance)
- Severity (critical, high, medium, low)

Pass the changes summary from Phase 2 to all agents for context.

---

### Agent 1: Bug Detection (Opus)

**Task**: Scan for obvious bugs in the diff itself.

**Focus only on the diff** without reading extra context. Flag only significant
bugs; ignore nitpicks and likely false positives.

**Look for**:

- Syntax errors, type errors, undefined variables
- Null/undefined dereferences
- Logic errors (wrong operators, incorrect conditionals)
- Missing imports or incorrect module paths
- React hooks used incorrectly (e.g., hooks in conditionals)
- Async/await issues (missing await, unhandled promises)
- Off-by-one errors, incorrect array access
- Framework-specific issues (consult CLAUDE.md for the project's framework and
  common pitfalls)

**Do NOT flag**:

- Style issues
- Potential bugs that require broader context
- Performance concerns
- Issues that depend on specific inputs

**Only flag if you are certain** the code will fail or produce wrong results.

**Output format**:

```json
{
  "issues": [
    {
      "file": "path/to/file.ts",
      "lines": "23",
      "description": "Variable 'userId' is undefined - should be 'user.id'",
      "category": "bug",
      "severity": "critical"
    }
  ]
}
```

Mark task as completed when done.

---

### Agent 2: CLAUDE.md Compliance (Sonnet)

**Task**: Audit changes for CLAUDE.md compliance.

**Scope**: Review all changed files for violations of guidelines in CLAUDE.md.

**Process**:

1. Read `CLAUDE.md` to understand the project's architectural patterns and
   conventions
2. Review all changed files for violations

**Check for**:

- Non-compliance with architectural patterns documented in CLAUDE.md
- Violation of naming conventions, file organization, or module patterns in
  CLAUDE.md
- Incorrect use of project-specific APIs or libraries as documented in CLAUDE.md
- Violation of environment setup or configuration requirements in CLAUDE.md

**Only flag issues where**:

- You can quote the exact CLAUDE.md rule being violated
- The violation is unambiguous and clear
- The issue is within the changed code (not pre-existing)

**Output format**:

```json
{
  "issues": [
    {
      "file": "path/to/file.ts",
      "lines": "45-50",
      "description": "Provider order violates CLAUDE.md composition order",
      "category": "CLAUDE.md compliance",
      "severity": "high",
      "rule": "Quote from CLAUDE.md: 'Providers wrap the app in this order: SafeAreaProvider → PostHogProvider → ClerkProvider → ConvexProvider'"
    }
  ]
}
```

Mark task as completed when done.

---

## Phase 4: Generate Review Report

### Step 4.1: Output Results

**If NO issues found**:

```markdown
## Code Review ✅

No issues found. Checked for:

- Bugs and logic errors
- CLAUDE.md compliance

Changes look good to proceed.
```

**If issues found**, output each issue with:

```markdown
## Code Review

Found {count} issue(s) requiring attention:

### {Severity}: {Category}

**File**: `path/to/file.ts:{lines}`

**Issue**: {description}

**Recommendation**: {suggested fix or approach}

---
```

Group issues by severity (Critical → High → Medium → Low).

For each issue:

- Include file path and line numbers (clickable in IDE)
- Brief description
- Category and severity
- For small fixes, include suggested code change
- For larger fixes, describe the issue and recommended approach
- For CLAUDE.md violations, quote the violated rule

Mark "Generate review report" task as completed.

## Phase 5: Submit Review to GitHub PR

### Step 5.1: Check for GitHub PR

After generating the review report, check if there's a GitHub PR for the current
branch:

```bash
gh pr view --json number,url,title 2>&1
```

If no PR exists, skip this phase and inform the user they can create a PR first
if they want the review posted to GitHub.

### Step 5.2: Format Review for GitHub

If a PR exists, format the review results as a GitHub PR review comment.

**For reviews with NO issues**:

```markdown
## Code Review ✅

Automated review completed with **no issues found**.

**Checks performed:**

- ✅ Bugs and logic errors
- ✅ CLAUDE.md compliance

Changes look good to proceed.

---

_Streamlined review conducted by `/review-small` skill (2 AI agents)_
```

**For reviews with issues**, format as:

```markdown
## Code Review - {issue_count} Issue(s) Found

{For each severity level with issues:}

### {Severity} Priority

{For each issue at this severity:}

**{Category}** in `{file}:{lines}`

{description}

**Recommendation:** {recommendation}

---

{End of issues}

**Summary:**

- Critical: {count}
- High: {count}
- Medium: {count}
- Low: {count}

**Checks performed:**

- Bugs and logic errors
- CLAUDE.md compliance

---

_Streamlined review conducted by `/review-small` skill (2 AI agents)_
```

### Step 5.3: Submit Review to PR

Submit the formatted review as a PR comment using `gh pr review`:

**If NO issues found** (approve):

```bash
gh pr review --approve --body "{formatted_review_from_5.2}"
```

**If ONLY low/medium issues found** (comment):

```bash
gh pr review --comment --body "{formatted_review_from_5.2}"
```

**If critical/high issues found** (request changes):

```bash
gh pr review --request-changes --body "{formatted_review_from_5.2}"
```

**Decision logic**:

- **APPROVE**: Zero issues found
- **COMMENT**: Only low or medium severity issues
- **REQUEST_CHANGES**: Any critical or high severity issues

### Step 5.4: Confirm Submission

After submitting, display a confirmation message to the user:

```markdown
✅ Review submitted to PR #{number}: {title} {pr_url}

Review type: {APPROVE|COMMENT|REQUEST_CHANGES}
```

If the submission fails (e.g., permissions issue, gh CLI not authenticated),
inform the user and suggest they manually post the review or authenticate with
`gh auth login`.

## False Positives to Avoid

**Do NOT flag these** (these are false positives):

- Pre-existing issues not introduced in this changeset
- Code that appears buggy but is actually correct
- Pedantic nitpicks a senior engineer wouldn't flag
- Issues a linter will catch (TypeScript, ESLint)
- Minor code quality concerns not in CLAUDE.md
- CLAUDE.md issues explicitly silenced (e.g., `// eslint-ignore`)
- Pre-existing comments not in the changeset

## High Signal Criteria

**CRITICAL: Only flag issues with high confidence.**

Flag when:

- Code will fail to compile or parse
- Code will definitely produce wrong results
- Clear, unambiguous CLAUDE.md violations with exact rule quote

Do NOT flag:

- Style or quality concerns
- Potential issues dependent on specific inputs
- Subjective suggestions
- Uncertain issues

**If not certain, do not flag it.** False positives waste reviewer time and
erode trust.

## Notes

- Create a task list before starting (use TaskCreate)
- Update task status as you progress (use TaskUpdate)
- When citing CLAUDE.md rules, quote the exact rule
- Use `git diff --cached` for staged changes
- Use `git diff main...HEAD` for committed changes
- Launch review agents in parallel for efficiency
