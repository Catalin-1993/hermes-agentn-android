package com.mobilefork.hermesagent.ui.chat

import org.junit.Assert.assertEquals
import org.junit.Test

class WorkUntilDoneTest {
    @Test
    fun itKeepsGoingWhileTheAgentHasNotSaidItIsFinished() {
        assertEquals(GoalStep.Continue, nextGoalStep("I did the first part.", turnsRun = 0, failed = false))
        assertEquals(GoalStep.Continue, nextGoalStep("Next I will test it.", turnsRun = GOAL_MAX_TURNS - 1, failed = false))
    }

    @Test
    fun itStopsWhenTheAgentIsDoneOrNeedsThePerson() {
        assertEquals(GoalStep.Stop, nextGoalStep("All set.\n$GOAL_DONE", 3, false))
        assertEquals(GoalStep.Stop, nextGoalStep("Which account?\n$GOAL_NEEDS_INPUT", 1, false))
    }

    @Test
    fun aFailedOrEmptyTurnEndsItInsteadOfRepeating() {
        assertEquals(GoalStep.Stop, nextGoalStep("partial", 1, failed = true))
        assertEquals(GoalStep.Stop, nextGoalStep("   ", 1, failed = false))
    }

    @Test
    fun itCanNeverRunAway() {
        assertEquals(GoalStep.Stop, nextGoalStep("still going", GOAL_MAX_TURNS, false))
    }

    @Test
    fun theMarkersAreHiddenFromTheReader() {
        assertEquals("All set.", withoutGoalMarkers("All set.\n$GOAL_DONE"))
        assertEquals("Plain.", withoutGoalMarkers("Plain."))
    }

    @Test
    fun theDirectiveIsOnlyAddedWhileTheModeIsOn() {
        assertEquals(false, chatDirectives(ChatOptions()).contains(GOAL_DONE))
        assertEquals(true, chatDirectives(ChatOptions(workUntilDone = true)).contains(GOAL_DONE))
    }
}
