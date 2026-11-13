# MONOLITH TV · Visual system v0

## Visual thesis

A quiet, editorial television home screen: fast recognition, no commercial feeds, no gradients pretending to be content. Premium means precision and legibility, not decorative complexity.

## Surface and type

- Base canvas: charcoal near-black `#0B0D10`; panels: `#16191E`.
- Foreground: near-white `#F3F4F5`; secondary: `#AFB4BC`.
- Focus: neutral white keyline and subtle elevation/background shift, not neon glow.
- Type: platform sans serif; title 26–32sp, tile labels 15–18sp, supportive text 13–16sp.
- TV 16:9 landscape first; preserve approximately 48dp safe margins on 1080p-class DP canvas.
- Never put decorative shapes behind or over **original installed application icons**.

## Interaction

- Every interactive control is reachable by remote navigation.
- The focused tile must be distinguishable without relying on color alone.
- DPAD_CENTER and ENTER trigger activation; BACK must behave predictably.
- MENU can toggle favorite in the initial implementation; show this affordance in UI.
- No hover-only or touch-only features. Animations <= approximately 180 ms and respect reduced motion when detectable.

## States

- Startup loading: meaningful text rather than black screen.
- No apps: explicit empty state, access to Settings and safe exit.
- Launch failure: non-blocking message, focus remains stable.
- Favorite persistence: no random focus jump after reordering.

## Hard ban on generic generated UI

No invented KPI cards, stock photography, glassmorphism, ornament-only gradients, decorative numbering, unrelated charts, gratuitous icon packs or inconsistent focus animations.

## Visual quality gates

Measure screenshots on a real TV/display, verify each DPAD direction, font contrast, 720p/1080p/4K scaling, and app icon artwork fidelity. Visual references do not count as device tests.

