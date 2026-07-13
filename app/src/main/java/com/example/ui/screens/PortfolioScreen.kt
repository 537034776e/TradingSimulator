package com.example.ui.screens

import android.content.Intent
import android.widget.Toast
import java.util.Locale
import com.example.ui.util.FormatUtils
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import android.text.format.DateFormat
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.ShowChart
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.data.local.CryptoHoldingEntity
import com.example.data.local.TransactionEntity
import com.example.data.model.CurrencySetting
import com.example.ui.theme.GreenCrypto
import com.example.ui.theme.RedCrypto
import com.example.ui.viewmodel.CryptoViewModel
import java.util.Date
import com.example.data.model.CryptoCoin
import com.example.ui.viewmodel.MarketUiState
import com.example.ui.viewmodel.PortfolioUiState
import com.example.ui.viewmodel.TradeResult
import com.example.ui.theme.MyApplicationTheme
import androidx.compose.ui.tooling.preview.Preview

@Composable
fun PortfolioScreen(
    viewModel: CryptoViewModel,
    onNavigateToCoin: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val portfolioState by viewModel.portfolioUiState.collectAsState()
    val marketState by viewModel.marketUiState.collectAsState()
    val selectedCurrency by viewModel.selectedCurrency.collectAsState()

    val exportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("text/csv")
    ) { uri ->
        if (uri != null) {
            try {
                context.contentResolver.openOutputStream(uri)?.use { outputStream ->
                    val csvContent = buildCsvContent(
                        portfolioState = portfolioState,
                        marketCoins = marketState.coins,
                        currencySetting = selectedCurrency
                    )
                    outputStream.write(csvContent.toByteArray(Charsets.UTF_8))
                }
                Toast.makeText(context, "Esportazione completata con successo!", Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                Toast.makeText(context, "Errore durante l'esportazione: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
            }
        }
    }

    PortfolioScreenContent(
        portfolioState = portfolioState,
        marketState = marketState,
        selectedCurrency = selectedCurrency,
        onCurrencySelected = { viewModel.setSelectedCurrency(it) },
        onSimulateDeposit = { viewModel.simulateDeposit(it) },
        onSetExactBalance = { viewModel.setExactBalance(it) },
        onPerformConversion = { source, target, amount, coins, onResult ->
            viewModel.performConversion(source, target, amount, coins, onResult)
        },
        onExportClick = {
            exportLauncher.launch("portafoglio_crypto.csv")
        },
        onNavigateToCoin = onNavigateToCoin,
        modifier = modifier
    )
}

@Composable
fun PortfolioScreenContent(
    portfolioState: PortfolioUiState,
    marketState: MarketUiState,
    selectedCurrency: CurrencySetting,
    onCurrencySelected: (CurrencySetting) -> Unit,
    onSimulateDeposit: (Double) -> Unit,
    onSetExactBalance: (Double) -> Unit,
    onPerformConversion: (String, String, String, List<CryptoCoin>, (TradeResult) -> Unit) -> Unit,
    onExportClick: () -> Unit,
    onNavigateToCoin: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var selectedTab by remember { mutableStateOf(0) }
    val tabs = listOf("I Miei Asset", "Cronologia", "Converti")

    var showDepositDialog by remember { mutableStateOf(false) }
    var depositAmountInput by remember { mutableStateOf("") }
    var isSetExactMode by remember { mutableStateOf(false) }
    var dialogError by remember { mutableStateOf<String?>(null) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Portfolio Asset Value Card with glowing header (High Density Outline Card)
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
        ) {
            Column(
                modifier = Modifier.padding(24.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Bilancio Simulazione",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                    )

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        CurrencyDropdown(
                            selectedCurrency = selectedCurrency,
                            onCurrencySelected = onCurrencySelected
                        )

                        // CSV Export Button
                        IconButton(
                            onClick = onExportClick,
                            modifier = Modifier.testTag("export_portfolio_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.FileDownload, 
                                contentDescription = "Esporta in CSV",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = selectedCurrency.format(portfolioState.totalPortfolioValue),
                    style = MaterialTheme.typography.displaySmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )

                // Variazione percentuale rispetto al totale inizialmente versato
                val totalDeposited = portfolioState.totalDeposited
                val variationPercent = if (totalDeposited > 0) {
                    ((portfolioState.totalPortfolioValue - totalDeposited) / totalDeposited) * 100.0
                } else {
                    0.0
                }
                val isPositive = variationPercent >= 0.0
                val sign = if (isPositive) "+" else ""
                val variationColor = if (isPositive) GreenCrypto else RedCrypto
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(top = 4.dp)
                ) {
                    Text(
                        text = "$sign${String.format(Locale.getDefault(), "%.2f", variationPercent)}%",
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Bold,
                        color = variationColor
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "rispetto al versato (${selectedCurrency.format(totalDeposited)})",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(
                            text = "Fondi Contanti (${selectedCurrency.code})",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                        )
                        Text(
                            text = selectedCurrency.format(portfolioState.cashBalance),
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }

                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = "Valore Criptovalute",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                        )
                        val cryptoVal = portfolioState.totalPortfolioValue - portfolioState.cashBalance
                        Text(
                            text = selectedCurrency.format(cryptoVal),
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.secondary
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Button(
                    onClick = { showDepositDialog = true },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("simulate_deposit_button"),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                        contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                ) {
                    Icon(
                        imageVector = Icons.Default.AccountBalance,
                        contentDescription = "Simula Bonifico",
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Simula Bonifico / Modifica Saldo",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        // TabLayout switcher
        TabRow(
            selectedTabIndex = selectedTab,
            containerColor = Color.Transparent,
            contentColor = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(bottom = 12.dp)
        ) {
            tabs.forEachIndexed { index, title ->
                Tab(
                    selected = selectedTab == index,
                    onClick = { selectedTab = index },
                    modifier = Modifier.testTag("portfolio_tab_$index")
                ) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = if (selectedTab == index) FontWeight.Bold else FontWeight.Normal,
                        modifier = Modifier.padding(vertical = 12.dp)
                    )
                }
            }
        }

        // Tab Content
        when (selectedTab) {
            0 -> MyAssetsTab(
                holdings = portfolioState.holdings,
                marketCoins = marketState.coins,
                currencySetting = selectedCurrency,
                onNavigateToCoin = onNavigateToCoin
            )
            1 -> TransactionsTab(
                transactions = portfolioState.transactions,
                currencySetting = selectedCurrency
            )
            2 -> ConvertTab(
                portfolioState = portfolioState,
                marketCoins = marketState.coins,
                selectedCurrency = selectedCurrency,
                onPerformConversion = onPerformConversion
            )
        }

        if (showDepositDialog) {
            AlertDialog(
                onDismissRequest = { 
                    showDepositDialog = false
                    depositAmountInput = ""
                    isSetExactMode = false
                    dialogError = null
                },
                title = {
                    Text(
                        text = if (isSetExactMode) "Modifica Saldo Contanti" else "Simula Bonifico",
                        fontWeight = FontWeight.Bold
                    )
                },
                text = {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = if (isSetExactMode) 
                                "Imposta l'importo esatto del tuo saldo contante in ${selectedCurrency.code}." 
                                else "Simula un bonifico sul tuo conto per ricaricare i fondi.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            if (!isSetExactMode) {
                                Button(
                                    onClick = { isSetExactMode = false; dialogError = null },
                                    modifier = Modifier.weight(1.5f)
                                ) {
                                    Text("Bonifico", style = MaterialTheme.typography.bodyMedium)
                                }
                                OutlinedButton(
                                    onClick = { isSetExactMode = true; dialogError = null },
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text("Imposta Saldo", style = MaterialTheme.typography.bodyMedium)
                                }
                            } else {
                                OutlinedButton(
                                    onClick = { isSetExactMode = false; dialogError = null },
                                    modifier = Modifier.weight(1.5f)
                                ) {
                                    Text("Bonifico", style = MaterialTheme.typography.bodyMedium)
                                }
                                Button(
                                    onClick = { isSetExactMode = true; dialogError = null },
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text("Imposta Saldo", style = MaterialTheme.typography.bodyMedium)
                                }
                            }
                        }

                        if (!isSetExactMode) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                listOf(100.0, 1000.0, 5000.0).forEach { amount ->
                                    val amountInSelected = amount * selectedCurrency.usdToCurrencyRate
                                    val formattedShortcut = if (selectedCurrency.isSymbolSuffix) {
                                        "${String.format(Locale.getDefault(), "%.0f", amountInSelected)} ${selectedCurrency.symbol}"
                                    } else {
                                        "${selectedCurrency.symbol}${String.format(Locale.getDefault(), "%.0f", amountInSelected)}"
                                    }
                                    OutlinedButton(
                                        onClick = {
                                            val usdAmount = amount
                                            onSimulateDeposit(usdAmount)
                                            showDepositDialog = false
                                            depositAmountInput = ""
                                            dialogError = null
                                        },
                                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Text(
                                            text = "+$formattedShortcut",
                                            style = MaterialTheme.typography.labelMedium
                                        )
                                    }
                                }
                            }
                        }

                        OutlinedTextField(
                            value = depositAmountInput,
                            onValueChange = {
                                depositAmountInput = it
                                dialogError = null
                            },
                            label = { 
                                Text(
                                    if (isSetExactMode) "Nuovo saldo (${selectedCurrency.code})" 
                                    else "Importo bonifico (${selectedCurrency.code})"
                                ) 
                            },
                            placeholder = { Text("0.00") },
                            isError = dialogError != null,
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Decimal,
                                imeAction = ImeAction.Done
                            ),
                            modifier = Modifier.fillMaxWidth().testTag("deposit_amount_field")
                        )

                        if (dialogError != null) {
                            Text(
                                text = dialogError ?: "",
                                color = MaterialTheme.colorScheme.error,
                                style = MaterialTheme.typography.labelSmall
                            )
                        }
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            val valueEntered = depositAmountInput.toDoubleOrNull()
                            if (valueEntered == null || valueEntered <= 0) {
                                dialogError = "Inserisci un importo valido e maggiore di zero."
                                return@Button
                            }

                            val usdValue = valueEntered / selectedCurrency.usdToCurrencyRate
                            if (isSetExactMode) {
                                if (usdValue < 0) {
                                    dialogError = "Il saldo non può essere negativo."
                                    return@Button
                                }
                                onSetExactBalance(usdValue)
                            } else {
                                onSimulateDeposit(usdValue)
                            }
                            showDepositDialog = false
                            depositAmountInput = ""
                            dialogError = null
                        },
                        modifier = Modifier.testTag("deposit_confirm_button")
                    ) {
                        Text("Conferma")
                    }
                },
                dismissButton = {
                    TextButton(
                        onClick = {
                            showDepositDialog = false
                            depositAmountInput = ""
                            isSetExactMode = false
                            dialogError = null
                        }
                    ) {
                        Text("Annulla")
                    }
                }
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun PortfolioScreenPreview() {
    MyApplicationTheme {
        PortfolioScreenContent(
            portfolioState = PortfolioUiState(
                cashBalance = 10000.0,
                holdings = listOf(
                    CryptoHoldingEntity("BTC", "Bitcoin", 0.5, 90000.0),
                    CryptoHoldingEntity("ETH", "Ethereum", 2.0, 3000.0)
                ),
                totalPortfolioValue = 65000.0,
                transactions = emptyList(),
                totalDeposited = 50000.0
            ),
            marketState = MarketUiState(
                isLoading = false,
                coins = listOf(
                    CryptoCoin("bitcoin", "BTC", "Bitcoin", "bitcoin", 1, 95000.0, 5.0, ""),
                    CryptoCoin("ethereum", "ETH", "Ethereum", "ethereum", 2, 3500.0, 2.0, "")
                )
            ),
            selectedCurrency = CurrencySetting.EUR,
            onCurrencySelected = {},
            onSimulateDeposit = {},
            onSetExactBalance = {},
            onPerformConversion = { _, _, _, _, _ -> },
            onExportClick = {},
            onNavigateToCoin = {}
        )
    }
}

@Composable
fun MyAssetsTab(
    holdings: List<CryptoHoldingEntity>,
    marketCoins: List<com.example.data.model.CryptoCoin>,
    currencySetting: CurrencySetting,
    onNavigateToCoin: (String) -> Unit
) {
    if (holdings.isEmpty()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = Icons.Default.ShowChart,
                contentDescription = null,
                modifier = Modifier.size(64.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Portafoglio Vuoto",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Fai acquisti nella sezione Mercato per comporre il tuo primo investimento simulato!",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }
    } else {
        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            items(holdings, key = { it.symbol }) { holding ->
                val coinModel = marketCoins.find { it.symbol.equals(holding.symbol, ignoreCase = true) }
                val currentPrice = coinModel?.priceUsd ?: holding.averagePurchasePrice
                val totalValue = holding.quantity * currentPrice
                val purchaseValue = holding.quantity * holding.averagePurchasePrice
                val returnVal = totalValue - purchaseValue
                val returnPercent = if (purchaseValue > 0) (returnVal / purchaseValue) * 100.0 else 0.0

                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            coinModel?.let { onNavigateToCoin(it.id) }
                        }
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = holding.name,
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "${FormatUtils.formatCryptoQuantity(holding.quantity)} ${holding.symbol}",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            Column(horizontalAlignment = Alignment.End) {
                                Text(
                                    text = currencySetting.format(totalValue),
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = FontWeight.Bold
                                )

                                val isProfit = returnVal >= 0
                                Text(
                                    text = "${if (isProfit) "+" else ""}${String.format(Locale.getDefault(), "%.2f", returnPercent)}%",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = if (isProfit) GreenCrypto else RedCrypto,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Column(
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                text = "Prezzo d'acquisto medio: ${currencySetting.format(holding.averagePurchasePrice)}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            Text(
                                text = "Prezzo Attuale: ${currencySetting.format(currentPrice)}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun TransactionsTab(
    transactions: List<TransactionEntity>,
    currencySetting: CurrencySetting
) {
    if (transactions.isEmpty()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = Icons.Default.List,
                contentDescription = null,
                modifier = Modifier.size(64.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Nessuna Operazione",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Qui apparirà la cronologia dei tuoi acquisti e vendite completati.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }
    } else {
        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            items(transactions, key = { it.id }) { tx ->
                val dateStr = DateFormat.format("dd MMM yyyy, HH:mm", Date(tx.timestamp)).toString()
                val isBuy = tx.type.equals("BUY", ignoreCase = true)
                val badgeColor = if (isBuy) GreenCrypto else RedCrypto

                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .padding(16.dp)
                            .fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Card(
                                    colors = CardDefaults.cardColors(containerColor = badgeColor.copy(alpha = 0.15f)),
                                    shape = RoundedCornerShape(4.dp)
                                ) {
                                    Text(
                                        text = if (isBuy) "COMPRA" else "VENDI",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = badgeColor,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "${tx.coinName} (${tx.symbol})",
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = dateStr,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = "${FormatUtils.formatCryptoQuantity(tx.quantity)} ${tx.symbol}",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Prezzo: ${currencySetting.format(tx.pricePerUnit)}",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "Valore: ${currencySetting.format(tx.totalValue)}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }
}

private fun buildCsvContent(
    portfolioState: com.example.ui.viewmodel.PortfolioUiState,
    marketCoins: List<com.example.data.model.CryptoCoin>,
    currencySetting: CurrencySetting
): String {
    val csvBuilder = java.lang.StringBuilder()
    
    // Header section
    csvBuilder.append("RIEPILOGO PORTAFOGLIO CRIPTO\n")
    csvBuilder.append("Valuta di riferimento;${currencySetting.name} (${currencySetting.code})\n")
    csvBuilder.append("Bilancio Totale;${currencySetting.format(portfolioState.totalPortfolioValue).replace("\"", "\"\"")}\n")
    csvBuilder.append("Fondi Contanti;${currencySetting.format(portfolioState.cashBalance).replace("\"", "\"\"")}\n")
    
    val cryptoVal = portfolioState.totalPortfolioValue - portfolioState.cashBalance
    csvBuilder.append("Valore Criptovalute;${currencySetting.format(cryptoVal).replace("\"", "\"\"")}\n\n")
    
    // Assets header
    csvBuilder.append("DETTAGLIO ATTIVITÀ (ASSETS)\n")
    csvBuilder.append("Nome;Simbolo;Quantità Posseduta;Prezzo d'acquisto medio;Prezzo Attuale;Valore Totale;Profitto/Perdita (%)\n")
    
    portfolioState.holdings.forEach { holding ->
        val coinModel = marketCoins.find { it.symbol.equals(holding.symbol, ignoreCase = true) }
        val currentPrice = coinModel?.priceUsd ?: holding.averagePurchasePrice
        val totalValue = holding.quantity * currentPrice
        val purchaseValue = holding.quantity * holding.averagePurchasePrice
        val returnVal = totalValue - purchaseValue
        val returnPercent = if (purchaseValue > 0) (returnVal / purchaseValue) * 100.0 else 0.0
        
        val nameEscaped = holding.name.replace("\"", "\"\"")
        val qtyStr = FormatUtils.formatCryptoQuantity(holding.quantity)
        val avgPriceStr = currencySetting.format(holding.averagePurchasePrice).replace("\"", "\"\"")
        val currentPriceStr = currencySetting.format(currentPrice).replace("\"", "\"\"")
        val totalValStr = currencySetting.format(totalValue).replace("\"", "\"\"")
        val returnPercentStr = "${if (returnVal >= 0) "+" else ""}${String.format(Locale.getDefault(), "%.2f", returnPercent)}%"
        
        csvBuilder.append("\"$nameEscaped\";${holding.symbol};$qtyStr;\"$avgPriceStr\";\"$currentPriceStr\";\"$totalValStr\";$returnPercentStr\n")
    }
    
    return csvBuilder.toString()
}

@Composable
fun ConvertTab(
    portfolioState: com.example.ui.viewmodel.PortfolioUiState,
    marketCoins: List<com.example.data.model.CryptoCoin>,
    selectedCurrency: CurrencySetting,
    onPerformConversion: (String, String, String, List<com.example.data.model.CryptoCoin>, (com.example.ui.viewmodel.TradeResult) -> Unit) -> Unit
) {
    val context = LocalContext.current
    var sourceAsset by remember { mutableStateOf("CASH") } // "CASH" or coin.symbol
    var targetAsset by remember { mutableStateOf("") } // "CASH" or coin.symbol
    var amountInput by remember { mutableStateOf("") }
    var resultMessage by remember { mutableStateOf<String?>(null) }
    var isErrorResult by remember { mutableStateOf(false) }

    // Dropdown expansion states
    var sourceExpanded by remember { mutableStateOf(false) }
    var targetExpanded by remember { mutableStateOf(false) }

    // Automatically select a target asset if empty or equal to sourceAsset
    val availableTargetCoins = marketCoins.filter { it.symbol != sourceAsset }
    if (targetAsset.isEmpty() || targetAsset == sourceAsset) {
        targetAsset = if (sourceAsset == "CASH") {
            availableTargetCoins.firstOrNull()?.symbol ?: ""
        } else {
            "CASH"
        }
    }

    // Source asset label and current owned amount
    val sourceHolding = portfolioState.holdings.find { it.symbol.equals(sourceAsset, ignoreCase = true) }
    val maxAvailable = if (sourceAsset == "CASH") {
        portfolioState.cashBalance * selectedCurrency.usdToCurrencyRate
    } else {
        sourceHolding?.quantity ?: 0.0
    }

    val sourceLabel = if (sourceAsset == "CASH") {
        "Contanti (${selectedCurrency.code})"
    } else {
        val coin = marketCoins.find { it.symbol.equals(sourceAsset, ignoreCase = true) }
        "${coin?.name ?: sourceAsset} ($sourceAsset)"
    }

    val targetLabel = if (targetAsset == "CASH") {
        "Contanti (${selectedCurrency.code})"
    } else {
        val coin = marketCoins.find { it.symbol.equals(targetAsset, ignoreCase = true) }
        "${coin?.name ?: targetAsset} ($targetAsset)"
    }

    // Calculate preview conversion output
    val parsedAmount = amountInput.replace(',', '.').toDoubleOrNull() ?: 0.0
    val estimatedOutput: Double = if (parsedAmount > 0.0) {
        if (sourceAsset == "CASH") {
            // Fiat to Crypto
            val targetCoin = marketCoins.find { it.symbol.equals(targetAsset, ignoreCase = true) }
            if (targetCoin != null && targetCoin.priceUsd > 0) {
                val usdAmount = parsedAmount / selectedCurrency.usdToCurrencyRate
                usdAmount / targetCoin.priceUsd
            } else 0.0
        } else if (targetAsset == "CASH") {
            // Crypto to Fiat
            val sourceCoin = marketCoins.find { it.symbol.equals(sourceAsset, ignoreCase = true) }
            val sourcePriceUsd = sourceCoin?.priceUsd ?: sourceHolding?.averagePurchasePrice ?: 0.0
            val usdVal = parsedAmount * sourcePriceUsd
            usdVal * selectedCurrency.usdToCurrencyRate
        } else {
            // Crypto to Crypto
            val sourceCoin = marketCoins.find { it.symbol.equals(sourceAsset, ignoreCase = true) }
            val targetCoin = marketCoins.find { it.symbol.equals(targetAsset, ignoreCase = true) }
            val sourcePriceUsd = sourceCoin?.priceUsd ?: sourceHolding?.averagePurchasePrice ?: 0.0
            if (sourcePriceUsd > 0 && targetCoin != null && targetCoin.priceUsd > 0) {
                (parsedAmount * sourcePriceUsd) / targetCoin.priceUsd
            } else 0.0
        }
    } else {
        0.0
    }

    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(bottom = 24.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        text = "Seleziona Valute",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )

                    // --- SOURCE ASSET SELECTOR ---
                    Column {
                        Text(
                            text = "Da (Sorgente):",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        
                        Box(modifier = Modifier.fillMaxWidth()) {
                            OutlinedButton(
                                onClick = { sourceExpanded = true },
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.fillMaxWidth().testTag("convert_source_select_button"),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = sourceLabel,
                                        style = MaterialTheme.typography.bodyLarge,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Text(
                                        text = "▼",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }

                            DropdownMenu(
                                expanded = sourceExpanded,
                                onDismissRequest = { sourceExpanded = false },
                                modifier = Modifier.fillMaxWidth(0.9f)
                            ) {
                                // Option 1: Cash (Always available, but user needs cash balance)
                                DropdownMenuItem(
                                    text = { 
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Text("Contanti (${selectedCurrency.code})")
                                            Text(selectedCurrency.format(portfolioState.cashBalance))
                                        }
                                    },
                                    onClick = {
                                        sourceAsset = "CASH"
                                        sourceExpanded = false
                                    },
                                    modifier = Modifier.testTag("source_option_cash")
                                )

                                // Option 2: Owned Cryptos from Portfolio
                                portfolioState.holdings.forEach { holding ->
                                    DropdownMenuItem(
                                        text = {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween
                                            ) {
                                                Text("${holding.name} (${holding.symbol})")
                                                Text("${FormatUtils.formatCryptoQuantity(holding.quantity)} ${holding.symbol}")
                                            }
                                        },
                                        onClick = {
                                            sourceAsset = holding.symbol
                                            sourceExpanded = false
                                        },
                                        modifier = Modifier.testTag("source_option_${holding.symbol}")
                                    )
                                }
                            }
                        }
                    }

                    // --- TARGET ASSET SELECTOR ---
                    Column {
                        Text(
                            text = "A (Destinazione):",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(4.dp))

                        Box(modifier = Modifier.fillMaxWidth()) {
                            OutlinedButton(
                                onClick = { targetExpanded = true },
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.fillMaxWidth().testTag("convert_target_select_button"),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = targetLabel,
                                        style = MaterialTheme.typography.bodyLarge,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Text(
                                        text = "▼",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }

                            DropdownMenu(
                                expanded = targetExpanded,
                                onDismissRequest = { targetExpanded = false },
                                modifier = Modifier.fillMaxWidth(0.9f)
                            ) {
                                // Option 1: Cash (only available if source is NOT Cash)
                                if (sourceAsset != "CASH") {
                                    DropdownMenuItem(
                                        text = { Text("Contanti (${selectedCurrency.code})") },
                                        onClick = {
                                            targetAsset = "CASH"
                                            targetExpanded = false
                                        },
                                        modifier = Modifier.testTag("target_option_cash")
                                    )
                                }

                                // Option 2: All Cryptos from Market
                                availableTargetCoins.forEach { coin ->
                                    DropdownMenuItem(
                                        text = { Text("${coin.name} (${coin.symbol})") },
                                        onClick = {
                                            targetAsset = coin.symbol
                                            targetExpanded = false
                                        },
                                        modifier = Modifier.testTag("target_option_${coin.symbol}")
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        text = "Inserisci Importo",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )

                    // Display max available balance helper
                    val balanceHelperText = if (sourceAsset == "CASH") {
                        "Saldo disponibile: ${selectedCurrency.format(portfolioState.cashBalance)}"
                    } else {
                        "Disponibili: ${FormatUtils.formatCryptoQuantity(maxAvailable)} $sourceAsset"
                    }

                    Text(
                        text = balanceHelperText,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = amountInput,
                            onValueChange = {
                                amountInput = it
                                resultMessage = null
                            },
                            placeholder = { Text("0.00") },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Decimal,
                                imeAction = ImeAction.Done
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("convert_amount_field")
                        )

                        Button(
                            onClick = {
                                amountInput = if (sourceAsset == "CASH") {
                                    String.format(Locale.getDefault(), "%.2f", maxAvailable).replace(',', '.')
                                } else {
                                    FormatUtils.formatCryptoQuantity(maxAvailable).replace(',', '.')
                                }
                                resultMessage = null
                            },
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.testTag("convert_max_button")
                        ) {
                            Text("Max")
                        }
                    }

                    // --- PREVIEW OUTPUT SECTION ---
                    if (parsedAmount > 0.0) {
                        val previewText = if (targetAsset == "CASH") {
                            "Riceverai circa: ${selectedCurrency.format(estimatedOutput)}"
                        } else {
                            "Riceverai circa: ${FormatUtils.formatCryptoQuantity(estimatedOutput)} $targetAsset"
                        }

                        Card(
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = previewText,
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.padding(12.dp)
                            )
                        }
                    }

                    // --- ERROR / SUCCESS FEEDBACK ---
                    if (resultMessage != null) {
                        val textColor = if (isErrorResult) MaterialTheme.colorScheme.error else GreenCrypto
                        Text(
                            text = resultMessage ?: "",
                            color = textColor,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(vertical = 4.dp).testTag("convert_feedback_text")
                        )
                    }

                    // --- SUBMIT BUTTON ---
                    Button(
                        onClick = {
                            if (amountInput.trim().isEmpty() || parsedAmount <= 0.0) {
                                isErrorResult = true
                                resultMessage = "Inserisci un importo maggiore di zero."
                                return@Button
                            }

                            if (parsedAmount > maxAvailable) {
                                isErrorResult = true
                                resultMessage = "Fondi insufficienti."
                                return@Button
                            }

                            onPerformConversion(
                                sourceAsset,
                                targetAsset,
                                amountInput,
                                marketCoins
                            ) { res ->
                                when (res) {
                                    is com.example.ui.viewmodel.TradeResult.Success -> {
                                        isErrorResult = false
                                        resultMessage = res.message
                                        amountInput = ""
                                        Toast.makeText(context, "Conversione completata!", Toast.LENGTH_SHORT).show()
                                    }
                                    is com.example.ui.viewmodel.TradeResult.Error -> {
                                        isErrorResult = true
                                        resultMessage = res.message
                                    }
                                }
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("convert_submit_button"),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(
                            text = "Converti Ora",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

