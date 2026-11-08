package sa.factory.warshanowservices1.hardware

import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import sa.factory.warshanowservices1.engine.GpsMobileDispatchEngine
import sa.factory.warshanowservices1.engine.MadaEmvContactlessProtocolEngine
import kotlin.random.Random

data class HardwareTelemetryFrame(
    val peripheralName: String,
    val connectionState: String,
    val livePayload: String,
    val timestamp: Long = System.currentTimeMillis()
)

class HardwareSensorDriver(
    private val madaEngine: MadaEmvContactlessProtocolEngine = MadaEmvContactlessProtocolEngine(),
    private val dispatchEngine: GpsMobileDispatchEngine = GpsMobileDispatchEngine()
) {
    private val peripheral = "Mada Contactless EMV L2 NFC + GPS Dispatch"

    fun observePeripheralStream(): Flow<HardwareTelemetryFrame> = flow {
        emit(HardwareTelemetryFrame(peripheral, "CONNECTING", "NFC ISO 14443-A Mada Kernel & GPS Telematics Initializing..."))
        delay(250)

        // Initial Transaction and Dispatch Frame
        val initialMada = madaEngine.processMadaTap("5888500012349912", 210.0, 102)
        val technician = GpsMobileDispatchEngine.MobileTechnician(
            technicianId = "TECH-409",
            name = "Eng. Tariq Al-Otaibi",
            vanPlate = "KSA-9142-R",
            currentLoc = GpsMobileDispatchEngine.GeoCoordinates(24.7136, 46.6753) // Riyadh Olaya
        )
        val mission = dispatchEngine.createDispatchMission(
            technician = technician,
            clientLocation = GpsMobileDispatchEngine.GeoCoordinates(24.7743, 46.7386) // Riyadh Al-Nakheel
        )

        emit(HardwareTelemetryFrame(
            peripheral,
            "ONLINE",
            "Mada L2 [OK] | Atheer (No PIN): ${initialMada.maskedPan} | Van: ${mission.technician.vanPlate} | GPS: ${String.format("%.2f", mission.distanceKm)}km | ETA: ${mission.etaMinutes} mins"
        ))

        var frameCounter = 400
        while (true) {
            delay(1500)
            frameCounter++

            val remainingDist = (mission.distanceKm - (frameCounter % 5) * 0.4).coerceAtLeast(0.8)
            val eta = dispatchEngine.calculateEtaMinutes(remainingDist)
            val amount = 180.0 + (frameCounter % 3) * 50.0
            val tapResult = madaEngine.processMadaTap("5888500012349912", amount, frameCounter)

            val livePayload = "Mada EMV: ${tapResult.cryptogramType.name} (${tapResult.cryptogram}) [OK] | Van: ${mission.technician.vanPlate} (${mission.status.name}) | Dist: ${String.format("%.2f", remainingDist)}km | ETA: ${eta}m"

            emit(HardwareTelemetryFrame(
                peripheralName = peripheral,
                connectionState = "ACTIVE_STREAMING",
                livePayload = livePayload
            ))
        }
    }
}
