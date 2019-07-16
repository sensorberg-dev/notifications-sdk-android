package com.sensorberg.notifications.sdk.internal

import android.app.Application
import org.koin.core.Koin
import org.koin.core.KoinApplication
import org.koin.core.KoinComponent
import org.koin.core.module.Module
import org.koin.dsl.koinApplication

internal interface NotificationSdkComponent : KoinComponent {
	override fun getKoin(): Koin = Kointainer.getKoin()
}

/**
 * Starts the Koin instance
 */
internal fun insertKoin(app: Application,
						apiKey: String,
						baseUrl: String,
						log: Boolean) {
	Kointainer.init(InjectionModule(app, apiKey, baseUrl, log).module)
}

private object Kointainer {
	private lateinit var koinInstance: KoinApplication

	fun init(module: Module) {
		this.koinInstance = koinApplication { modules(module) }
	}

	fun getKoin(): Koin = koinInstance.koin
}