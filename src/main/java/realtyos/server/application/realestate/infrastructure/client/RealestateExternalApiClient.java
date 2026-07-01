package realtyos.server.application.realestate.infrastructure.client;

import realtyos.server.application.realestate.infrastructure.client.dto.DealsApiResponse;
import realtyos.server.application.realestate.infrastructure.client.dto.ApartmentComplexApiResponse;
import realtyos.server.application.realestate.infrastructure.client.dto.ApartmentComplexBasisInfoApiResponse;
import realtyos.server.application.realestate.infrastructure.client.dto.AptPblancApiResponse;
import realtyos.server.application.realestate.infrastructure.client.dto.RentPblancApiResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;

@Slf4j
@Component
@RequiredArgsConstructor
public class RealestateExternalApiClient {

        private final RestClient restClient;

        @Value("${external.api.realestate.service-key}")
        private String serviceKey;

        @Value("${external.api.realestate.deals-base-url}")
        private String dealsBaseUrl;

        @Value("${external.api.realestate.pblanc-base-url}")
        private String pblancBaseUrl;

        @Value("${external.api.realestate.apt-list-base-url}")
        private String aptListBaseUrl;

        @Value("${external.api.realestate.apt-basis-info-base-url}")
        private String aptBasisInfoBaseUrl;

//        public DealsApiResponse fetchDeals(String lawdCd, String dealYmd, int pageNo, int numOfRows) {
//                RestClient restClient = restClientBuilder.build();
//
//                URI uri = UriComponentsBuilder.fromUriString(dealsBaseUrl + "/RTMSDataSvcAptTrade/getRTMSDataSvcAptTrade")
//                                .queryParam("serviceKey", serviceKey)
//                                .queryParam("LAWD_CD", lawdCd)
//                                .queryParam("DEAL_YMD", dealYmd)
//                                .queryParam("pageNo", pageNo)
//                                .queryParam("numOfRows", numOfRows)
//                                .build()
//                                .toUri();
//
//                log.info("Fetching real estate deals from: {}", uri);
//
//                return restClient.get()
//                                .uri(uri)
//                                .accept(MediaType.APPLICATION_XML, MediaType.TEXT_XML)
//                                .retrieve()
//                                .body(DealsApiResponse.class);
//        }

        public DealsApiResponse fetchDeals(String lawdCd, String dealYmd, int pageNo, int numOfRows) {
                URI uri = UriComponentsBuilder.fromUriString(dealsBaseUrl + "/RTMSDataSvcAptTradeDev/getRTMSDataSvcAptTradeDev")
                                .queryParam("serviceKey", serviceKey)
                                .queryParam("LAWD_CD", lawdCd)
                                .queryParam("DEAL_YMD", dealYmd)
                                .queryParam("pageNo", pageNo)
                                .queryParam("numOfRows", numOfRows)
                                .build()
                                .toUri();

                log.info("Fetching real estate deals detail from: {}", uri);

                return restClient.get()
                                .uri(uri)
                                .accept(MediaType.APPLICATION_XML, MediaType.TEXT_XML)
                                .retrieve()
                                .body(DealsApiResponse.class);
        }

        public AptPblancApiResponse fetchAptLttotPblancDetail(int page, int perPage) {
                URI uri = UriComponentsBuilder
                                .fromUriString(pblancBaseUrl + "/getAPTLttotPblancDetail")
                                .queryParam("page", page)
                                .queryParam("perPage", perPage)
                                .queryParam("serviceKey", serviceKey)
                                .build()
                                .toUri();

                log.info("Fetching APT Lttot Pblanc Detail from: {}", uri);

                return restClient.get()
                                .uri(uri)
                                .accept(MediaType.APPLICATION_JSON)
                                .retrieve()
                                .body(AptPblancApiResponse.class);
        }

        public RentPblancApiResponse fetchPblPvtRentLttotPblancDetail(int page, int perPage) {
                URI uri = UriComponentsBuilder.fromUriString(
                                pblancBaseUrl + "/getPblPvtRentLttotPblancDetail")
                                .queryParam("page", page)
                                .queryParam("perPage", perPage)
                                .queryParam("serviceKey", serviceKey)
                                .build()
                                .toUri();

                log.info("Fetching Pbl Pvt Rent Lttot Pblanc Detail from: {}", uri);

                return restClient.get()
                                .uri(uri)
                                .accept(MediaType.APPLICATION_JSON)
                                .retrieve()
                                .body(RentPblancApiResponse.class);
        }

        public ApartmentComplexApiResponse fetchTotalAptList(int pageNo, int numOfRows) {
                URI uri = UriComponentsBuilder
                                .fromUriString(aptListBaseUrl + "/AptListService3/getTotalAptList3")
                                .queryParam("serviceKey", serviceKey)
                                .queryParam("pageNo", pageNo)
                                .queryParam("numOfRows", numOfRows)
                                .queryParam("_type", "json")
                                .build()
                                .toUri();

                log.info("Fetching total apartment list from: {}", uri);

                return restClient.get()
                                .uri(uri)
                                .accept(MediaType.APPLICATION_JSON)
                                .retrieve()
                                .body(ApartmentComplexApiResponse.class);
        }

        public ApartmentComplexBasisInfoApiResponse fetchApartmentComplexBasisInfo(String kaptCode) {
                URI uri = UriComponentsBuilder
                                .fromUriString(aptBasisInfoBaseUrl + "/AptBasisInfoServiceV4/getAphusBassInfoV4")
                                .queryParam("serviceKey", serviceKey)
                                .queryParam("kaptCode", kaptCode)
                                .queryParam("_type", "json")
                                .build()
                                .toUri();

                log.info("Fetching apartment complex basis info from: {}", uri);

                return restClient.get()
                                .uri(uri)
                                .accept(MediaType.APPLICATION_JSON)
                                .retrieve()
                                .body(ApartmentComplexBasisInfoApiResponse.class);
        }
}
