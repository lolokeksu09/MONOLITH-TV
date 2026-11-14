package dev.monolith.tv.ui

/**
 * Resolve a stable component key after dock changes.
 *
 * Never use the previous visual index: adding a favorite may reorder apps.
 * Returning null for an empty list lets the screen focus the active tab.
 */
internal fun resolveDockFocusKey(
    visibleComponentKeys: List<String>,
    preferredComponentKey: String?
): String? =
    preferredComponentKey?.takeIf { it in visibleComponentKeys }
        ?: visibleComponentKeys.firstOrNull()
