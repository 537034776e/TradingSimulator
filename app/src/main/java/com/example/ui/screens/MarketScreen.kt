package com.example.ui.screens

import java.util.Locale
import com.example.ui.util.FormatUtils
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Sort
import androidx.compose.material.icons.filled.TrendingDown
import androidx.compose.material.icons.filled.TrendingUp
import com.example.ui.viewmodel.CoinSortOption
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material.icons.filled.ArrowDropDown
import com.example.data.model.CurrencySetting
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.data.model.CryptoCoin
import com.example.ui.theme.GreenCrypto
import com.example.ui.theme.RedCrypto
import com.example.ui.viewmodel.CryptoViewModel
import com.example.ui.viewmodel.MarketUiState

import androidx.compose.ui.tooling.preview.Preview

@Composable
fun MarketScreen(
    viewModel: CryptoViewModel,
    onNavigateToDetail: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val searchVal by viewModel.searchQuery.collectAsState()
    val marketState by viewModel.marketUiState.collectAsState()
    val selectedCurrency by viewModel.selectedCurrency.collectAsState()
    val selectedSortOption by viewModel.selectedSortOption.collectAsState()

    MarketScreenContent(
        searchVal = searchVal,
        onSearchValChange = { viewModel.setSearchQuery(it) },
        marketState = marketState,
        selectedCurrency = selectedCurrency,
        selectedSortOption = selectedSortOption,
        onCurrencySelected = { viewModel.setSelectedCurrency(it) },
        onSortOptionSelected = { viewModel.setSelectedSortOption(it) },
        onRefresh = { viewModel.refreshMarket() },
        onCoinClick = { coin ->
            viewModel.selectCoinById(coin.id)
            onNavigateToDetail(coin.id)
        },
        modifier = modifier
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MarketScreenContent(
    searchVal: String,
    onSearchValChange: (String) -> Unit,
    marketState: MarketUiState,
    selectedCurrency: CurrencySetting,
    selectedSortOption: CoinSortOption,
    onCurrencySelected: (CurrencySetting) -> Unit,
    onSortOptionSelected: (CoinSortOption) -> Unit,
    onRefresh: () -> Unit,
    onCoinClick: (CryptoCoin) -> Unit,
    modifier: Modifier = Modifier
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(end = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Mercato Cripto",
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.titleLarge
                        )
                        CurrencyDropdown(
                            selectedCurrency = selectedCurrency,
                            onCurrencySelected = onCurrencySelected
                        )
                    }
                },
                actions = {
                    var sortMenuExpanded by remember { mutableStateOf(false) }
                    Box {
                        IconButton(
                            onClick = { sortMenuExpanded = true },
                            modifier = Modifier.testTag("sort_market_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Sort,
                                contentDescription = "Ordina criptovalute"
                            )
                        }
                        DropdownMenu(
                            expanded = sortMenuExpanded,
                            onDismissRequest = { sortMenuExpanded = false },
                            modifier = Modifier.background(MaterialTheme.colorScheme.surfaceVariant)
                        ) {
                            CoinSortOption.entries.forEach { option ->
                                val isSelected = option == selectedSortOption
                                DropdownMenuItem(
                                    text = {
                                        Text(
                                            text = option.label,
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                            color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                                        )
                                    },
                                    onClick = {
                                        onSortOptionSelected(option)
                                        sortMenuExpanded = false
                                    },
                                    modifier = Modifier.testTag("sort_option_${option.name}")
                                )
                            }
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    titleContentColor = MaterialTheme.colorScheme.onBackground
                )
            )
        },
        modifier = modifier
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp)
        ) {
            // Search Input box with M3 styling
            OutlinedTextField(
                value = searchVal,
                onValueChange = onSearchValChange,
                placeholder = { Text("Cerca crypto o simbolo...") },
                leadingIcon = {
                    Icon(imageVector = Icons.Default.Search, contentDescription = "Pulsante di ricerca")
                },
                trailingIcon = {
                    if (searchVal.isNotEmpty()) {
                        IconButton(onClick = { onSearchValChange("") }) {
                            Icon(imageVector = Icons.Default.Clear, contentDescription = "Cancella")
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp)
                    .testTag("search_text_field"),
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outline
                ),
                singleLine = true
            )

            // Market State visual representations (Loading, Error, Success list, Empty list states)
            when {
                marketState.isLoading && marketState.coins.isEmpty() -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.testTag("loading_indicator")
                        )
                    }
                }
                marketState.errorMessage != null && marketState.coins.isEmpty() -> {
                    ErrorStateView(
                        message = marketState.errorMessage ?: "Errore indefinito",
                        onRetry = onRefresh
                    )
                }
                marketState.coins.isEmpty() -> {
                    EmptyStateView(query = searchVal)
                }
                else -> {
                    if (marketState.errorMessage != null) {
                        // Small offline badge at top if we have cached data but net error
                        Card(
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 8.dp),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Info,
                                    contentDescription = "Warning info status",
                                    tint = MaterialTheme.colorScheme.onErrorContainer
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Sei Offline. Utilizzando l'ultimo listino memorizzato.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onErrorContainer
                                )
                            }
                        }
                    }

                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier
                            .fillMaxSize()
                            .testTag("coins_lazy_column")
                    ) {
                        items(marketState.coins, key = { it.id }) { coin ->
                            CoinRowItem(
                                coin = coin,
                                currencySetting = selectedCurrency,
                                onClick = { onCoinClick(coin) }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun MarketScreenPreview() {
    MaterialTheme {
        MarketScreenContent(
            searchVal = "",
            onSearchValChange = {},
            marketState = MarketUiState(
                isLoading = false,
                coins = listOf(
                    CryptoCoin(
                        id = "bitcoin",
                        symbol = "BTC",
                        name = "Bitcoin",
                        nameId = "bitcoin",
                        rank = 1,
                        priceUsd = 95000.0,
                        percentChange24h = 1.2,
                        imageUrl = "",
                        volume24h = 45000000000.0
                    ),
                    CryptoCoin(
                        id = "ethereum",
                        symbol = "ETH",
                        name = "Ethereum",
                        nameId = "ethereum",
                        rank = 2,
                        priceUsd = 3400.0,
                        percentChange24h = -0.5,
                        imageUrl = "",
                        volume24h = 20000000000.0
                    ),
                    CryptoCoin(
                        id = "aave",
                        symbol = "AAVE",
                        name = "Aave",
                        nameId = "aave",
                        rank = 50,
                        priceUsd = 140.0,
                        percentChange24h = 3.4,
                        imageUrl = "",
                        volume24h = 250000000.0
                    )
                )
            ),
            selectedCurrency = CurrencySetting.EUR,
            selectedSortOption = CoinSortOption.NAME_ASC,
            onCurrencySelected = {},
            onSortOptionSelected = {},
            onRefresh = {},
            onCoinClick = {}
        )
    }
}

@Composable
fun CoinRowItem(
    coin: CryptoCoin,
    currencySetting: CurrencySetting,
    onClick: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        ),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .testTag("coin_card_${coin.symbol}"),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Coil Async logo loader
            AsyncImage(
                model = coin.imageUrl,
                contentDescription = "Logo di ${coin.name}",
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
            )

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = coin.name,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.15f)
                        ),
                        shape = RoundedCornerShape(4.dp)
                    ) {
                        Text(
                            text = "#${coin.rank}",
                            style = MaterialTheme.typography.labelSmall,
                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                        )
                    }
                }
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = coin.symbol,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "•",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                    )
                    Text(
                        text = "Vol: ${formatVolume(coin.volume24h, currencySetting)}",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                    )
                }
            }

            Column(
                horizontalAlignment = Alignment.End
            ) {
                Text(
                    text = currencySetting.format(coin.priceUsd),
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(2.dp))

                val isPositive = coin.percentChange24h >= 0
                val textColor = if (isPositive) GreenCrypto else RedCrypto
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = if (isPositive) Icons.Default.TrendingUp else Icons.Default.TrendingDown,
                        contentDescription = null,
                        tint = textColor,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(2.dp))
                    Text(
                        text = "${if (isPositive) "+" else ""}${String.format(Locale.getDefault(), "%.2f", coin.percentChange24h)}%",
                        style = MaterialTheme.typography.labelMedium,
                        color = textColor,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                if (coin.quantityOwned > 0) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Surface(
                        color = MaterialTheme.colorScheme.secondary.copy(alpha = 0.15f),
                        shape = RoundedCornerShape(100.dp)
                    ) {
                        Text(
                            text = "Possiedi: ${FormatUtils.formatCryptoQuantity(coin.quantityOwned)}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.secondary,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ErrorStateView(
    message: String,
    onRetry: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Card(
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.errorContainer
            ),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Qualcosa è andato storto",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onErrorContainer
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = message,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onErrorContainer,
                    textAlign = TextAlign.Center
                )
            }
        }
        Spacer(modifier = Modifier.height(24.dp))
        Button(
            onClick = onRetry,
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
        ) {
            Icon(imageVector = Icons.Default.Refresh, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Riprova ad aggiornare")
        }
    }
}

