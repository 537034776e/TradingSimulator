package com.example.ui.navigation

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.ShowChart
import androidx.compose.material.icons.outlined.AccountBalanceWallet
import androidx.compose.material.icons.outlined.ShowChart
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.ui.screens.DetailScreen
import com.example.ui.screens.MarketScreen
import com.example.ui.screens.PortfolioScreen
import com.example.ui.viewmodel.CryptoViewModel

sealed class NavigationItem(
    val route: String,
    val title: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
    val testTag: String
) {
    object Market : NavigationItem(
        route = "market",
        title = "Mercato",
        selectedIcon = Icons.Filled.ShowChart,
        unselectedIcon = Icons.Outlined.ShowChart,
        testTag = "bottom_tab_market"
    )

    object Portfolio : NavigationItem(
        route = "portfolio",
        title = "Portafoglio",
        selectedIcon = Icons.Filled.AccountBalanceWallet,
        unselectedIcon = Icons.Outlined.AccountBalanceWallet,
        testTag = "bottom_tab_portfolio"
    )
}

@Composable
fun CryptoAppNavigation(
    viewModel: CryptoViewModel,
    modifier: Modifier = Modifier,
    navController: NavHostController = rememberNavController()
) {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    // Hide Bottom Navigation if on Detail screen
    val showBottomBar = currentRoute?.startsWith("detail") == false

    Scaffold(
        bottomBar = {
            if (showBottomBar) {
                val items = listOf(NavigationItem.Market, NavigationItem.Portfolio)
                NavigationBar(
                    modifier = Modifier.testTag("app_navigation_bar"),
                    tonalElevation = 8.dp
                ) {
                    items.forEach { item ->
                        val isSelected = currentRoute == item.route
                        NavigationBarItem(
                            selected = isSelected,
                            onClick = {
                                if (currentRoute != item.route) {
                                    navController.navigate(item.route) {
                                        popUpTo(navController.graph.startDestinationId) {
                                            saveState = true
                                        }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                }
                            },
                            icon = {
                                Icon(
                                    imageVector = if (isSelected) item.selectedIcon else item.unselectedIcon,
                                    contentDescription = item.title
                                )
                            },
                            label = { Text(text = item.title) },
                            modifier = Modifier.testTag(item.testTag)
                        )
                    }
                }
            }
        },
        modifier = modifier
    ) { paddingValues ->
        NavHost(
            navController = navController,
            startDestination = NavigationItem.Market.route,
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Market List Screen Route
            composable(route = NavigationItem.Market.route) {
                MarketScreen(
                    viewModel = viewModel,
                    onNavigateToDetail = { coinId ->
                        navController.navigate("detail/$coinId")
                    }
                )
            }

            // Portfolio Assets Screen Route
            composable(route = NavigationItem.Portfolio.route) {
                PortfolioScreen(
                    viewModel = viewModel,
                    onNavigateToCoin = { coinId ->
                        navController.navigate("detail/$coinId")
                    }
                )
            }

            // Coin Detail Screen Route
            composable(
                route = "detail/{coinId}",
                arguments = listOf(
                    navArgument("coinId") {
                        type = NavType.StringType
                    }
                )
            ) { backStackEntry ->
                val coinId = backStackEntry.arguments?.getString("coinId") ?: ""
                DetailScreen(
                    viewModel = viewModel,
                    onNavigateBack = {
                        navController.popBackStack()
                    }
                )
            }
        }
    }
}
