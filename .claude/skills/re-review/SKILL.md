---
name: re-review
description: Verify that code review issues from /review-changes have been fixed
---

Verify that issues flagged by `/review-changes` have been addressed. This is a
lightweight follow-up review (2 agents vs 7) that checks specific known issues
rather than scanning from scratch.

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

## Phase 1: Load Context

### Step 1.1: Check for GitHub PR

Verify a PR exists for the current branch:

```bash
gh pr view --json number,url,title,headRefName 2>&1
```

If no PR exists, stop and inform the user: "No PR found for the current branch.
Create a PR first with `/submit-pr`, then run `/review-changes` before using
`/re-review`."

Save the PR number for later use.

### Step 1.2: Fetch the Original Review Comment

Fetch all review comments on the PR and find the most recent one from
`/review-changes`. Extract the owner/repo first:

```bash
REPO=$(gh repo view --json nameWithOwner --jq '.nameWithOwner')
PR_NUMBER={from step 1.1}
```

Then fetch the review:

```bash
gh api "repos/$REPO/pulls/$PR_NUMBER/reviews" \
  --jq '[.[] | select(.body | contains("Review conducted by") and contains("/review-changes"))] | last'
```

This returns the most recent review that contains the `/review-changes`
signature. Extract:

- `id`: The review ID
- `body`: The full review comment body
- `submitted_at`: The timestamp when the review was posted

If no matching review is found, stop and inform the user: "No `/review-changes`
review found on this PR. Run `/review-changes` first."

### Step 1.3: Parse Original Issues

Parse the review comment body to extract each issue. The `/review-changes`
format is:

```
### {Severity} Priority

**{Category}** in `{file}:{lines}`

{description}

**Recommendation:** {recommendation}
```

For each issue, extract into this structure:

```json
{
  "issues": [
    {
      "file": "path/to/file.ts",
      "lines": "45-50",
      "description": "Description of the issue",
      "category": "bug|CLAUDE.md compliance|plan compliance|sloppy comment",
      "severity": "critical|high|medium|low",
      "recommendation": "Suggested fix"
    }
  ]
}
```

If the review was "Code Review ✅" (no issues), stop and inform the user: "The
last `/review-changes` found no issues. Nothing to re-review."

### Step 1.4: Get Changes Since Review

Get the diff of changes made after the review was posted:

```bash
REVIEW_DATE="{submitted_at from step 1.2}"
# Find the first commit after the review
FIRST_FIX_COMMIT=$(git log --after="$REVIEW_DATE" --format="%H" --reverse HEAD | head -1)
```

If there are no commits after the review, inform the user: "No commits found
since the last review. Make fixes first, then run `/re-review`."

Get the diff from the first post-review commit to HEAD:

```bash
git diff ${FIRST_FIX_COMMIT}^..HEAD
```

### Step 1.5: Create Task List

Create a task list with TaskCreate:

- Task: "Parse original review issues"
- Task: "Verify fixes for original issues (Agent A)"
- Task: "Scan fix delta for regressions (Agent B)"
- Task: "Generate re-review report"
- Task: "Submit re-review to GitHub PR"

## Phase 2: Verify Fixes (Parallel Agents)

### Step 2.1: Launch Verification Agents

Launch **2 agents in parallel**:

---

### Agent A: Fix Verification (Opus)

**Task**: For each original high and medium issue, verify whether it has been
fixed in the current code.

**Input**:

- The list of original issues (from Step 1.3)
- The diff since the review (from Step 1.4)

**Process**:

For each original issue:

1. Read the current state of the file at the flagged lines
2. Check whether the specific problem described in the issue still exists
3. Classify as one of:
   - **fixed**: The issue has been resolved
   - **partially-fixed**: Partially addressed but not fully resolved (explain
     what remains)
   - **not-fixed**: The issue still exists in the current code

**Only check high and medium severity issues.** Low severity issues are
informational and do not need verification.

**Be generous**: If the developer took a different but valid approach to fix the
issue, mark it as **fixed**. The goal is to verify the problem is gone, not that
a specific fix was applied.

**Output format**:

```json
{
  "verifications": [
    {
      "original_issue": {
        "file": "path/to/file.ts",
        "lines": "45-50",
        "description": "Original issue description",
        "severity": "high",
        "category": "bug"
      },
      "status": "fixed|partially-fixed|not-fixed",
      "explanation": "Brief explanation of what changed or why it's still present"
    }
  ]
}
```

Mark task as completed when done.

---

