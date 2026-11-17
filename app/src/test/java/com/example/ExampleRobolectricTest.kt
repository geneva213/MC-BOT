package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.core.AppLanguage
import com.example.core.McLocalization
import com.example.core.McVoiceEngine
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

    @Test
    fun `app name matches MC Assistant`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("MC Assistant", appName)
    }

    @Test
    fun `bilingual wake greeting returns expected English and Bangla responses`() {
        val en = McLocalization.get(AppLanguage.ENGLISH)
        val bn = McLocalization.get(AppLanguage.BANGLA)
        assertEquals("Yes, Boss! How can I help you?", en.mcGreetingWake)
        assertTrue(bn.mcGreetingWake.contains("হ্যাঁ বস"))
    }

    @Test
    fun `wake phrase matcher recognizes MC triggers`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val engine = McVoiceEngine(context)
        assertTrue(engine.isMcWakePhrase("MC"))
        assertTrue(engine.isMcWakePhrase("Hey MC"))
        assertTrue(engine.isMcWakePhrase("এমসি"))
        engine.destroy()
    }
}
