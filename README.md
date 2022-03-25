# jbang-catalog

Register to get your AMADEUS_CLIENT_ID & AMADEUS_CLIENT_SECRET
https://developers.amadeus.com

```
mvn compile exec:java \
    -Dexec.mainClass="com.jab.tools.AmadeusCOVIDRestrictions" \
    -Dexec.args="AMADEUS_CLIENT_ID AMADEUS_CLIENT_SECRET COUNTRY_CODE"

jbang hello-world@jabrena
jbang amadeus-covid-restrictions@jabrena AMADEUS_CLIENT_ID AMADEUS_CLIENT_SECRET COUNTRY_CODE
```