///usr/bin/env jbang "$0" "$@" ; exit $?

//DEPS com.github.lalyos:jfiglet:0.0.8
//DEPS com.amadeus:amadeus-java:5.9.0
//DEPS org.projectlombok:lombok:1.18.22

package com.jab.tools;

import com.amadeus.Amadeus;
import com.amadeus.Params;
import com.amadeus.exceptions.ResponseException;
import com.amadeus.resources.DiseaseAreaReport;
import com.github.lalyos.jfiglet.FigletFont;

import java.io.IOException;
import java.util.Arrays;

public class AmadeusCOVIDRestrictions {

    public static void main(String... args) throws ResponseException, IOException {

        System.out.println(FigletFont.convertOneLine("Amadeus for Developers"));

        var AMADEUS_CLIENT_ID = args[0];
        var AMADEUS_CLIENT_SECRET = args[1];

        var COUNTRY_CODE = args[2];

        Amadeus amadeus = Amadeus
                .builder(AMADEUS_CLIENT_ID, AMADEUS_CLIENT_SECRET)
                .build();

        DiseaseAreaReport diseaseAreaReport = amadeus.dutyOfCare.diseases.
                covid19AreaReport.get(Params.with("countryCode",COUNTRY_CODE));//.and("cityCode", "MAD")

        System.out.println("https://test.api.amadeus.com/v1/duty-of-care/diseases/covid19-area-report?countryCode=" + "FR");
        System.out.println();
        System.out.println(diseaseAreaReport.getAreaAccessRestriction().getDeclarationDocuments().getText());
        System.out.println();
        Arrays.stream(diseaseAreaReport.getAreaRestrictions()).map(DiseaseAreaReport.AreaRestriction::getText).forEach(System.out::println);
        System.out.println();
        System.out.println(diseaseAreaReport.getAreaAccessRestriction().getDiseaseTesting().getText());
    }

}
