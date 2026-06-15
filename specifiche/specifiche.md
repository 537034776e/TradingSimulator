# Documento di Specifica del Progetto: Simulatore Crypto

## 1. Obiettivo dell'applicazione
**Simulatore Crypto** è un'applicazione Android nativa progettata come simulatore educativo di trading di criptovalute offline-first. Lo scopo è far comprendere agli utenti le fluttuazioni del mercato delle valute digitali e consentire loro di testare strategie di acquisto e vendita in tempo reale utilizzando un saldo iniziale virtuale di $10.000.  
L'applicazione è rivolta a studenti, appassionati e chiunque voglia approcciarsi al mondo del trading di asset digitali senza rischi finanziari.

---

## 2. Cosa può fare l'utente
* **Visualizzare il listino prezzi in tempo reale:** Ottenere informazioni aggiornate su 50 criptovalute popolari (grazie all'integrazione con le API pubbliche di CoinLore).
* **Cercare specifiche monete:** Filtrare istantaneamente la lista per nome o simbolo (es. BTC, ETH) attraverso una ricerca reattiva locale.
* **Analizzare un singolo asset:** Visualizzare dettagli sui prezzi, classifiche, variazioni percentuali nelle 24 ore e una rappresentazione grafica personalizzata dell'andamento dei prezzi (disegnato con un grafico vettoriale liscio su Canvas).
* **Negoziare (Simulazione di Compravendita):** Eseguire simulazioni di acquisto o vendita di criptovalute inserendo la quantità desiderata. L'applicazione valida rigorosamente in tempo reale che l'utente abbia contanti a sufficienza (per l'acquisto) o quantità di monete sufficienti (per la vendita).
* **Monitorare il Portafoglio:** Esaminare il valore totale corrente degli investimenti (pari alla somma del saldo contante residuo e del valore di mercato odierno dei token posseduti), il rendimento d'investimento dettagliato (ROI, profitto o perdita) e la cronologia completa delle transazioni eseguite.
* **Condividere quotazioni e profitti:** Interagire con altre applicazioni di sistema tramite Intent impliciti per condividere i prezzi delle monete o i traguardi del proprio bilancio di simulazione.

---

## 3. Struttura dell'app
L'applicazione è strutturata su 3 viste principali collegate tramite sistema di navigazione Bottom Navigation (Navigation Compose):
1. **Schermata Mercato (Market):**
   * Elenco a scorrimento delle criptovalute disponibili con Classifica (Rank), Simbolo, Nome, prezzo in USD e variazione % nelle 24h.
   * Barra di ricerca dinamica in alto.
   * Pulsante di aggiornamento manuale (manual refresh) per ricaricare istantaneamente le quotazioni.
2. **Schermata Dettaglio Criptovaluta (Detail):**
   * Mostra logo del token, prezzo, variazione, indicatore grafico su Canvas dell'andamento e riepilogo della quota posseduta dell'asset selezionato (con prezzo medio di carico e ROI).
   * Modulo di immissione testo per quantità e azioni interattive "Compra" e "Vendi".
   * Pulsante di Condivisione (Share Intent).
3. **Schermata Portafoglio (Portfolio):**
   * Bilancio sintetico con valore di portafoglio virtuale stimato in tempo reale.
   * Sezione a schede (TabRow):
     * *I Miei Asset:* Criptovalute attualmente possedute con prezzo medio di carico, ROI % e valore attuale di mercato.
     * *Cronologia:* Registro storico delle transazioni eseguite (tipo Buy/Sell, quantità scambiata, timestamp preciso, valore scambiato).

---

## 4. Mockup minimale
```text
+--------------------------------------+
|  Simulatore Crypto        [Refresh]  |
|                                      |
|  [ Cerca crypto...               X ] |
|                                      |
|  #1 Bitcoin (BTC)         $64,120.00 |
|     Variazione 24h:          +1.24%  |
|  #2 Ethereum (ETH)         $3,450.50 |
|     Variazione 24h:          -0.45%  |
|  #3 Binance Coin (BNB)       $580.20 |
|                                      |
+--------------------------------------+
|  [MERCATO (A)]       [PORTAFOGLIO]   |
+--------------------------------------+
```

---

## 5. Scenari di test
| Caso di Test | Azione Utente | Risultato Atteso |
| :--- | :--- | :--- |
| **T1: Input non valido** | Inserimento di testo vuoto, lettere o quantità negativa nel modulo di trading e invio. | Mostra messaggio di validazione localizzato in rosso sotto il campo dell'input: *"Inserisci una quantità superiore a zero."* |
| **T2: Fondi insufficienti** | Tentativo di acquisto di 2 BTC con un saldo iniziale di soli $10.000. | Blocca l'operazione di acquisto e visualizza un errore rosso esplicativo indicando il costo totale stimato e il fondo disponibile. |
| **T3: Vendita non posseduta** | Tentativo di vendita di un asset non posseduto o in quantità superiore a quella detenuta. | Impedisce la transazione evidenziando l'errore: *"Non possiedi abbastanza monete."* |
| **T4: Funzionamento Offline** | Disattivazione della connessione di rete e avvio/refresh dell'app. | L'app degrada offline in modo reattivo: carica dal database locale Room il portafoglio e le monete cached visualizzando l'avviso *"Sei Offline. Utilizzando l'ultimo listino memorizzato."* per evitare arresti anomali. |
| **T5: Rotazione dello schermo** | Cambio di orientamento del dispositivo durante l'immissione o la visualizzazione del mercato. | Lo stato della UI (es. query inserite, coin visualizzati, e navigazione corrente) sopravvive intatto grazie ai Lifecycle Flow del ViewModel. |
