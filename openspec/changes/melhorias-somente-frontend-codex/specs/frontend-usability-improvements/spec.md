## Purpose

Tornar as telas existentes mais claras, modernas, acessíveis e responsivas, preservando integralmente os contratos e regras atuais do backend.

## ADDED Requirements

### Requirement: Clear frontend feedback
The frontend SHALL present labels, contextual help, loading, empty, success and actionable error states without exposing technical backend details.

#### Scenario: Form submission failure
- **WHEN** an existing API operation fails
- **THEN** the form preserves user values and presents a contextual message near the affected action

### Requirement: Responsive existing workflows
The frontend SHALL keep existing workflows usable on desktop, tablet and mobile widths using the current application routes.

#### Scenario: Mobile layout
- **WHEN** the viewport is narrow
- **THEN** forms stack, tables remain horizontally accessible and primary actions remain reachable
