# Piano di sviluppo DAF


Per la modifica e pubblicazione on-line del Piano di sviluppo seguire la seguente procedura:

1. Apportare le modifiche ai sorgenti in formato *md* presenti nella cartella [markdown](markdown).
2. Eseguire lo script [md2rst.sh](md2rst.sh).
3. Copiare il contenuto della cartella *rst* (generata dallo script di cui al passo 2) nella cartella di lavoro locale relativa al progetto gitHub [daf-piano-di-sviluppo](https://github.com/italia/daf-piano-di-sviluppo). Se si vuole aggiungere un nuovo caso d'uso si può utilizzare l'apposito [template](markdown/casi-uso/_template-pagina-casi-applicativi.md).
4. Seguire le istruzioni relative ai passi 2 e 3 della sezione [Come eseguire la build](https://github.com/italia/daf-piano-di-sviluppo/blob/master/README.md#come-eseguire-la-build).