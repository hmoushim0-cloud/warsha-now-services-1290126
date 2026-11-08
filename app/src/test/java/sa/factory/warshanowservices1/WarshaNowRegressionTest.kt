package sa.factory.warshanowservices1

import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import sa.factory.warshanowservices1.data.repository.WarshaNowRepositoryImpl
import sa.factory.warshanowservices1.engine.GpsMobileDispatchEngine
import sa.factory.warshanowservices1.engine.MadaEmvContactlessProtocolEngine
import sa.factory.warshanowservices1.hardware.HardwareSensorDriver
import sa.factory.warshanowservices1.model.WarshaNowItem

class WarshaNowRegressionTest {

    private val madaEngine = MadaEmvContactlessProtocolEngine()
    private val dispatchEngine = GpsMobileDispatchEngine()

    @Test
    fun test01_berTlvHexParser() {
        // Tag 5A (length 8: 5888500012349912), Tag 9F02 (length 6: 000000015000)
        val tlvHex = "5A 08 58 88 50 00 12 34 99 12 9F02 06 00 00 00 01 50 00"
        val parsed = madaEngine.parseTlvString(tlvHex)

        assertTrue(parsed.containsKey("5A"))
        assertEquals(8, parsed["5A"]?.length)
        assertEquals("5888500012349912", parsed["5A"]?.valueHex)

        assertTrue(parsed.containsKey("9F02"))
        assertEquals(6, parsed["9F02"]?.length)
        assertEquals("000000015000", parsed["9F02"]?.valueHex)
    }

    @Test
    fun test02_madaAtheerUnder300SarNoCvm() {
        val result = madaEngine.processMadaTap("5888500012349912", 150.0, 101)
        assertEquals(MadaEmvContactlessProtocolEngine.CvmRequired.NO_CVM_ATHEER, result.cvmMethod)
        assertTrue(result.isApproved)
    }

    @Test
    fun test03_madaAbove300SarRequiresOnlinePin() {
        val result = madaEngine.processMadaTap("5888500012349912", 450.0, 102)
        assertEquals(MadaEmvContactlessProtocolEngine.CvmRequired.ONLINE_PIN, result.cvmMethod)
        assertTrue(result.isApproved)
    }

    @Test
    fun test04_madaCardMaskingAndCryptogramGeneration() {
        val result = madaEngine.processMadaTap("5888500012349912", 210.0, 103)
        assertEquals("588850******9912", result.maskedPan)
        assertEquals(16, result.cryptogram.length) // 8 bytes -> 16 hex chars
        assertTrue(result.authCode.startsWith("AUTH-"))
    }

    @Test
    fun test05_haversineDistanceCalculationAccuracy() {
        val olaya = GpsMobileDispatchEngine.GeoCoordinates(24.7136, 46.6753)
        val nakheel = GpsMobileDispatchEngine.GeoCoordinates(24.7743, 46.7386)

        val distance = dispatchEngine.calculateHaversineDistanceKm(olaya, nakheel)
        assertTrue(distance > 8.0 && distance < 11.0) // ~9.4 km
    }

    @Test
    fun test06_urbanEtaCalculationWithCongestionMultiplier() {
        // 10 km at 42 km/h * 1.25 congestion = 0.297 hours = ~18 minutes
        val eta = dispatchEngine.calculateEtaMinutes(10.0, 1.25)
        assertTrue(eta in 16..20)

        // Very close distance bounds to at least 3 minutes minimum
        val etaShort = dispatchEngine.calculateEtaMinutes(0.5)
        assertEquals(3, etaShort)
    }

    @Test
    fun test07_dispatchMissionCreationAndStateTransition() {
        val technician = GpsMobileDispatchEngine.MobileTechnician(
            technicianId = "TECH-101",
            name = "Eng. Sami",
            vanPlate = "KSA-1122-A",
            currentLoc = GpsMobileDispatchEngine.GeoCoordinates(24.7136, 46.6753)
        )
        val destination = GpsMobileDispatchEngine.GeoCoordinates(24.7500, 46.7000)

        val mission = dispatchEngine.createDispatchMission(technician, destination, "Emergency Battery Jumpstart")

        assertEquals(technician.vanPlate, mission.technician.vanPlate)
        assertEquals(GpsMobileDispatchEngine.DispatchStatus.EN_ROUTE, mission.status)
        assertTrue(mission.distanceKm > 0.0)
        assertTrue(mission.etaMinutes >= 3)
        assertEquals("Emergency Battery Jumpstart", mission.serviceCategory)
    }

    @Test
    fun test08_geodesicZeroDistanceSelfCheck() {
        val point = GpsMobileDispatchEngine.GeoCoordinates(24.7136, 46.6753)
        val distance = dispatchEngine.calculateHaversineDistanceKm(point, point)
        assertEquals(0.0, distance, 0.001)
    }

    @Test
    fun test09_hardwareSensorDriverDualEngineStream() = runBlocking {
        val driver = HardwareSensorDriver(madaEngine, dispatchEngine)
        val frame = driver.observePeripheralStream().first()

        assertEquals("CONNECTING", frame.connectionState)
        assertTrue(frame.peripheralName.contains("Mada Contactless"))
        assertTrue(frame.livePayload.contains("NFC ISO 14443-A"))
    }

    @Test
    fun test10_repositoryCartAndVatCalculation() = runBlocking {
        val driver = HardwareSensorDriver(madaEngine, dispatchEngine)
        val repository = WarshaNowRepositoryImpl(driver)

        val item1 = WarshaNowItem("srv-1", "فحص كمبيوتر وتشخيص أعطال متنقل", "فحص شامل OBD-II", 250.0, "معتمد", "الخدمات الرئيسية")
        val item2 = WarshaNowItem("srv-2", "تبديل بطارية وتوليد ميداني", "بطارية أصلية مع الضمان", 400.0, "فوري", "المراقبة الحية")

        repository.addToCart(item1)
        repository.addToCart(item2)

        val order = repository.checkout("Mada Contactless EMV L2")

        assertEquals(650.0, order.subtotalSar, 0.01)
        assertEquals(97.5, order.vatSar, 0.01) // 15% VAT
        assertEquals(747.5, order.totalSar, 0.01)
        assertEquals("Mada Contactless EMV L2", order.paymentMethod)
        assertEquals(0, repository.activeCart.value.size)
    }
}
