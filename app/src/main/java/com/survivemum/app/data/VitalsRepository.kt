package com.survivemum.app.data

import com.survivemum.app.model.VitalsState
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlin.random.Random

/**
 * Repository for maternal vitals.
 * In a real app, this would interface with sensors or a backend.
 */
class VitalsRepository {

    fun getVitalsStream(isHighRisk: Boolean): Flow<VitalsState> = flow {
        while (true) {
            emit(generateRandomVitals(isHighRisk))
            delay(2000)
        }
    }

    private fun generateRandomVitals(isHighRisk: Boolean): VitalsState {
        val multiplier = if (isHighRisk) 1.2 else 1.0
        val hr = (Random.nextInt(70, 90) * multiplier).toInt()
        val spo2 = if (isHighRisk) Random.nextInt(92, 96) else Random.nextInt(97, 100)
        val rr = (Random.nextInt(12, 20) * multiplier).toInt()
        val temp = 36.5 + (Random.nextDouble(0.0, 1.0) * (if (isHighRisk) 1.5 else 1.0))
        val systolic = (Random.nextInt(110, 130) * multiplier).toInt()
        val diastolic = (Random.nextInt(70, 85) * multiplier).toInt()

        return VitalsState(
            hr = hr,
            spo2 = spo2,
            rr = rr,
            temp = (temp * 10).toInt() / 10.0,
            bp = "$systolic/$diastolic",
            status = if (isHighRisk) "HIGH" else "NORMAL"
        )
    }
}
