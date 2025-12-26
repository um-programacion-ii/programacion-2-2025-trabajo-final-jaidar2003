package org.example.project.network

object ApiConfig {
    // 10.0.2.2 is localhost for Android Emulator
    // localhost is localhost for iOS Simulator
    // We should probably detect platform or just use a constant we can swap?
    // For now let's try to detect or use the appropriate one.
    // Ideally we'd use BuildConfig but that's Android specific.
    // Let's use expect/actual for BaseUrl
}

expect val SERVER_URL: String
expect val PROXY_URL: String