@Composable
fun EmptyStateView(query: String) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "🔍",
            style = MaterialTheme.typography.displayMedium
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "Nessun risultato per: \"$query\"",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Verifica il nome della criptovaluta inserito o prova un simbolo diverso (es. BTC).",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
fun CurrencyDropdown(
    selectedCurrency: CurrencySetting,
    onCurrencySelected: (CurrencySetting) -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }

    Box(modifier = modifier) {
        Card(
            onClick = { expanded = true },
            shape = RoundedCornerShape(8.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant
            ),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
            modifier = Modifier.testTag("currency_dropdown_trigger")
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = if (selectedCurrency.isSymbolSuffix) selectedCurrency.code else "${selectedCurrency.symbol} ${selectedCurrency.code}",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Icon(
                    imageVector = Icons.Default.ArrowDropDown,
                    contentDescription = "Cambia valuta",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(18.dp)
                )
            }
        }

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier.background(MaterialTheme.colorScheme.surfaceVariant)
        ) {
            CurrencySetting.entries.forEach { currency ->
                DropdownMenuItem(
                    text = {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = currency.symbol,
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.Bold,
                                color = if (currency == selectedCurrency) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = currency.code,
                                style = MaterialTheme.typography.bodyMedium,
                                color = if (currency == selectedCurrency) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    },
                    onClick = {
                        onCurrencySelected(currency)
                        expanded = false
                    },
                    modifier = Modifier.testTag("currency_item_${currency.code}")
                )
            }
        }
    }
}

fun formatVolume(valueInUsd: Double, currency: CurrencySetting): String {
    val converted = valueInUsd * currency.usdToCurrencyRate
    return when {
        converted >= 1_000_000_000 -> {
            val valStr = String.format(Locale.getDefault(), "%.2f", converted / 1_000_000_000)
            if (currency.isSymbolSuffix) "$valStr Mld ${currency.symbol}" else "${currency.symbol}$valStr Mld"
        }
        converted >= 1_000_000 -> {
            val valStr = String.format(Locale.getDefault(), "%.2f", converted / 1_000_000)
            if (currency.isSymbolSuffix) "$valStr Mn ${currency.symbol}" else "${currency.symbol}$valStr Mn"
        }
        else -> {
            currency.format(valueInUsd)
        }
    }
}

