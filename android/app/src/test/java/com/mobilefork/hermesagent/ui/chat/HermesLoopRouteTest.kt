package com.mobilefork.hermesagent.ui.chat

import com.mobilefork.hermesagent.data.CapabilitiesBridge
import com.mobilefork.hermesagent.data.SkillState
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class HermesLoopRouteTest {
    @Test
    fun anAgentWithDeviceToolsAlwaysRunsInTheHermesLoop() {
        assertTrue(hermesLoopRoute(deviceToolsAllowed = true, playEdition = false))
        // Kept away from the device: talks to its model directly.
        assertFalse(hermesLoopRoute(deviceToolsAllowed = false, playEdition = false))
        // The Play edition ships without the Python runtime.
        assertFalse(hermesLoopRoute(deviceToolsAllowed = true, playEdition = true))
    }

    @Test
    fun capabilitiesReadEveryPartAndKeepThePartsThatLoaded() {
        val snapshot = CapabilitiesBridge.parse(
            JSONObject(
                """{"ok":true,"skills":[{"name":"a","state":"active"},{"name":"b","state":"available","identifier":"official/x/b"}],
                "plugins":[{"key":"p","name":"P","enabled":true}],"toolsets":[],"errors":{"toolsets":"boom"}}""",
            ),
        )
        assertEquals(listOf(SkillState.Active, SkillState.Available), snapshot.skills.map { it.state })
        assertEquals("official/x/b", snapshot.skills[1].identifier)
        assertTrue(snapshot.plugins.single().enabled)
        assertEquals("boom", snapshot.partErrors["toolsets"])
    }
}
