# Memory Setup

In order to setup the memory usage for the deployed play applications add the following to the .yaml file used for the deploy.

```
spec:
  template:
    spec:
        env:
        - name: JAVA_OPTS
          value: "-server -XX:+UseG1GC -XX:MaxGCPauseMillis=100 -XX:+PerfDisableSharedMem -XX:+ParallelRefProcEnabled -Xmx2g -Xms2g -XX:MaxPermSize=1024m"
```
