# 🎮 TriSense

**TriSense** è un'applicazione mobile Android nativa che propone tre mini-giochi interattivi, ciascuno progettato per sfruttare un diverso sensore hardware del dispositivo (microfono, touchscreen, accelerometro). 

Il progetto esplora l'interazione uomo-macchina attraverso canali sensoriali non convenzionali, il tutto avvolto in un'accattivante veste grafica a tema **Neo-Retro Arcade**. Sviluppato per il corso di Mobile Programming presso l'Università degli Studi di Salerno (UNISA).

---

## 🕹️ I Giochi

L'app si compone di tre esperienze distinte, renderizzate tramite un game loop customizzato su `SurfaceView` a ~60 FPS:

* 🎤 **Decibel Jump (Microfono):** Un gioco a scorrimento laterale dove la tua voce è il controller. Emetti un suono sufficientemente forte nel microfono per far saltare il personaggio ed evitare gli ostacoli.
* 👆 **Flash Reflex (Touchscreen):** Testa la tua prontezza visuo-motoria toccando i cerchi colorati che appaiono e scompaiono a intervalli progressivamente più brevi. Un solo errore e la partita finisce.
* 📱 **Gyro Maze (Accelerometro):** Inclina fisicamente il tuo smartphone per guidare una pallina all'interno di uno spazio delimitato, raccogliendo monete ed evitando gli ostacoli generati dinamicamente.

---

## ✨ Funzionalità Principali

* **Autenticazione Sicura:** Sistema di login e registrazione gestito tramite Firebase Authentication.
* **Classifiche Globali (Hall of Fame):** Competizione in tempo reale! I 5 migliori punteggi di tutti i giocatori per ciascun gioco sono salvati e recuperati tramite Firebase Cloud Firestore.
* **Statistiche Locali:** L'app calcola e mantiene la tua media punteggi personale utilizzando le `SharedPreferences` (funzionante anche offline).
* **Motore Fisico Nativo:** Collisioni calcolate dinamicamente tramite distanza euclidea e intersezioni rettangolari, con fisica applicata (gravità, attrito, smorzamento) senza l'uso di engine esterni.

---

## 🛠️ Tecnologie e Architettura

L'applicazione segue un'architettura a strati (MVC adattato) e utilizza un pattern Client-Server per la comunicazione con il database NoSQL.

* **Linguaggio:** Java 11
* **Piattaforma:** Android SDK (minSdk 24, targetSdk 36)
* **Backend as a Service (BaaS):** Firebase (Auth + Firestore)
* **UI/UX:** Material Design 3, Layout XML, approccio EdgeToEdge
