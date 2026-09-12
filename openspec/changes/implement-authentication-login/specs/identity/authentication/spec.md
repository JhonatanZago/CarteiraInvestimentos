## Purpose

Fornecer identidade, sessão e recuperação de acesso seguras para proteger dados financeiros e permitir uma entrada consistente na aplicação.

## ADDED Requirements

### Requirement: Register users securely
The system SHALL register a user with a required name, normalized unique e-mail, BCrypt-protected password, active status, and the default USER profile without accepting a public profile override.

#### Scenario: Valid registration
- **WHEN** a client submits matching valid registration fields
- **THEN** the system creates the user and returns a representation that never contains the password or hash

#### Scenario: Duplicate e-mail
- **WHEN** a client submits an e-mail already registered after normalization
- **THEN** the system returns HTTP 409 with a stable error code and does not create another user

### Requirement: Authenticate and manage sessions
The system SHALL authenticate active users with e-mail and password, issue a short-lived signed access token and a refresh token in a protected HttpOnly cookie, and support `/auth/me`, refresh, and logout operations.

#### Scenario: Successful login
- **WHEN** an active user submits valid credentials and the keep-connected option
- **THEN** the system returns HTTP 200, establishes the session, and does not expose the refresh token in JSON

#### Scenario: Invalid credentials or inactive user
- **WHEN** credentials are invalid or the account is inactive
- **THEN** the system returns HTTP 401 with a generic message that does not reveal account existence

#### Scenario: Logout
- **WHEN** an authenticated client calls logout
- **THEN** the refresh token is invalidated, the cookie is cleared, and the system returns HTTP 204

### Requirement: Recover passwords without account disclosure
The system SHALL accept password-recovery requests with a generic response, store only a one-time hashed reset token with expiration, and invalidate existing sessions after a successful reset.

#### Scenario: Recovery request
- **WHEN** a client submits any syntactically valid e-mail
- **THEN** the system returns the same generic confirmation regardless of whether the e-mail exists

#### Scenario: Expired or reused reset token
- **WHEN** a client submits an expired or previously consumed reset token
- **THEN** the system rejects the reset and does not change the password
