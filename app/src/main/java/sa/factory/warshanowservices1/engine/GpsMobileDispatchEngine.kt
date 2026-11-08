package sa.factory.warshanowservices1.engine

import kotlin.math.*

/**
 * GPS Mobile Workshop Fleet Dispatch & Geodesic Navigation Engine.
 * Calculates Haversine distances, predicts travel ETAs with urban congestion factors,
 * and manages the on-demand technician job dispatch lifecycle.
 */
class GpsMobileDispatchEngine {

    companion object {
        const val EARTH_RADIUS_KM = 6371.0
        const val URBAN_AVERAGE_SPEED_KMH = 42.0 // Riyadh/Jeddah urban average speed
    }

    enum class DispatchStatus(val arabicLabel: String) {
        REQUESTED("تم استلام طلب الصيانة الميدانية"),
        ASSIGNED("تم تعيين فني الورشة المتنقلة"),
        EN_ROUTE("الفني في الطريق إلى الموقع"),
        ON_SITE("الفني يباشر أعمال الصيانة بالموقع"),
        COMPLETED("تم إنهاء الصيانة وإصدار الفاتورة")
    }

    data class GeoCoordinates(
        val latitude: Double,
        val longitude: Double
    )

    data class MobileTechnician(
        val technicianId: String,
        val name: String,
        val vanPlate: String,
        val currentLoc: GeoCoordinates,
        val rating: Double = 4.9
    )

    data class DispatchMission(
        val missionId: String,
        val technician: MobileTechnician,
        val destination: GeoCoordinates,
        val distanceKm: Double,
        val etaMinutes: Int,
        val status: DispatchStatus,
        val serviceCategory: String
    )

    /**
     * Calculates geodesic distance between two GPS coordinates using the Haversine formula.
     */
    fun calculateHaversineDistanceKm(start: GeoCoordinates, end: GeoCoordinates): Double {
        val dLat = Math.toRadians(end.latitude - start.latitude)
        val dLon = Math.toRadians(end.longitude - start.longitude)

        val lat1 = Math.toRadians(start.latitude)
        val lat2 = Math.toRadians(end.latitude)

        val a = sin(dLat / 2).pow(2) + sin(dLon / 2).pow(2) * cos(lat1) * cos(lat2)
        val c = 2 * asin(sqrt(a))
        return EARTH_RADIUS_KM * c
    }

    /**
     * Estimates transit duration in minutes, incorporating an urban congestion multiplier.
     */
    fun calculateEtaMinutes(distanceKm: Double, congestionFactor: Double = 1.25): Int {
        val travelHours = (distanceKm / URBAN_AVERAGE_SPEED_KMH) * congestionFactor
        val minutes = (travelHours * 60.0).roundToInt()
        return max(3, minutes) // minimum 3 minutes
    }

    /**
     * Synthesizes an active mobile service dispatch mission.
     */
    fun createDispatchMission(
        technician: MobileTechnician,
        clientLocation: GeoCoordinates,
        serviceCategory: String = "Automotive Battery & Electrical"
    ): DispatchMission {
        val distance = calculateHaversineDistanceKm(technician.currentLoc, clientLocation)
        val eta = calculateEtaMinutes(distance)

        return DispatchMission(
            missionId = "DISPATCH-%05d".format((System.currentTimeMillis() % 100000)),
            technician = technician,
            destination = clientLocation,
            distanceKm = distance,
            etaMinutes = eta,
            status = DispatchStatus.EN_ROUTE,
            serviceCategory = serviceCategory
        )
    }
}
