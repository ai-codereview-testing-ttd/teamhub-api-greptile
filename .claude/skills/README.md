# Claude Code Skills

This repository includes custom skills for automated code review.

## Available Skills

### `/review-small`
Streamlined 2-agent code review for quick feedback.

**When to use:**
- Small bug fixes (< 200 lines)
- Simple features (1-3 files, straightforward logic)
- Documentation or configuration updates

**How it works:**
- Agent 1: Bug detection (syntax, logic, security issues)
- Agent 2: CLAUDE.md compliance (architectural patterns, conventions)
- Issues grouped by severity and validated before reporting

**Usage:**
```bash
/review-small
```

### `/review-changes`
Comprehensive 7-agent code review for complex changes.

**When to use:**
- New architectural patterns
- Complex state management or business logic
- Security-sensitive changes
- Large refactors or new features

**How it works:**
- 7 parallel agents review different aspects (bugs, compliance, plan adherence, comment quality, test coverage)
- All issues validated by sub-agents before reporting
- Reports grouped by severity with detailed recommendations

**Usage:**
```bash
/review-changes
```

### `/re-review`
Follow-up verification after fixing issues.

**When to use:**
- After addressing issues from `/review-changes` or `/review-small`
- To verify fixes don't introduce regressions

**How it works:**
- Loads original review from GitHub PR
- Validates each issue is fixed/partially-fixed/not-fixed
- Scans post-review changes for new bugs
- Posts checklist-style update to PR

**Usage:**
```bash
/re-review
```

## General Review Process

1. Make your code changes
2. Run `/review-small` or `/review-changes`
3. Address flagged issues
4. Run `/re-review` to verify fixes
5. Commit and push when clean
