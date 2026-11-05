package com.mobilefork.hermesagent.ui.shell

import org.junit.Assert.assertEquals
import org.junit.Test

class AppShellNavigationDrawerTest {
    @Test
    fun everySectionIsReachableFromTheTabBarOrFromApps() {
        val reachable = shellTabs() + shellAppsDestinations()

        assertEquals(AppSection.Hermes, shellTabs().first())
        assertEquals(reachable, reachable.distinct())
        assertEquals(AppSection.entries.toSet(), reachable.toSet())
    }
}
