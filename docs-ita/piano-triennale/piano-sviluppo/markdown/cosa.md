# Cos'è il DAF

Il [Data & Analytics
Framework](https://pianotriennale-ict.readthedocs.io/it/latest/doc/09_data-analytics-framework.html) (DAF) è una delle attività atte a valorizzare il patrimonio informativo pubblico nazionale approvata dal Governo italiano nell’ambito del [Piano
Triennale per l’Informatica nella PA
2017-2019](https://pianotriennale-ict.italia.it). L’obiettivo
principale del DAF è di abbattere le barriere esistenti
nell’interscambio dei dati pubblici tra PA e promuoverne l’utilizzo a
supporto del decision making pubblico, ottimizzare i processi di analisi
dati e generazione di sapere, standardizzare e promuovere la diffusione
degli open data, promuovere e supportare iniziative di ricerca
scentifica favorendo la collaborazione con Università ed enti di
ricerca.

Il DAF si compone di:

-  Una **Piattaforma Big Data** costituita da un data lake, un insieme
   di data engine e strumenti per la comunicazione dei dati.

   - Nel *data lake* vengono memorizzati, nel rispetto delle normative in materia di protezione dei dati personali, dati di potenziale interesse quali, ad esempio: 
     - le basi di dati che le PA generano per svolgere il proprio mandato istituzionale;
     - i dati generati dai sistemi informatici delle Pubbliche Amministrazioni come log e dati di utilizzo che non rientrano nella definizione precedente;
     - i dati autorizzati provenienti dal web e dai social network di potenziale interesse della Pubblica Amministrazione;

   - *big data engine* utile ad armonizzare ed elaborare, sia in modalità batch che real-time, i dati grezzi memorizzati nel data lake e a implementare modelli di machine learning;

   - *strumenti per l'interscambio dei dati*, utili a favorire la fruizione dei dati elaborati da parte dei soggetti interessati, anche attraverso API che espongono dati e funzionalità ad applicazioni terze;

   - *strumenti di analisi e visualizzazione dei dati* offerti in modalità self-service agli utilizzatori del DAF.

-  Un **Dataportal**, che rappresenta l'interfaccia utente per l'utilizzo delle funzionalità implementate nel DAF. In particolare, il dataportal si compone di:

   - un catalogo dei dataset basato su CKAN, che gestisce i metadati relativi sia ai dati contenuti nel DAF che agli open data harvestati dai siti delle PA;

   - interfacce utente per accedere ai tool di analisi e data visualization menzionati sopra;

   - un modulo riservato alle PA per gestire il processo di *ingestion* e gestione dei dati e metadati nel DAF;

   - un modulo per *data stories*, attraverso il quale gli utenti possono pubblicare le proprie analisi e collaborare con altri utenti.

-  Da un **team di esperti di dati**, composto da data scientist, data engineer e big data architect che provvedono al disegno e all’evoluzione concettuale della piattaforma big data, alla costruzione di modelli di interconnessione delle diverse sorgenti dati, all’analisi dei dati, allo sviluppo di modelli di machine learning, al coordinamento dello sviluppo di data application e all’incentivazione della ricerca scientifica su tematiche di interesse per la PA.

Si rimanda al [capitolo 9 del Piano Triennale](https://pianotriennale-ict.readthedocs.io/it/latest/doc/09_data-analytics-framework.html) per maggiori informazioni.