### Agent B: Regression Scan (Opus)

**Task**: Quick scan of the new changes (diff since review) for regressions or
new bugs introduced by the fixes.

**Input**:

- The diff since the review (from Step 1.4)
- The list of modified files

**Scope**: Only scan the delta since the review. Do not review code that was
already reviewed by `/review-changes`.

**Look for**:

- Syntax errors, type errors, undefined variables
- Null/undefined dereferences introduced by the fixes
- Logic errors in the fix code
- Missing imports or broken module paths
- React hooks used incorrectly
- Async/await issues
- Framework-specific issues (consult CLAUDE.md for the project's framework and
  common pitfalls)

**Do NOT flag**:

- Style issues
- Potential issues requiring broader context
- Performance concerns
- Issues that a linter will catch
- Pre-existing issues not in the new delta

**Only flag if you are certain** the new code will fail or produce wrong
results.

**Output format**:

```json
{
  "regressions": [
    {
      "file": "path/to/file.ts",
      "lines": "23",
      "description": "New bug introduced by fix: ...",
      "category": "regression",
      "severity": "critical|high|medium"
    }
  ]
}
```

Mark task as completed when done.

## Phase 3: Generate Report

### Step 3.1: Combine Results

Combine results from Agent A (fix verifications) and Agent B (regressions).

Count:

- Total original issues checked (high + medium only)
- Fixed
- Partially fixed
- Not fixed
- New regressions found

### Step 3.2: Format Report

**If ALL high/medium issues are fixed and NO regressions found:**

```markdown
## Re-Review ✅ - All Issues Resolved

All {count} high/medium issue(s) from the original review have been addressed.

{For each original issue:}

- [x] ~~**{Category}** in `{file}:{lines}` — {brief description}~~

No regressions detected in the fix changes.

---

_Re-review conducted by `/re-review` skill — verifying fixes from
`/review-changes`_
```

**If some issues remain OR regressions found:**

```markdown
## Re-Review - {remaining_count} Issue(s) Remaining

**Original issues:** {total} checked, {fixed} fixed, {partial} partially fixed,
{not_fixed} not fixed

### Fix Status

{For each original issue:}

- [x] ~~**{Category}** in `{file}:{lines}` — {description}~~ **Fixed**
- [ ] **{Category}** in `{file}:{lines}` — {description} **Not Fixed**
  > {explanation of what still needs to change}

{If regressions found:}

### New Regressions

**{Category}** in `{file}:{lines}`

{description}

**Recommendation:** {fix suggestion}

---

_Re-review conducted by `/re-review` skill — verifying fixes from
`/review-changes`_
```

Mark "Generate re-review report" task as completed.

## Phase 4: Submit to GitHub PR

### Step 4.1: Submit Review

Submit the formatted report as a PR review using a HEREDOC:

**If ALL high/medium issues fixed and NO regressions** (approve):

```bash
gh pr review --approve --body "$(cat <<'EOF'
{formatted_report}
EOF
)"
```

**If some high/medium issues remain** (comment):

```bash
gh pr review --comment --body "$(cat <<'EOF'
{formatted_report}
EOF
)"
```

**If new critical/high regressions found** (request changes):

```bash
gh pr review --request-changes --body "$(cat <<'EOF'
{formatted_report}
EOF
)"
```

**Decision logic:**

- **APPROVE**: All high/medium issues fixed, no critical/high regressions
- **COMMENT**: Some high/medium issues remain but no new critical/high
  regressions
- **REQUEST_CHANGES**: New critical or high severity regressions found

Note: If submitting fails because you cannot approve/request-changes on your own
PR, fall back to `--comment`.

### Step 4.2: Confirm Submission

Display a confirmation message:

```markdown
Re-review submitted to PR #{number}: {title} {pr_url}

Review type: {APPROVE|COMMENT|REQUEST_CHANGES} Original issues: {fixed}/{total}
fixed Regressions: {count} found
```

If the submission fails, inform the user and suggest they manually post the
review or authenticate with `gh auth login`.

Mark "Submit re-review to GitHub PR" task as completed.

## Notes

- This skill only works after `/review-changes` has been run on the same PR
- It checks the most recent `/review-changes` review, not all reviews
- Low severity issues are listed for completeness but do not block approval
- The regression scan is scoped to the post-review delta only
- If the developer squashed or rebased after the review, the timestamp-based
  commit detection still works because `git log --after` uses commit dates
- Create a task list before starting (use TaskCreate)
- Update task status as you progress (use TaskUpdate)
