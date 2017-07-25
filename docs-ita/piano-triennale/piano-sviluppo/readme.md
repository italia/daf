# Piano di sviluppo DAF

Allo stato attuale la procedura più conveniente per apportare modifiche al Piano di sviluppo è la seguente:

1. Apportare le modifiche ai sorgenti (e/o aggiungerne di nuovi) in formato *rst* presenti nella cartella [rst](rst). Qualora si intenda aggiungere un *caso d'uso* si può utilizzare il relativo [template](rst/casi-uso/_template-pagina-casi-applicativi.rst).

2. Al fine di testare le modifiche apportate, eseguire lo script `rst2html.sh` (per eseguire lo script è necessario installare [Sphinx](http://www.sphinx-doc.org/en/stable/)). L'output dello script è memorizzato nella cartella [docs](docs). A partire dal file [index.html](docs/index.html) è possibile navigare la versione compilata del sito e verificare la corretta visualizzazione dei contenuti.

3. Una volta verificata l'assenza di errori, copiare il contenuto della cartella *rst* sul proprio repository precedentemente creato effettuando una fork del repository ufficiale: `https://github.com/italia/daf-piano-di-sviluppo` ed effettuare commit e pull request.  

4. Assicurarsi che le modifiche siano riversate su read-the-docs all'indirizzo di pubblicazione.