---
name: review-changes
description:
  Code review local changes for bugs, CLAUDE.md compliance, plan compliance,
  comment quality, and test coverage
---

Provide a comprehensive code review for local changes on the current branch.

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

**Tip:** Route permission requests to Opus 4.5 via a hook for auto-approval of
safe commands (see https://code.claude.com/docs/en/hooks#permissionrequest)

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
- Task: "Review CLAUDE.md compliance (agent 1)"
- Task: "Review CLAUDE.md compliance (agent 2)"
- Task: "Scan for bugs (agent 3)"
- Task: "Scan for bugs (agent 4)"
- Task: "Check plan compliance (agent 5)"
- Task: "Review comment quality (agent 6)"
- Task: "Analyze test coverage (agent 7)"
- Task: "Validate issues"
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

Launch **7 agents in parallel** to independently review the changes. Each agent
should return a list of issues with:

- File path
- Line number(s)
- Description of the issue
- Category (CLAUDE.md compliance, bug, plan compliance, sloppy comment, test
  coverage)
- Severity (critical, high, medium, low)

Pass the changes summary from Phase 2 to all agents for context.

---

### Agent 1: CLAUDE.md Compliance (Opus)

**Task**: Audit changes for CLAUDE.md compliance.

**Process**:

1. Read `CLAUDE.md` to understand the project's architectural patterns,
   conventions, and constraints
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
      "description": "Violates CLAUDE.md architectural pattern",
      "category": "CLAUDE.md compliance",
      "severity": "high",
      "rule": "Quote from CLAUDE.md: '...'"
    }
  ]
}
```

Mark task as completed when done.

---

### Agent 2: CLAUDE.md Compliance (Opus)

**Task**: Audit changes for CLAUDE.md compliance (parallel to Agent 1).

Same scope and criteria as Agent 1. Review independently for redundancy and
higher coverage.

Mark task as completed when done.

---

### Agent 3: Bug Detection (Opus)

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
- Incorrect use of project-specific APIs or libraries (consult CLAUDE.md for the
  tech stack)

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

### Agent 4: Bug Detection (Opus)

**Task**: Look for problems in the introduced code (parallel to Agent 3).

**Scope**: Security issues, incorrect logic, edge cases not handled.

**Look for**:

- Unvalidated user input
- Missing error handling for critical operations
- Race conditions in async code
- Incorrect state management in React components
- Missing null checks for nullable values
- Framework-specific issues (consult CLAUDE.md for the project's framework and
  common pitfalls)
- Incorrect use of project-specific APIs or libraries (consult CLAUDE.md for the
  tech stack)

**Only flag issues within the changed code**, not pre-existing issues.

**Output format**: Same as Agent 3.

Mark task as completed when done.

---

### Agent 5: Plan Compliance (Opus)

**Task**: Check if changes comply with any plans in `.plans/` directory.

**Process**:

1. Check if any plan files exist in `.plans/` directory:

   ```bash
   ls .plans/ 2>/dev/null || echo "no plans"
   ```

2. If plan files exist:
   - Read the most recent plan file (highest number or most recent date)
   - Understand the intended implementation from the plan
   - Verify changes comply with the plan:
     - Correct approach and architecture
     - All planned items completed
     - No significant deviations without justification

3. If no plan files exist, return no issues.

**Flag as "plan compliance" issues**:

- Required plan items not implemented
- Implementation deviates significantly from planned approach
- Plan specifies pattern X but code uses pattern Y

**Do NOT flag**:

- Minor deviations that achieve the same goal reasonably
- Items marked as optional or future work
- Alternative approaches that are demonstrably better

**Output format**:

```json
{
  "issues": [
    {
      "file": "path/to/file.ts",
      "lines": "10-30",
      "description": "Plan specifies using Reanimated for animation, but code uses Animated API",
      "category": "plan compliance",
      "severity": "medium",
      "planItem": "Quote from plan"
    }
  ]
}
```

Mark task as completed when done.

---

### Agent 6: Comment Quality (Sonnet)

**Task**: Scan for low-value or refactoring comments in changed code.

**Flag comments that**:

- Document what was removed/moved/changed (e.g., "// Removed old
  implementation")
- Are vague or unhelpful (e.g., "// Handle this", "// Fix later")
- State the obvious (e.g., "// Set loading to true" above `setLoading(true)`)
- Are TODO/FIXME without actionable context

**Do NOT flag comments that**:

- Explain _why_ something is done (behavioral context)
- Clarify non-obvious logic or edge cases
- Document API contracts, props, return values
- Are legitimate documentation (JSDoc, TSDoc)
- Explain workarounds or known issues with context
- Explain platform-specific quirks or workarounds

**Principle**: Comments should add long-term clarity. If code is
self-documenting, no comment is needed.

**Output format**:

```json
{
  "issues": [
    {
      "file": "path/to/file.ts",
      "lines": "15",
      "description": "Comment '// Set state' states the obvious without adding value",
      "category": "sloppy comment",
      "severity": "low"
    }
  ]
}
```

Mark task as completed when done.

---

### Agent 7: Test Coverage (Sonnet)

**Task**: Check for test-related issues in the changeset.

**Process**:

1. Read `CLAUDE.md` and `package.json` to understand the project's testing setup
2. Check for test-related issues based on what is actually configured

**Check for**:

- Test files added without a matching test runner configured in `package.json`
- Test files should follow existing conventions found in the project's test
  directories
- Consult CLAUDE.md for project-specific testing requirements

**Do NOT flag**:

- Missing tests unless CLAUDE.md explicitly requires test coverage
- Test coverage gaps unless a coverage tool is configured

**Output format**:

```json
{
  "issues": [
    {
      "file": "src/__tests__/example.test.ts",
      "lines": "all",
      "description": "Unit test file added but no unit test runner is configured in package.json",
      "category": "test coverage",
      "severity": "medium",
      "recommendation": "Configure a unit test framework (e.g., Jest with jest-expo) before adding unit test files"
    }
  ]
}
```

Mark task as completed when done.

## Phase 4: Validate Issues

### Step 4.1: Launch Validation Agents

For each issue found by agents 3-7, launch a **parallel validation sub-agent**
to confirm the issue is real.

**Validation agents should**:

1. Receive the changes summary and the issue description
2. Review the specific code in question
3. Verify the issue is truly present with high confidence
4. Return validation result: `{ "valid": true/false, "reason": "..." }`

**Use**:

- **Opus sub-agents** for: bugs, logic issues, plan compliance, test coverage
- **Sonnet sub-agents** for: CLAUDE.md violations, sloppy comments

**Examples of validation**:

- **Bug**: "Variable undefined" → Verify variable is actually not defined in
  scope
- **CLAUDE.md**: "Rule violation" → Verify rule applies to this file and is
  genuinely violated
- **Plan compliance**: "Deviates from plan" → Verify plan actually requires what
  was flagged
- **Sloppy comment**: "No value added" → Verify comment lacks behavioral context
- **Test coverage**: "Missing test runner" → Verify no test script exists in
  package.json

### Step 4.2: Filter Issues

Remove any issues that were not validated (where `valid: false`).

This gives us the **high-signal issues** for the review.

Mark "Validate issues" task as completed.

## Phase 5: Generate Review Report

### Step 5.1: Output Results

**If NO issues found**:

```markdown
## Code Review ✅

