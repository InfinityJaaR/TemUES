package com.market.temues

import android.app.Application
import com.stripe.android.PaymentConfiguration
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class TemUESApp : Application() {
    override fun onCreate() {
        super.onCreate()
        if (BuildConfig.STRIPE_KEY.isNotEmpty()) {
            PaymentConfiguration.init(applicationContext, BuildConfig.STRIPE_KEY)
        }
    }
}
