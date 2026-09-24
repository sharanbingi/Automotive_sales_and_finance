package com.automotive.salesfinance

import android.app.Application
import com.automotive.salesfinance.data.AppContainer

class AutomotiveApplication : Application() {
    val appContainer: AppContainer by lazy {
        AppContainer()
    }
}
