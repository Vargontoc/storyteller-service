# Sprint {number} - {layer}


## Goal
(One sentence describing what this sprint achieves)

## Status
status: {active | blocked | pending_review | completed}
started_at: {YYYY-MM-DD HH:mm:ss}
closed_at: {YYYY-MM-DD HH:mm:ss - filled by agent when completed}
blocked_by: {layer}
waiting_for: {description of what is needed to unblock}

## Tasks
(Agent updates checkboxes as tasks are completed)
- [ ] {task 1}
- [ ] {task 2}

## Risks
{What could go wrong or generate dependencies with other layers}
- {risk 1}

## Dependencies
{What this layer neds from another layer before proceeding}
{Reference docs/contracts/ if an API or schema contract is involved}
- {dependency 1}

## Agent Instruction
{Specific directives for agent working on this sprint}
{These override default skill behaviour for this sprint only}
- {instruction 1}

## Notes
{Decisions taken during the sprint, context for the agent}
{Both human cand agent can write here}

## Review
{Filled automatically by the agent when status is set to completed}

completed_tasks:
    {agent lists what was done}
    
incomplete_tasks:
    {agent lists what was not completed and why}

contract_changes:
    {agent lists any changes made to docs/contracts/ during this sprint}

learnings:
    {aggent summaries decisions, blockers found and how they were resolved}

next_sprint_suggestions:
    {agent propose what should go in the next sprint based on incomplete tasks}