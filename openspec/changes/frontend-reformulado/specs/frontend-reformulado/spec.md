## Purpose

Definir uma experiência Angular reformulada, acessível e responsiva para gerenciar carteiras, ações, corretoras e indicadores financeiros usando dados reais das APIs existentes.

## ADDED Requirements

### Requirement: Guided investment screens
The frontend SHALL present labeled forms, contextual help, named selectors, empty states and actionable feedback for investment workflows.

#### Scenario: Position prerequisites
- **WHEN** a portfolio has no stock or broker available
- **THEN** the interface explains the prerequisite and provides the registration action

### Requirement: Valid portfolio navigation
The frontend SHALL open dashboard views only for a valid selected portfolio and SHALL NOT depend on a fixed portfolio identifier.

#### Scenario: Dashboard entry
- **WHEN** the user opens the overview without a selected portfolio
- **THEN** the interface presents portfolio selection or onboarding instead of requesting a fabricated ID

### Requirement: Responsive accessible presentation
The frontend SHALL keep primary controls labelled, keyboard reachable and usable across desktop, tablet and mobile layouts.

#### Scenario: Narrow viewport
- **WHEN** the viewport is mobile width
- **THEN** forms stack and tables remain accessible through horizontal scrolling
