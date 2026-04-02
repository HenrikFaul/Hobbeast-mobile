# Shared Dev Governance Controller

This repository follows the central governance pack from HenrikFaul/governance.

## Core rules
1. Never break already working functionality.
2. Read .governance/agent_execution_rules.md, .governance/codingLessonsLearnt.md, and the local CHANGELOG.md before implementation.
3. Prefer the smallest regression-risk solution.
4. Run the available validation steps after implementation.
5. Update changelog and versioning artifacts when the workflow requires it.
6. Treat clear user requests as execution instructions for related non-destructive Jira, GitHub, changelog, governance, and documentation steps.

## Required checklist
- Read execution rules, lessons, and changelog
- Identify root cause
- Compare at least two solution options when risk is non-trivial
- Re-check that previously working features still work
- Update required governance and delivery artifacts

## Key files
- .governance/agent_execution_rules.md
- .governance/codingLessonsLearnt.md
- .governance/business_process_automation_prompts.md
- codingLessonsLearnt.local.md
