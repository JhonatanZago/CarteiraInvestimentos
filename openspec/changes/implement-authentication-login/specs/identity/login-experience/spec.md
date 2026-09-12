## Purpose

Oferecer uma experiência de login acessível, responsiva e visualmente alinhada à referência financeira, integrada ao estado global de autenticação e tema.

## ADDED Requirements

### Requirement: Provide authentication screens
The client SHALL provide real routes for login, registration, forgot-password, and reset-password with reactive validation, accessible feedback, password visibility control, loading state, and duplicate-submit prevention.

#### Scenario: Invalid login form
- **WHEN** the user submits an empty or malformed e-mail or an empty password
- **THEN** the form marks the relevant fields, shows localized validation guidance, and does not call the API

#### Scenario: Login request failure
- **WHEN** the authentication API returns invalid credentials or is unavailable
- **THEN** the client shows one friendly message, preserves the e-mail, and re-enables the form

### Requirement: Protect navigation and restore sessions
The client SHALL redirect unauthenticated users from private routes to login with the original URL, redirect authenticated users away from login, restore a valid session after reload, and avoid refresh loops.

#### Scenario: Private route without session
- **WHEN** an unauthenticated user opens a private route
- **THEN** the client navigates to `/login` and retains the requested destination

#### Scenario: Session restoration
- **WHEN** the application starts with a valid refresh session
- **THEN** the client loads the authenticated user and allows navigation without requiring a second manual login

### Requirement: Match the login reference responsively
The login page SHALL render a centered translucent card over a non-interactive blurred financial background, support light and dark themes through the global theme mechanism, and remain usable at desktop and mobile viewport sizes without horizontal scrolling.

#### Scenario: Desktop reference layout
- **WHEN** the login page is viewed at a desktop viewport
- **THEN** the card, branding, fields, actions, theme switch, and decorative chart elements are aligned and legible as in the reference

#### Scenario: Mobile layout
- **WHEN** the login page is viewed on a narrow or short viewport
- **THEN** the form remains accessible with safe margins, no clipped controls, and simplified decorative content
