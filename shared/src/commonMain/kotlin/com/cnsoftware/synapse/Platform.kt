package com.cnsoftware.synapse

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform