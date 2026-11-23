package com.nijobenny.wayfinder.data.repository

import com.nijobenny.wayfinder.data.remote.ValhallaApi
import com.nijobenny.wayfinder.data.remote.ValhallaCostingOptions
import com.nijobenny.wayfinder.data.remote.ValhallaLocation
import com.nijobenny.wayfinder.data.remote.ValhallaRequest
import com.nijobenny.wayfinder.data.remote.toRoute
import com.nijobenny.wayfinder.domain.model.LatLng
import com.nijobenny.wayfinder.domain.model.Route
import com.nijobenny.wayfinder.domain.model.RouteOptions
import com.nijobenny.wayfinder.domain.model.TravelMode
import com.nijobenny.wayfinder.domain.repository.RoutingRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay

/** Routes with Valhalla, respecting the public server's 1 request/second limit. */
class ValhallaRoutingRepository(
    private val api: ValhallaApi,
    private val currentTimeMillis: () -> Long,
) : RoutingRepository {

    private var lastRequestAt = 0L

    override suspend fun route(points: List<LatLng>, mode: TravelMode, options: RouteOptions): Result<Route> {
        require(points.size >= 2) { "A route needs at least two points" }
        val wait = 1_100 - (currentTimeMillis() - lastRequestAt)
        if (wait > 0) delay(wait)
        lastRequestAt = currentTimeMillis()

        val costingOptions = if (mode == TravelMode.Car && (options.avoidTolls || options.avoidHighways)) {
            mapOf(
                mode.costing to ValhallaCostingOptions(
                    useTolls = if (options.avoidTolls) 0.0 else null,
                    useHighways = if (options.avoidHighways) 0.0 else null,
                )
            )
        } else emptyMap()

        return try {
            val response = api.route(
                ValhallaRequest(
                    locations = points.map { ValhallaLocation(it.latitude, it.longitude) },
                    costing = mode.costing,
                    costingOptions = costingOptions,
                )
            )
            val route = response.toRoute(mode)
            if (route.geometry.size < 2) Result.failure(IllegalStateException("No route found"))
            else Result.success(route)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
