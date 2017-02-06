```java
Qoncrete qoncrete = new Qoncrete.Builder().sourceID("sourceID").apiToken("apiToken")
                .secureTransport(false)
                .cacheDNS(true)
                .autoBatch(true)
                .batchSize(1000)
                .autoSendAfter(2)
                .build(this);
// Qoncrete qoncrete = new Qoncrete(this, "sourceID", "apiToken");
qoncrete.send(json);
```

##### API

* send(String string)

* setCallback(Callback callback)

* destroy()

