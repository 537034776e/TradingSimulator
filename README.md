# Simulatore Crypto - Android Trading app

Un'applicazione Android nativa e moderna per la simulazione del trading di criptovalute in tempo reale, sviluppata in **Kotlin** con **Jetpack Compose** seguendo l'architettura **MVVM (Model-View-ViewModel)**.

---

## 🚀 Requisiti di Sistema
* **Android Studio:** Jellyfish / Ladybug (o versioni successive)
* **SDK Minimo (minSdk):** 24 (Android 7.0)
* **SDK Target (targetSdk):** 36 (Android 15)
* **API Key Necessarie:** Nessuna (l'app consuma l'API pubblica di CoinLore che non richiede alcuna autenticazione o chiave segreta).

---

## 🛠️ Architettura e Tecniche Implementate
Il progetto è stato sviluppato implementando rigorosamente le linee guida del corso:
* **UI e Layout:** Realizzata al 100% in **Jetpack Compose** utilizzando i componenti Material Design 3 (M3) (es. `Scaffold`, `NavigationBar`, `TabRow`, `OutlinedTextField`) e assecondando logiche di State Hoisting ("events up, state down").
* **Data Layer (Offline-First e Caching):**
  * **Persistenza Locale (Room Database):** Utilizzo di `@Database`, tre diverse `@Entity` separate (`UserProfileEntity`, `CryptoHoldingEntity`, `TransactionEntity`), coordinate da un `@Dao` con interrogazioni reattive esposte tramite `Flow<T>` e scritture asincrone mediate da `suspend` coroutines.
  * **Transazione Integrata:** Gestione atomica di compravendita mediante l'annotazione `@Transaction` di Room in `CryptoDao` per aggiornare saldo, holding e archivio storico in un unico blocco transazionale sicuro.
  * **Chiamate di Rete (Retrofit):** Integrazione type-safe con Retrofit + Moshi Converter per invocare le API CoinLore in modalità ad alte prestazioni (suspend).
  * **Caricamento Immagini (Coil):** Caricamento asincrono e performante dei loghi delle criptovalute da URL remoto mediante `AsyncImage`.
* **Navigazione:** Gestita in modo nativo tramite **Navigation Compose** con transizioni reattive, passaggio di parametri stringa (`coinId`) e conservazione dinamica del backstack.
* **Integrazioni con il Sistema (Intent):**
  * Utilizzo di **Intent impliciti (`ACTION_SEND`)** per consentire all'utente di condividere via social o messaggistica le quotazioni attuali di mercato delle valute o i traguardi del proprio saldo virtuale direttamente dalla visualizzazione Portafoglio.
* **Componenti Grafiche Avanzate:** Rappresentazione della variazione dei prezzi nelle 24h tramite una curva vettoriale fluida disegnata tramite l'API Compose `Canvas` (`drawPath` con gradienti Brush dinamici).

---

## 💡 Funzionalità Implementate
1. **Mercato in tempo reale:** Listino delle prime 50 monete con ordinamento, variazioni di prezzo nelle 24 ore e indicatore di possedimento se l'utente possiede già quote dell'asset.
2. **Ricerca dinamica:** Filtro istantaneo delle valute sia per nome che per simbolo (es. "Bitcoin" o "BTC").
3. **Gestione del bilancio iniziale:** Ogni utente inizia con un saldo virtuale preimpostato a **$10.000**.
4. **Acquisto e Vendita con Validazione lato client:**
  * Verifica del saldo contante disponibile prima di un acquisto.
  * Verifica delle quote detenute in portafoglio prima di una vendita.
  * Validazione dei caratteri immessi (quantità positive non vuote).
5. **Dettaglio interattivo:** Schermata secondaria con andamento grafico dei prezzi vettoriale, riepilogo ROI (Rendimento sull'investimento), profitto di carico medio, e controllo transazionale.
6. **Portafoglio e Storico Transazioni:** Un resoconto in tempo reale del valore complessivo degli asset, diviso in sottovalutazione per la quota contante e quella crypto, affiancata da un registro completo delle transazioni (BUY/SELL cronologici).
7. **Graceful Offline Degradation:** Se la connessione internet è assente, l'app notifica lo stato tramite apposita barra di sistema e consente di continuare a visualizzare il portafoglio e gli ultimi tassi di cambio memorizzati in locale senza andare in crash.

---

## 🤖 Note sull'Uso dell'Intelligenza Artificiale (AI Studio Build)
* **Strumenti utilizzati:** Google AI Studio Coding Agent (alimentato da modelli Gemini 3.5).
* **Parti generate dall'AI:**
  * Struttura iniziale del database Room, definizioni degli adapter Moshi per il mapping JSON delle API CoinLore e funzioni matematiche per il ricalcolo del prezzo medio ponderato durante gli acquisti consecutivi di token.
  * Progettazione grafica della curva Canvas in `DetailScreen.kt` per calcolare coordinate vettoriali basate sulla percentuale di incremento o deprezzamento della moneta.
* **Parti modificate ed estese manualmente:**
  * Integrazione dei controlli di validazione degli input di testo per prevenire eccezioni d'immissione vuota o formati numerici errati locali.
  * Personalizzazione del tema scuro premium (`DarkColorScheme`) con tonalità e contrasti accessibili ad alta densità ideali per cruscotti finanziari.
  * Adattamento degli Intent impliciti `ACTION_SEND` e delle logiche di insets per garantire un design edge-to-edge sicuro dall'intersezione con notch e barre di sistema.

---

## 📦 Istruzioni per Compiere ed Eseguire l'app
1. Apri **Android Studio**.
2. Scegli **File > Open** e seleziona la cartella radice del progetto per importarlo.
3. Attendi il completamento della sincronizzazione di Gradle (Gradle Sync).
4. Collega un dispositivo fisico Android con debug USB abilitato oppure avvia un emulatore integrato (AVD).
5. Premi il tasto **Run (Play verde)** o premi la scorciatoia `Shift + F10` per compilare l'APK ed installarlo sul target di test.
