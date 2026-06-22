package com.example.ruts

import com.example.ruts.domain.DeliveryStop
import com.example.ruts.domain.GeoPoint
import com.example.ruts.domain.RouteOptimizer
import com.example.ruts.domain.StopOrderPreference
import com.example.ruts.domain.StopStatus
import org.junit.Assert.*
import org.junit.Test

class ExampleUnitTest {
    @Test
    fun optimizerKeepsEveryStopExactlyOnce() {
        val stops = listOf(
            DeliveryStop(
                id = "a",
                customerName = "A",
                address = "A",
                location = GeoPoint(40.4169, -3.7035),
                orderIndex = 0,
            ),
            DeliveryStop(
                id = "b",
                customerName = "B",
                address = "B",
                location = GeoPoint(40.4154, -3.7074),
                orderIndex = 1,
            ),
            DeliveryStop(
                id = "c",
                customerName = "C",
                address = "C",
                location = GeoPoint(40.4138, -3.6921),
                orderIndex = 2,
            ),
        )

        val optimizedStops = RouteOptimizer.optimize(stops, GeoPoint(40.4168, -3.7038))

        assertEquals(stops.size, optimizedStops.size)
        assertEquals(stops.map { it.id }.toSet(), optimizedStops.map { it.id }.toSet())
        assertEquals(listOf(0, 1, 2), optimizedStops.map { it.orderIndex })
    }

    @Test
    fun optimizerAppendsCompletedStopsAfterPendingStops() {
        val pendingStop = DeliveryStop(
            id = "pending",
            customerName = "Pendiente",
            address = "Pendiente",
            location = GeoPoint(40.4169, -3.7035),
            orderIndex = 0,
        )
        val deliveredStop = DeliveryStop(
            id = "delivered",
            customerName = "Entregada",
            address = "Entregada",
            location = GeoPoint(40.4154, -3.7074),
            orderIndex = 1,
            status = StopStatus.Delivered,
        )

        val optimizedStops = RouteOptimizer.optimize(
            listOf(deliveredStop, pendingStop),
            GeoPoint(40.4168, -3.7038),
        )

        assertEquals("pending", optimizedStops.first().id)
        assertEquals("delivered", optimizedStops.last().id)
    }

    @Test
    fun optimizerRespectsFirstAndLastPreferences() {
        val firstStop = DeliveryStop(
            id = "first",
            customerName = "Primera",
            address = "Primera",
            location = GeoPoint(40.4200, -3.7100),
            orderIndex = 0,
            orderPreference = StopOrderPreference.First,
        )
        val middleStop = DeliveryStop(
            id = "middle",
            customerName = "Media",
            address = "Media",
            location = GeoPoint(40.4154, -3.7074),
            orderIndex = 1,
        )
        val lastStop = DeliveryStop(
            id = "last",
            customerName = "Última",
            address = "Última",
            location = GeoPoint(40.4138, -3.6921),
            orderIndex = 2,
            orderPreference = StopOrderPreference.Last,
        )

        val optimizedStops = RouteOptimizer.optimize(
            listOf(lastStop, middleStop, firstStop),
            GeoPoint(40.4168, -3.7038),
        )

        assertEquals("first", optimizedStops.first().id)
        assertEquals("last", optimizedStops.last().id)
    }

    @Test
    fun optimizerKeepsNearestStopFirstAndImprovesReturnToStart() {
        val nearestStop = DeliveryStop(
            id = "nearest",
            customerName = "Cercana",
            address = "Cercana",
            location = GeoPoint(0.0, 1.0),
            orderIndex = 0,
        )
        val returnStop = DeliveryStop(
            id = "return",
            customerName = "Vuelta",
            address = "Vuelta",
            location = GeoPoint(1.0, 0.0),
            orderIndex = 1,
        )
        val farStop = DeliveryStop(
            id = "far",
            customerName = "Lejana",
            address = "Lejana",
            location = GeoPoint(10.0, 10.0),
            orderIndex = 2,
        )

        val optimizedStops = RouteOptimizer.optimize(
            listOf(nearestStop, returnStop, farStop),
            GeoPoint(0.0, 0.0),
        )

        assertEquals("nearest", optimizedStops.first().id)
        assertEquals("return", optimizedStops.last().id)
    }
}