package com.fieldassets

import android.app.Application
import com.fieldassets.data.remote.buildAssetApi
import com.fieldassets.data.repository.AssetRepository

class FieldAssetsApp : Application() {
    val repository: AssetRepository by lazy { AssetRepository(buildAssetApi()) }
}
