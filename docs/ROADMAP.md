# Roadmap

These are **planned** milestones, not promises or shipped features.

## 0.1.0-prealpha — runnable foundation

- [x] Product specification, architecture and recovery policy
- [x] Basic standalone Android TV project
- [x] Favorites + launchable apps prototype
- [x] CI configuration
- [x] CI green for initial pre-alpha and 0.1.1 source
- [ ] Verify **0.1.1** preview on BOXY (earlier build has screenshot evidence, not complete DPAD acceptance)
- [ ] Manual stock launcher return test

## 0.1.1-prealpha — implemented; device acceptance pending

- [x] All / Favorites navigation and empty-state copy
- [x] Launcher settings: compact grid, favorites-first sorting, system Settings entry
- [x] Persist launcher options independently of favorite storage
- [x] Focus target restoration after favorite reordering and returning from settings (source; device validation pending)
- [x] Regression tests for catalog ordering and filtering
- [ ] BOXY validation for new UI/focus + repeatable performance baseline

## 0.2.0-alpha — first usable replacement

- [x] Internal settings panel; [ ] explicit stock-home exit
- [ ] Reliable app install/uninstall updates and launcher exceptions
- [x] Favorite-first sort and focus-restoration implementation; [ ] physical D-pad acceptance
- [ ] TV banner and original MONOLITH TV identity designed and approved
- [ ] Official signed update channel

## 0.3.0-alpha — polish

- [x] Configurable grid density; [ ] alternate theme
- [ ] Optional hidden app support with safe recovery
- [ ] Structured bilingual localization
- [ ] Focus and startup benchmark on BOXY

## 1.0.0 — stable

- [ ] Crash-free real-device regression
- [ ] Settings migration and backup verified
- [ ] Verified same-certificate updates
- [ ] Clear installation, uninstallation and recovery instructions
- [ ] No critical accessibility or DPAD defects

**Do not declare 1.0-ready based only on emulator and CI.**

