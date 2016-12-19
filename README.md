```java
Qoncrete qoncrete = new Qoncrete.Builder().sourceID("sourceID").apiToken("apiToken")
                .secureTransport(false)
                .cacheDNS(true)
                .autoBatch(true)
                .batchSize(1000)
                .autoSendAfter(2)
                .build(this);
qoncrete.send(json);
```

