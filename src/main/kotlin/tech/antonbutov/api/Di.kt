package tech.antonbutov.api

import dagger.dsl.core.DaggerDsl
import dagger.dsl.core.component
import dagger.dsl.core.get

interface Component {
    fun getRecogniser(): SpeechKitAuth
}

@DaggerDsl
val di =
    component<Component> {
    }
