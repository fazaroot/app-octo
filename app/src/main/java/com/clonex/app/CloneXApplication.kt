package com.clonex.app

import android.app.Application
import com.clonex.app.data.db.AppDatabase

/**
 * Application CloneX.
 *
 * Menyediakan instance Room Database secara malas (lazy) agar seluruh
 * layar memakai satu sumber data yang sama. Semua pemrosesan lokal,
 * tidak ada upload APK atau data pribadi.
 */
class CloneXApplication : Application() {
    val database: AppDatabase by lazy {
        AppDatabase.getInstance(this)
    }
}
