package com.example.expenseassistant

import com.example.expenseassistant.ai.ConversationalAiHelper
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ConversationalAiHelperTest {

    @Test
    fun testAdvisoryQueryDetection() {
        assertTrue(ConversationalAiHelper.isAdvisoryOrCoachingQuery("how to save money?"))
        assertTrue(ConversationalAiHelper.isAdvisoryOrCoachingQuery("taka bachabo kivabe"))
        assertTrue(ConversationalAiHelper.isAdvisoryOrCoachingQuery("explain 50/30/20 budget rule"))
        assertTrue(ConversationalAiHelper.isAdvisoryOrCoachingQuery("what is an emergency fund?"))
        assertFalse(ConversationalAiHelper.isAdvisoryOrCoachingQuery("50 tk bus vara"))
    }

    @Test
    fun testPersonaOrSmallTalkDetection() {
        assertTrue(ConversationalAiHelper.isPersonaOrSmallTalk("who are you?"))
        assertTrue(ConversationalAiHelper.isPersonaOrSmallTalk("tumi ke"))
        assertTrue(ConversationalAiHelper.isPersonaOrSmallTalk("who made you?"))
        assertTrue(ConversationalAiHelper.isPersonaOrSmallTalk("hello"))
        assertTrue(ConversationalAiHelper.isPersonaOrSmallTalk("kemon acho"))
        assertTrue(ConversationalAiHelper.isPersonaOrSmallTalk("thank you!"))
        assertFalse(ConversationalAiHelper.isPersonaOrSmallTalk("salary paisi 30000 tk"))
    }

    @Test
    fun testOfflineFallbackResponses() {
        val identity = ConversationalAiHelper.tryOfflineFallbackResponse("who are you")
        assertNotNull(identity)
        assertTrue(identity!!.contains("Gemma"))

        val tips = ConversationalAiHelper.tryOfflineFallbackResponse("how to save money")
        assertNotNull(tips)
        assertTrue(tips!!.contains("50/30/20"))
    }
}
