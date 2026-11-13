# Security and privacy

- MONOLITH TV must work offline. No INTERNET, microphone, camera, location, accessibility service, VPN, overlay or broad storage permission.
- Only query TV-launchable activities with explicit Android 11+ visibility declarations; no QUERY_ALL_PACKAGES in the baseline.
- Save favorites in local app-private preferences; do not transmit app inventory.
- All app launch requests use explicit components obtained from PackageManager with exception handling.
- No root, privileged device-admin, hidden ADB services, automatic HOME hijacking or stock launcher disabling.
- CI debug signing is temporary. A stable release requires a *separate* MONOLITH TV keystore in private secrets, certificate fingerprint verification and an update rehearsal.
- Do not reuse the mobile MONOLITH signing key, its applicationId or private configuration.
- Report vulnerabilities privately to the repository owner. Do not submit real credentials in issues or logs.

