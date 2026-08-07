package com.cnsoftware.synapse.util

import kotlin.random.Random

/** Basit, çakışma ihtimali son derece düşük bir yerel kimlik üretici. */
fun generateId(): String = "${currentTimeMillis()}-${Random.nextInt(0, Int.MAX_VALUE)}"
