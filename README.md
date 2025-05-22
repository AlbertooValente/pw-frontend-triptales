# TripTales – Diario di Gita con Intelligenza Artificiale

TripTales è un'app Android pensata per studenti in gita scolastica. Permette di documentare e condividere in modo collaborativo momenti salienti attraverso foto, commenti e geolocalizzazione, arricchiti con funzionalità smart offerte da ML Kit e Google API. Questo è il frontend del progetto.

## Obiettivo del progetto

L’obiettivo è realizzare un’applicazione mobile che:

* Favorisca la collaborazione tra studenti durante una gita
* Sfrutti intelligenza artificiale e geolocalizzazione per migliorare l’esperienza educativa
* Permetta la documentazione multimediale tramite foto, testi, mappe e badge gamificati

## Tecnologie usate nel frontend

L'app è sviluppata in **Kotlin** utilizzando **Jetpack Compose**, seguendo i principi dell’architettura moderna Android.

### Librerie e strumenti principali

* **Retrofit** – per la comunicazione con l’API backend
* **Google API**, tra cui:
  * Location Services per il rilevamento della posizione al momento dello scatto
  * Geocoder per la conversione di coordinate in indirizzi reali
* **ML Kit** per:
  * Image labeling
  * OCR (riconoscimento testo nelle immagini)
  * Identificazione della lingua
  * Traduzione automatica

## Funzionalità principali

* **Login e registrazione**
  
  ![image](https://github.com/user-attachments/assets/5153880a-a84e-4022-885b-d0d0b55a37ac)
  
  ![image](https://github.com/user-attachments/assets/8e15e90d-6f6d-4759-918c-61f2910b23f8)
  
* **Home** con visualizzazione dei trip a cui si è iscritti. 
  * Il pulsante `+` permette di creare un nuovo trip o aggiungersi a uno esistente
  * Cliccando sull’immagine profilo si accede al menu di modifica profilo o al logout
    
  ![image](https://github.com/user-attachments/assets/fa76966f-4f16-4b78-bc7e-b52c12c04113)

* **Menu del trip** con:
  * **Bacheca**: tutti i post pubblicati
  * **Mappa**: mostra la posizione associata ai post
  * **Classifiche**: post con più like, utenti più attivi
  * Il pulsante `+` consente la creazione di nuovi post
  * Doppio tap su un post per mettere/togliere like
  * Tap singolo per aprire i dettagli
  * Tre puntini in alto per modificare o eliminare il proprio post
    
  ![image](https://github.com/user-attachments/assets/2f8149dc-0f8b-4db1-b2a9-ecc87789eed7)

* **Dettagli del post**:
  * Titolo, descrizione e immagine in alta risoluzione
  * Rilevamento automatico del contenuto tramite image labeling
  * Posizione geografica e indirizzo
  * Testo riconosciuto con OCR e tradotto automaticamente, se necessario
  * Sezione commenti dedicata
     
  ![image](https://github.com/user-attachments/assets/45210e29-0eb2-4a1d-8925-9ed88b998c32)

## Installazione e Setup

### 1. Clona il repository

Apri il terminale e digita:

```bash
git clone https://github.com/AlbertooValente/pw-frontend-triptales.git
```

Oppure scarica il file .zip direttamente da GitHub e decomprimilo.

### 2. Apri il progetto in Android Studio

* Apri Android Studio
* Seleziona "Open an existing project"
* Naviga nella cartella del progetto appena scaricato
  
### 3. Configura l’indirizzo del backend (IP o dominio ngrok)

Nel file TripTalesAPI.kt troverai una variabile globale:

```kotlin
object Constants {
  const val BASE_URL = "https://light-active-shrimp.ngrok-free.app" // <-- MODIFICA QUI
}
```

Sostituisci il valore con l'indirizzo corretto del backend:
* In locale su Wi-Fi: usa l'IP del server, es. `http://192.168.1.123:8000`
* Con ngrok: usa il dominio `https` fornito da ngrok, es. `https://abcd1234.ngrok.io`
  
### 4. Esegui il progetto
Collega un dispositivo Android oppure usa un emulatore, quindi premi Run ▶️ da Android Studio.

## Struttura dei repository
* Frontend
* [Backend](https://github.com/Basso-Giovanni/pw-backend-triptales.git)

## Autori
SegFault Squad: Basso Giovanni e Valente Alberto
