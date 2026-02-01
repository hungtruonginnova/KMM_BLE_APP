package com.blecenter.blecenter

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform