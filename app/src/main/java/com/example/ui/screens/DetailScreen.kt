package com.example.ui.screens

import android.content.Intent
import android.widget.Toast
import java.util.Locale
import com.example.ui.util.FormatUtils
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.TrendingDown
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import kotlin.math.roundToInt
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.data.model.CryptoCoin
import com.example.data.model.CurrencySetting
import com.example.ui.theme.GreenCrypto
import com.example.ui.theme.RedCrypto
import com.example.ui.viewmodel.CryptoViewModel
import com.example.ui.viewmodel.TradeResult

import com.example.ui.viewmodel.PortfolioUiState
import androidx.compose.ui.tooling.preview.Preview

@Composable
fun DetailScreen(
    viewModel: CryptoViewModel,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val coinState by viewModel.selectedCoin.collectAsState()
    val portfolioState by viewModel.portfolioUiState.collectAsState()
    val selectedCurrency by viewModel.selectedCurrency.collectAsState()

    DetailScreenContent(
        coin = coinState,
        portfolioState = portfolioState,
        selectedCurrency = selectedCurrency,
        onNavigateBack = onNavigateBack,
        onPerformTrade = { coin, isBuy, quantity, onResult ->
            viewModel.performTrade(coin, isBuy, quantity, onResult)
        },
        modifier = modifier
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DetailScreenContent(
    coin: CryptoCoin?,
    portfolioState: PortfolioUiState,
    selectedCurrency: CurrencySetting,
    onNavigateBack: () -> Unit,
    onPerformTrade: (CryptoCoin, Boolean, String, (TradeResult) -> Unit) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var quantityInput by remember { mutableStateOf("") }
    var inputError by remember { mutableStateOf<String?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(coin?.name ?: "Dettaglio", fontWeight = FontWeight.SemiBold) },
                navigationIcon = {
                    IconButton(
                        onClick = onNavigateBack,
                        modifier = Modifier.testTag("detail_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Torna indietro"
                        )
                    }
                },
                actions = {
                    coin?.let { c ->
                        IconButton(
                            onClick = {
                                // Implicit Intent - Share Prices with action ACTION_SEND
                                val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                    type = "text/plain"
                                    putExtra(
                                        Intent.EXTRA_TEXT,
                                        "La criptovaluta ${c.name} (${c.symbol}) viene valutata a ${selectedCurrency.format(c.priceUsd)} (Variazione 24h: ${if (c.percentChange24h >= 0) "+" else ""}${String.format(Locale.getDefault(), "%.2f", c.percentChange24h)}%). Negozia in sicurezza con l'app Simulatore Crypto!"
                                    )
                                }
                                context.startActivity(Intent.createChooser(shareIntent, "Condividi questa quotazione"))
                            },
                            modifier = Modifier.testTag("share_coin_button")
                        ) {
                            Icon(imageVector = Icons.Default.Share, contentDescription = "Condividi asset")
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
        if (coin == null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("Caricamento dettagli asset in corso...")
                }
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(horizontal = 16.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                // Asset Logo and Price Card
                AssetHeaderSection(coin = coin, currencySetting = selectedCurrency)

                Spacer(modifier = Modifier.height(16.dp))

                // Custom Graphic Price Curve using standard Canvas drawing methods
                PriceChartSection(coin = coin, currencySetting = selectedCurrency)

                Spacer(modifier = Modifier.height(16.dp))

                // Self Holdings status card
                HoldingsSection(coin = coin, currencySetting = selectedCurrency)

                Spacer(modifier = Modifier.height(16.dp))

                // Trading Action card with input validations
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("trading_card")
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "Fai un'operazione di trading",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(modifier = Modifier.height(12.dp))

                        // Styled input text field
                        OutlinedTextField(
                            value = quantityInput,
                            onValueChange = {
                                quantityInput = it
                                inputError = null // Reset errors upon typing
                            },
                            label = { Text("Quantità di ${coin.symbol}") },
                            placeholder = { Text("es. 0.05") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            isError = inputError != null,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = MaterialTheme.colorScheme.primary,
                                errorBorderColor = MaterialTheme.colorScheme.error
                            ),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("quantity_input_field"),
                            singleLine = true
                        )

                        inputError?.let { err ->
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = err,
                                color = MaterialTheme.colorScheme.error,
                                style = MaterialTheme.typography.bodySmall,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }

                        // Cash balance display
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "Tuo saldo contante:",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = selectedCurrency.format(portfolioState.cashBalance),
                                modifier = Modifier.testTag("cash_balance_val"),
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }

                        // Valuation indicators
                        val qty = quantityInput.replace(',', '.').toDoubleOrNull() ?: 0.0
                        if (qty > 0.0) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 4.dp),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "Valore totale stimato:",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = selectedCurrency.format(qty * coin.priceUsd),
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            // Buy button with client-side validation check
                            Button(
                                onClick = {
                                    onPerformTrade(coin, true, quantityInput) { res ->
                                        when (res) {
                                            is TradeResult.Success -> {
                                                Toast.makeText(context, res.message, Toast.LENGTH_LONG).show()
                                                quantityInput = ""
                                                inputError = null
                                            }
                                            is TradeResult.Error -> {
                                                inputError = res.message
                                            }
                                        }
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = GreenCrypto),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(48.dp)
                                    .testTag("buy_button"),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text("COMPRA", fontWeight = FontWeight.Bold, color = Color.Black)
                            }

                            // Sell button with client-side validation check
                            Button(
                                onClick = {
                                    onPerformTrade(coin, false, quantityInput) { res ->
                                        when (res) {
                                            is TradeResult.Success -> {
                                                Toast.makeText(context, res.message, Toast.LENGTH_LONG).show()
                                                quantityInput = ""
                                                inputError = null
                                            }
                                            is TradeResult.Error -> {
                                                inputError = res.message
                                            }
                                        }
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = RedCrypto),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(48.dp)
                                    .testTag("sell_button"),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text("VENDI", fontWeight = FontWeight.Bold, color = Color.White)
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun DetailScreenPreview() {
    MaterialTheme {
        DetailScreenContent(
            coin = CryptoCoin(
                id = "bitcoin",
                symbol = "BTC",
                name = "Bitcoin",
                nameId = "bitcoin",
                rank = 1,
                priceUsd = 95000.0,
                percentChange24h = 3.5,
                imageUrl = "",
                volume24h = 45000000000.0,
                quantityOwned = 0.05
            ),
            portfolioState = PortfolioUiState(
                cashBalance = 15000.0,
                totalPortfolioValue = 19750.0,
                totalDeposited = 10000.0
            ),
            selectedCurrency = CurrencySetting.EUR,
            onNavigateBack = {},
            onPerformTrade = { _, _, _, _ -> }
        )
    }
}

@Composable
fun AssetHeaderSection(coin: CryptoCoin, currencySetting: CurrencySetting) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .padding(20.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AsyncImage(
                model = coin.imageUrl,
                contentDescription = "Logo di ${coin.name}",
                modifier = Modifier
                    .size(56.dp)
                    .clip(CircleShape)
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = coin.name,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Surface(
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                        shape = RoundedCornerShape(100.dp)
                    ) {
                        Text(
                            text = "Rank #${coin.rank}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
                Text(
                    text = coin.symbol,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Column(modifier = Modifier, horizontalAlignment = Alignment.End) {
                Text(
                    text = currencySetting.format(coin.priceUsd),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
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
                        style = MaterialTheme.typography.bodyMedium,
                        color = textColor,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

enum class PriceInterval(val label: String, val pointsCount: Int) {
    H24("24h", 24),
    W1("1S", 7),
    M1("1M", 30),
    Y1("1A", 12)
}

data class PricePoint(
    val priceUsd: Double,
    val label: String
)

fun generatePriceHistory(coin: CryptoCoin, interval: PriceInterval): List<PricePoint> {
    val currentPrice = coin.priceUsd
    val seed = (coin.id.hashCode() % 100) / 100.0
    val percentChange = when (interval) {
        PriceInterval.H24 -> coin.percentChange24h
        PriceInterval.W1 -> (coin.percentChange24h * 1.5 + seed * 12.0).coerceIn(-75.0, 250.0)
        PriceInterval.M1 -> (coin.percentChange24h * 3.0 + seed * 35.0).coerceIn(-92.0, 600.0)
        PriceInterval.Y1 -> (coin.percentChange24h * 7.5 + seed * 180.0).coerceIn(-98.0, 1500.0)
    }
    
    val count = interval.pointsCount
    val startPrice = currentPrice / (1.0 + (percentChange / 100.0))
    val points = ArrayList<PricePoint>()
    val randomSeed = coin.id.hashCode().toLong() + interval.ordinal * 1000L
    val random = java.util.Random(randomSeed)
    
    for (i in 0 until count) {
        val fraction = if (count > 1) i.toDouble() / (count - 1) else 1.0
        val basePrice = startPrice + (currentPrice - startPrice) * fraction
        val noiseScale = currentPrice * 0.06
        val noise = if (i == 0 || i == count - 1) {
            0.0
        } else {
            val sineWave = Math.sin(fraction * Math.PI * 4) * noiseScale * 0.25
            val randNoise = (random.nextDouble() - 0.5) * noiseScale
            sineWave + randNoise
        }
        val price = (basePrice + noise).coerceAtLeast(currentPrice * 0.01)
        val label = when (interval) {
            PriceInterval.H24 -> {
                val hoursAgo = count - 1 - i
                if (hoursAgo == 0) "Ora" else "-${hoursAgo}h"
            }
            PriceInterval.W1 -> {
                val daysAgo = count - 1 - i
                if (daysAgo == 0) "Ora" else "-${daysAgo}g"
            }
            PriceInterval.M1 -> {
                val daysAgo = count - 1 - i
                if (daysAgo == 0) "Ora" else if (daysAgo % 5 == 0) "-${daysAgo}g" else ""
            }
            PriceInterval.Y1 -> {
                val monthsAgo = count - 1 - i
                if (monthsAgo == 0) "Ora" else if (monthsAgo % 2 == 0) "-${monthsAgo}m" else ""
            }
        }
        points.add(PricePoint(price, label))
    }
    return points
}

@Composable
fun PriceChartSection(coin: CryptoCoin, currencySetting: CurrencySetting) {
    var selectedInterval by remember { mutableStateOf(PriceInterval.H24) }
    val points = remember(coin, selectedInterval) { generatePriceHistory(coin, selectedInterval) }
    
    // Derived values
    val currentPrice = coin.priceUsd
    val startPrice = points.first().priceUsd
    val percentChange = ((currentPrice - startPrice) / startPrice) * 100.0
    val isPositive = percentChange >= 0
    val chartColor = if (isPositive) GreenCrypto else RedCrypto
    
    val minPrice = remember(points) { points.minOf { it.priceUsd } }
    val maxPrice = remember(points) { points.maxOf { it.priceUsd } }
    
    var activeTouchX by remember { mutableStateOf<Float?>(null) }
    var containerWidth by remember { mutableStateOf(0f) }
    
    val selectedIndex = remember(activeTouchX, points, containerWidth) {
        activeTouchX?.let { touchX ->
            if (containerWidth > 0) {
                val fraction = (touchX / containerWidth).coerceIn(0f, 1f)
                (fraction * (points.size - 1)).roundToInt().coerceIn(0, points.size - 1)
            } else {
                null
            }
        }
    }
    
    val activePoint = selectedIndex?.let { points[it] }

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Header Row: Title and Selector
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Andamento Prezzo",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                
                // Interval selector buttons
                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    PriceInterval.entries.forEach { interval ->
                        val isSelected = selectedInterval == interval
                        Card(
                            onClick = { selectedInterval = interval },
                            shape = RoundedCornerShape(8.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = if (isSelected) MaterialTheme.colorScheme.primary 
                                                else MaterialTheme.colorScheme.surface
                            ),
                            border = BorderStroke(1.dp, if (isSelected) Color.Transparent else MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)),
                            modifier = Modifier.testTag("interval_chip_${interval.name}")
                        ) {
                            Text(
                                text = interval.label,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = if (isSelected) MaterialTheme.colorScheme.onPrimary 
                                        else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // Stats / Hover values Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (activePoint != null) {
                    // Hovering State: display selected values
                    Column {
                        Text(
                            text = "PREZZO SELEZIONATO",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = currencySetting.format(activePoint.priceUsd),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = "ISTANTE",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = if (activePoint.label.isNotEmpty()) activePoint.label else "Dettaglio",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                } else {
                    // Default State: display overall change and stats
                    Column {
                        Text(
                            text = "VARIAZIONE (${selectedInterval.label})",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontWeight = FontWeight.Bold
                        )
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            val sign = if (percentChange >= 0) "+" else ""
                            Text(
                                text = "$sign${String.format(Locale.getDefault(), "%.2f", percentChange)}%",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = chartColor
                            )
                            Icon(
                                imageVector = if (isPositive) Icons.Default.TrendingUp else Icons.Default.TrendingDown,
                                contentDescription = if (isPositive) "Su" else "Giù",
                                tint = chartColor,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = "PREZZO CORRENTE",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = currencySetting.format(currentPrice),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // Custom Drawing Canvas box
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(160.dp)
                    .background(Color.Black.copy(alpha = 0.2f), RoundedCornerShape(12.dp))
                    .onSizeChanged { containerWidth = it.width.toFloat() }
                    .pointerInput(points) {
                        awaitPointerEventScope {
                            while (true) {
                                val event = awaitPointerEvent()
                                val change = event.changes.firstOrNull()
                                if (change != null) {
                                    if (change.pressed) {
                                        activeTouchX = change.position.x
                                        change.consume()
                                    } else {
                                        activeTouchX = null
                                    }
                                }
                            }
                        }
                    }
            ) {
                Canvas(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 12.dp, vertical = 16.dp)
                ) {
                    val width = size.width
                    val height = size.height
                    
                    if (points.size < 2) return@Canvas
                    
                    // Convert points to coordinates
                    val priceMin = minPrice
                    val priceMax = maxPrice
                    val priceRange = if (priceMax > priceMin) priceMax - priceMin else 1.0
                    
                    val coordinates = points.mapIndexed { idx, pt ->
                        val x = idx * (width / (points.size - 1))
                        val y = height - ((pt.priceUsd - priceMin) / priceRange * height).toFloat()
                        Offset(x, y)
                    }
                    
                    // Draw grid background line for max / min / average
                    drawLine(
                        color = Color.White.copy(alpha = 0.15f),
                        start = Offset(0f, 0f),
                        end = Offset(width, 0f),
                        strokeWidth = 1.dp.toPx(),
                        pathEffect = androidx.compose.ui.graphics.PathEffect.dashPathEffect(floatArrayOf(10f, 10f))
                    )
                    drawLine(
                        color = Color.White.copy(alpha = 0.15f),
                        start = Offset(0f, height / 2),
                        end = Offset(width, height / 2),
                        strokeWidth = 1.dp.toPx(),
                        pathEffect = androidx.compose.ui.graphics.PathEffect.dashPathEffect(floatArrayOf(10f, 10f))
                    )
                    drawLine(
                        color = Color.White.copy(alpha = 0.15f),
                        start = Offset(0f, height),
                        end = Offset(width, height),
                        strokeWidth = 1.dp.toPx(),
                        pathEffect = androidx.compose.ui.graphics.PathEffect.dashPathEffect(floatArrayOf(10f, 10f))
                    )
                    
                    // Build path
                    val path = Path()
                    path.moveTo(coordinates[0].x, coordinates[0].y)
                    for (i in 1 until coordinates.size) {
                        val prev = coordinates[i - 1]
                        val curr = coordinates[i]
                        val controlX = (prev.x + curr.x) / 2
                        path.quadraticTo(controlX, prev.y, controlX, curr.y)
                        path.lineTo(curr.x, curr.y)
                    }
                    
                    // Draw stroke
                    drawPath(
                        path = path,
                        color = chartColor,
                        style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round)
                    )
                    
                    // Draw gradient area underneath
                    val brushPath = Path().apply {
                        addPath(path)
                        lineTo(width, height)
                        lineTo(0f, height)
                        close()
                    }
                    drawPath(
                        path = brushPath,
                        brush = Brush.verticalGradient(
                            colors = listOf(chartColor.copy(alpha = 0.3f), Color.Transparent),
                            startY = 0f,
                            endY = height
                        )
                    )
                    
                    // Touch point and horizontal projection drawing
                    selectedIndex?.let { index ->
                        val activeCoord = coordinates[index]
                        
                        // Vertical guideline
                        drawLine(
                            color = Color.White.copy(alpha = 0.4f),
                            start = Offset(activeCoord.x, 0f),
                            end = Offset(activeCoord.x, height),
                            strokeWidth = 1.5.dp.toPx(),
                            pathEffect = androidx.compose.ui.graphics.PathEffect.dashPathEffect(floatArrayOf(15f, 10f))
                        )
                        
                        // Circle background glow
                        drawCircle(
                            color = chartColor.copy(alpha = 0.3f),
                            radius = 12.dp.toPx(),
                            center = activeCoord
                        )
                        
                        // Core touch dot
                        drawCircle(
                            color = chartColor,
                            radius = 6.dp.toPx(),
                            center = activeCoord
                        )
                        drawCircle(
                            color = Color.White,
                            radius = 2.dp.toPx(),
                            center = activeCoord
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // Additional metrics / details block for premium, realistic aesthetic
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = "APERTURA",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                    )
                    Text(
                        text = currencySetting.format(startPrice),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "MIN",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                    )
                    Text(
                        text = currencySetting.format(minPrice),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = RedCrypto.copy(alpha = 0.9f)
                    )
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "MAX",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                    )
                    Text(
                        text = currencySetting.format(maxPrice),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = GreenCrypto.copy(alpha = 0.9f)
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Tieni premuto e scorri sul grafico per visualizzare il valore in un dato istante.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
fun HoldingsSection(coin: CryptoCoin, currencySetting: CurrencySetting) {
    val owned = coin.quantityOwned > 0

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (owned) MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.4f)
            else MaterialTheme.colorScheme.surfaceVariant
        ),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Tuo Portafoglio per questo asset",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(12.dp))

            if (!owned) {
                Text(
                    text = "Non possiedi ancora questa criptovaluta. Acquistane una parte usando il modulo sottostante.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(
                            text = "Quantità Posseduta",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "${FormatUtils.formatCryptoQuantity(coin.quantityOwned)} ${coin.symbol}",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = "Valore Attuale",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = currencySetting.format(coin.totalValue),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(
                            text = "Prezzo Medio d'Acquisto",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = currencySetting.format(coin.averagePurchasePrice),
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = "Profitto / Perdita (ROI)",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        val profit = coin.profitLoss
                        val profitColor = if (profit >= 0) GreenCrypto else RedCrypto
                        val sign = if (profit >= 0) "+" else ""
                        Text(
                            text = if (profit >= 0) "+${currencySetting.format(profit)} ($sign${String.format(Locale.getDefault(), "%.2f", coin.profitLossPercent)}%)" else "${currencySetting.format(profit)} ($sign${String.format(Locale.getDefault(), "%.2f", coin.profitLossPercent)}%)",
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Bold,
                            color = profitColor
                        )
                    }
                }
            }
        }
    }
}
