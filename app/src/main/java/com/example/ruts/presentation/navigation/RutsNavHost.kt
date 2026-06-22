package com.example.ruts.presentation.navigation

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.ruts.data.RouteRepository
import com.example.ruts.presentation.screens.CreateRouteScreen
import com.example.ruts.presentation.screens.RouteDetailScreen
import com.example.ruts.presentation.screens.RouteEditorScreen

object Routes {
    const val Home = "home"
    const val RouteDetail = "route_detail/{routeId}"
    const val CreateRoute = "create_route"
    const val RouteEditor = "route_editor/{routeId}"

    fun routeDetail(routeId: String) = "route_detail/$routeId"
    fun routeEditor(routeId: String) = "route_editor/$routeId"
}

@Composable
fun RutsNavHost(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val repository = remember { RouteRepository(context) }
    val navController = rememberNavController()

    val startDestination = repository.getLastRouteId()?.let { Routes.routeDetail(it) } ?: Routes.Home

    NavHost(
        navController = navController,
        startDestination = startDestination,
        modifier = modifier.fillMaxSize(),
    ) {
        composable(Routes.Home) {
            RouteDetailScreen(
                routeId = "",
                repository = repository,
                onCreateRoute = { navController.navigate(Routes.CreateRoute) },
                onEditRoute = { id -> navController.navigate(Routes.routeEditor(id)) },
                onRouteSelected = { selectedId ->
                    navController.navigate(Routes.routeDetail(selectedId)) {
                        popUpTo(Routes.Home) { inclusive = true }
                        launchSingleTop = true
                    }
                },
                onRouteDeleted = {
                    navController.navigate(Routes.Home) {
                        popUpTo(Routes.Home) { inclusive = true }
                        launchSingleTop = true
                    }
                },
            )
        }

        composable(
            route = Routes.RouteDetail,
            arguments = listOf(navArgument("routeId") { type = NavType.StringType }),
        ) { backStackEntry ->
            val routeId = backStackEntry.arguments?.getString("routeId").orEmpty()

            RouteDetailScreen(
                routeId = routeId,
                repository = repository,
                onCreateRoute = {
                    navController.navigate(Routes.CreateRoute)
                },
                onEditRoute = { id ->
                    navController.navigate(Routes.routeEditor(id))
                },
                onRouteSelected = { selectedId ->
                    navController.navigate(Routes.routeDetail(selectedId)) {
                        popUpTo(Routes.routeDetail(routeId)) { inclusive = true }
                        launchSingleTop = true
                    }
                },
                onRouteDeleted = {
                    navController.navigate(Routes.Home) {
                        popUpTo(0) { inclusive = true }
                        launchSingleTop = true
                    }
                },
            )
        }

        composable(Routes.CreateRoute) {
            CreateRouteScreen(
                routes = repository.getAllRoutes(),
                onBack = {
                    if (navController.previousBackStackEntry != null) {
                        navController.popBackStack()
                    } else {
                        val lastId = repository.getLastRouteId()
                        if (lastId != null) {
                            navController.navigate(Routes.routeDetail(lastId)) {
                                popUpTo(Routes.CreateRoute) { inclusive = true }
                            }
                        } else {
                            navController.navigate(Routes.Home) {
                                popUpTo(Routes.CreateRoute) { inclusive = true }
                            }
                        }
                    }
                },
                onConfirm = { createdAtMillis, routeName ->
                    val route = repository.createRoute(
                        createdAtMillis = createdAtMillis,
                        name = routeName,
                    )
                    navController.navigate(Routes.routeEditor(route.id)) {
                        popUpTo(Routes.CreateRoute) { inclusive = true }
                    }
                },
            )
        }

        composable(
            route = Routes.RouteEditor,
            arguments = listOf(navArgument("routeId") { type = NavType.StringType }),
        ) { backStackEntry ->
            val routeId = backStackEntry.arguments?.getString("routeId").orEmpty()

            RouteEditorScreen(
                routeId = routeId,
                repository = repository,
                onBack = {
                    navController.navigate(Routes.routeDetail(routeId)) {
                        popUpTo(Routes.routeEditor(routeId)) { inclusive = true }
                        launchSingleTop = true
                    }
                },
                onFinished = {
                    navController.navigate(Routes.routeDetail(routeId)) {
                        popUpTo(Routes.routeEditor(routeId)) { inclusive = true }
                        launchSingleTop = true
                    }
                },
            )
        }
    }
}