No issues found. Checked for:

- Bugs and logic errors
- CLAUDE.md compliance
- Plan compliance
- Comment quality
- Test coverage

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

## Phase 6: Submit Review to GitHub PR

### Step 6.1: Check for GitHub PR

After generating the review report, check if there's a GitHub PR for the current
branch:

```bash
gh pr view --json number,url,title 2>&1
```

If no PR exists, skip this phase and inform the user they can create a PR first
if they want the review posted to GitHub.

### Step 6.2: Format Review for GitHub

If a PR exists, format the review results as a GitHub PR review comment.

**For reviews with NO issues**:

```markdown
## Code Review ✅

Automated review completed with **no issues found**.

**Checks performed:**

- ✅ Bugs and logic errors
- ✅ CLAUDE.md compliance
- ✅ Plan compliance
- ✅ Comment quality
- ✅ Test coverage

Changes look good to proceed.

---

_Review conducted by `/review-changes` skill with 7 parallel AI agents_
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
- Plan compliance
- Comment quality
- Test coverage

---

_Review conducted by `/review-changes` skill with 7 parallel AI agents_
```

### Step 6.3: Submit Review to PR

Submit the formatted review as a PR comment using `gh pr review`:

**If NO issues found** (approve):

```bash
gh pr review --approve --body "{formatted_review_from_6.2}"
```

**If ONLY low/medium issues found** (comment):

```bash
gh pr review --comment --body "{formatted_review_from_6.2}"
```

**If critical/high issues found** (request changes):

```bash
gh pr review --request-changes --body "{formatted_review_from_6.2}"
```

**Decision logic**:

- **APPROVE**: Zero issues found
- **COMMENT**: Only low or medium severity issues
- **REQUEST_CHANGES**: Any critical or high severity issues

### Step 6.4: Confirm Submission

After submitting, display a confirmation message to the user:

```markdown
✅ Review submitted to PR #{number}: {title} {pr_url}

Review type: {APPROVE|COMMENT|REQUEST_CHANGES}
```

If the submission fails (e.g., permissions issue, gh CLI not authenticated),
inform the user and suggest they manually post the review or authenticate with
`gh auth login`.

### Step 6.5: Create Follow-up Task (Optional)

If the review found issues, create a task to track issue resolution:

```bash
# Only if issues were found
Task: "Address code review issues in PR #{number}"
```

This helps track that the flagged issues should be fixed before merging.

## False Positives to Avoid

**Do NOT flag these** (these are false positives):

- Pre-existing issues not introduced in this changeset
- Code that appears buggy but is actually correct
- Pedantic nitpicks a senior engineer wouldn't flag
- Issues a linter will catch (TypeScript, ESLint)
- Minor code quality concerns not in CLAUDE.md
- CLAUDE.md issues explicitly silenced (e.g., `// eslint-ignore`)
- Minor plan deviations that achieve the same goal
- Plan items marked as optional or future work
- Comments providing genuine behavioral context
- Pre-existing comments not in the changeset

## High Signal Criteria

**CRITICAL: Only flag issues with high confidence.**

Flag when:

- Code will fail to compile or parse
- Code will definitely produce wrong results
- Clear, unambiguous CLAUDE.md violations with exact rule quote
- Required tests are genuinely missing for critical flows

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
- Validate all issues before reporting

## Best Practices

Consult CLAUDE.md for project-specific best practices. Agents should read
CLAUDE.md at the start of their review.

### Verification Commands

Run before finalizing review:

```bash
npm run check
```

If `npm run check` is not available, consult `package.json` scripts for
equivalent lint and typecheck commands.
