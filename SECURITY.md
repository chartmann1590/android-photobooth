# Security Policy

## Supported Versions

Security fixes are applied to the latest release on `main`. Older tagged
releases are not maintained — please update to the most recent version before
reporting an issue.

## Reporting a Vulnerability

**Do not open a public GitHub issue for security problems.** Please use one of
the private channels below so the vulnerability can be fixed before details
become public.

- **GitHub private vulnerability reporting** (preferred):
  <https://github.com/chartmann1590/android-photobooth/security/advisories/new>
- **Email:** charles.h.hartmann1@gmail.com

When reporting, include as much of the following as you can:

- A description of the issue and the potential impact
- Steps to reproduce, ideally with a minimal proof of concept
- The affected version (commit SHA or release tag)
- Any suggested mitigation or fix

You can expect an acknowledgement within **5 business days**. After triage,
you will receive a timeline for the fix; the goal for high-severity issues is
a patched release within **30 days** of confirmation. Coordinated disclosure
is appreciated — please give us a chance to ship a fix before publishing
details.

## Scope

In scope:

- The Android application in this repository (`app/`)
- Build, release, and CI configuration in `.github/` and Gradle files

Out of scope:

- Vulnerabilities in third-party services the app can be configured to talk
  to (Immich, SMTP providers, SMS gateways, anonymous image hosts). Report
  those to the relevant upstream project.
- Issues that require a rooted device, physical access, or a malicious
  companion app already installed with broad permissions.
- Findings produced solely by automated scanners without a demonstrated
  impact.
