package com.example.ui.screens

import android.content.Intent
import android.widget.Toast
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
import androidx.compose.ui.graphics.drawscope.Stroke
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DetailScreen(
    viewModel: CryptoViewModel,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val coinState by viewModel.selectedCoin.collectAsState()
    val portfolioState by viewModel.portfolioUiState.collectAsState()
    val selectedCurrency by viewModel.selectedCurrency.collectAsState()

    var quantityInput by remember { mutableStateOf("") }
    var inputError by remember { mutableStateOf<String?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(coinState?.name ?: "Dettaglio", fontWeight = FontWeight.SemiBold) },
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
                    coinState?.let { coin ->
                        IconButton(
                            onClick = {
                                // Implicit Intent - Share Prices with action ACTION_SEND
                                val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                    type = "text/plain"
                                    putExtra(
                                        Intent.EXTRA_TEXT,
                                        "La criptovaluta ${coin.name} (${coin.symbol}) viene valutata a ${selectedCurrency.format(coin.priceUsd)} (Variazione 24h: ${if (coin.percentChange24h >= 0) "+" else ""}${String.format("%.2f", coin.percentChange24h)}%). Negozia in sicurezza con l'app Simulatore Crypto!"
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
        val coin = coinState
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
                PriceChartSection(coin = coin)

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
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
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
                        val qty = quantityInput.toDoubleOrNull() ?: 0.0
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
                                    viewModel.performTrade(coin, isBuy = true, quantityInput) { res ->
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
                                    viewModel.performTrade(coin, isBuy = false, quantityInput) { res ->
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
                        text = "${if (isPositive) "+" else ""}${String.format("%.2f", coin.percentChange24h)}%",
                        style = MaterialTheme.typography.bodyMedium,
                        color = textColor,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
fun PriceChartSection(coin: CryptoCoin) {
    val isPositive = coin.percentChange24h >= 0
    val chartColor = if (isPositive) GreenCrypto else RedCrypto

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Andamento Prezzo (24h)",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(16.dp))

            // Custom Drawing Canvas of waves indicating fluctuations
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(140.dp)
                    .background(Color.Black.copy(alpha = 0.2f), RoundedCornerShape(12.dp))
            ) {
                Canvas(modifier = Modifier.fillMaxSize().padding(horizontal = 8.dp, vertical = 12.dp)) {
                    val path = Path()
                    val width = size.width
                    val height = size.height

                    // Generate waves based on changes to provide an aesthetic visual fluctuation
                    val points = mutableListOf<Offset>()
                    val stepX = width / 9f
                    val seedPoints = if (isPositive) {
                        listOf(0.9f, 0.85f, 0.7f, 0.75f, 0.5f, 0.45f, 0.3f, 0.25f, 0.2f, 0.1f)
                    } else {
                        listOf(0.1f, 0.15f, 0.35f, 0.3f, 0.55f, 0.5f, 0.75f, 0.7f, 0.85f, 0.9f)
                    }

                    for (i in seedPoints.indices) {
                        val x = i * stepX
                        val y = seedPoints[i] * height
                        points.add(Offset(x, y))
                    }

                    path.moveTo(points[0].x, points[0].y)
                    for (i in 1 until points.size) {
                        val prevPoint = points[i - 1]
                        val currentPoint = points[i]
                        // Smooth bezier control points
                        val controlX = (prevPoint.x + currentPoint.x) / 2
                        path.quadraticTo(controlX, prevPoint.y, controlX, currentPoint.y)
                        path.lineTo(currentPoint.x, currentPoint.y)
                    }

                    // Stroke Path drawing
                    drawPath(
                        path = path,
                        color = chartColor,
                        style = Stroke(width = 3.dp.toPx())
                    )

                    // Optional filled linear brush gradient underneath the chart path
                    val brushPath = Path().apply {
                        addPath(path)
                        lineTo(width, height)
                        lineTo(0f, height)
                        close()
                    }
                    drawPath(
                        path = brushPath,
                        brush = Brush.verticalGradient(
                            colors = listOf(chartColor.copy(alpha = 0.35f), Color.Transparent),
                            startY = 0f,
                            endY = height
                        )
                    )
                }
            }
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
                            text = "${String.format("%.4f", coin.quantityOwned)} ${coin.symbol}",
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
                            text = if (profit >= 0) "+${currencySetting.format(profit)} ($sign${String.format("%.2f", coin.profitLossPercent)}%)" else "${currencySetting.format(profit)} ($sign${String.format("%.2f", coin.profitLossPercent)}%)",
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
