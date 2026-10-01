# Security Policy

## Reporting a vulnerability

Please use GitHub's private vulnerability reporting feature for this repository. Do not open a public issue for an unpatched vulnerability.

Include the affected version, reproduction steps, impact, and any suggested mitigation. Reports will be acknowledged as soon as practical.

## Scope

FTP is intentionally an unencrypted protocol. Cleartext credentials and file contents on the local network are a documented limitation, not a vulnerability in this project. Authentication bypasses outside explicitly enabled anonymous mode, path traversal, unintended network access, and unsafe file operations are in scope.
