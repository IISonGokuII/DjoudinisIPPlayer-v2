package com.djoudini.iplayer.presentation.ui

import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withId
import com.djoudini.iplayer.R
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class SplashActivityTest {

    @get:Rule
    val activityRule = ActivityScenarioRule(SplashActivity::class.java)

    @Test
    fun splashScreenShowsCoreBranding() {
        onView(withId(R.id.splash_logo)).check(matches(isDisplayed()))
        onView(withId(R.id.splash_welcome_text)).check(matches(isDisplayed()))
        onView(withId(R.id.splash_subtitle)).check(matches(isDisplayed()))
    }
}
