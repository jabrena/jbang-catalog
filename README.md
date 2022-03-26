# jbang-catalog

## Motivation

Sometimes, I need to travel in Europe or cross the Atlantic ocean but 
after 2 years of COVID, before travelling I need to review the country restrictions 
defined for travellers.

Amadeus for developers provides an API for that and it offer information.

## How to use it?

Register in [Amadeus for Developers](https://developers.amadeus.com) to get your `AMADEUS_CLIENT_ID` & `AMADEUS_CLIENT_SECRET`

Export the values:

```
export AMADEUS_CLIENT_ID=YOUR_CLIENT_ID
export AMADEUS_CLIENT_SECRET=YOUR_CLIENT_SECRET
```

And run the following `jbang` script:

```
jbang amadeus-covid-restrictions@jabrena ES
```

Enjoy and travel safe

Juan Antonio
