package com.aeris.cv.ota.vcpsadaptor.service;

import com.aeris.cv.ota.vcpsadaptor.configuration.ConfigurationService;
import com.aeris.cv.ota.vcpsadaptor.metrics.HttpExchangeMetricFilter;
import com.aeris.cv.ota.vcpsadaptor.service.campaignmgmt.CampaignMgmtService;
import com.aeris.cv.ota.vcpsadaptor.service.contentmgmt.ContentMgmtService;
import com.aeris.cv.ota.vcpsadaptor.service.fullmaphistory.FullMapHistoryService;
import com.aeris.cv.ota.vcpsadaptor.service.profilemgmt.HttpProfileMgmtService;
import com.aeris.cv.ota.vcpsadaptor.service.profilemgmt.ProfileMgmtService;
import com.aeris.cv.ota.vcpsadaptor.service.redbendadaptor.RedbendAdaptorClient;
import com.aeris.cv.ota.vcpsadaptor.service.sevicemgmt.ServiceCatalogueService;
import com.aeris.cv.ota.vcpsadaptor.service.sevicemgmt.ServiceMgmtService;
import com.aeris.cv.ota.vcpsadaptor.service.vespaadaptor.VespaAdaptorService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.support.WebClientAdapter;
import org.springframework.web.service.invoker.HttpServiceProxyFactory;

import java.time.Duration;
import java.time.temporal.ChronoUnit;

/**
 * @author Thomas Li 2023/9/12 10:22
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class WebServices {

    private final ConfigurationService configurationService;

    private final HttpExchangeMetricFilter httpExchangeMetricFilter;

    /**
     * if set defaultStatusHandler in client, all exception will be converted to ReactiveException we
     * can not handle this in controllerExceptionHandler, so remove defaultStatusHandler, throw http
     * exception directly
     */
    @Bean
    public CampaignMgmtService campaignMgmtService() {
        WebClient client = WebClient.builder()
                .baseUrl(configurationService.getCampaignMgmtUrl())
                .filter(httpExchangeMetricFilter)
                .build();
        HttpServiceProxyFactory factory =
                HttpServiceProxyFactory.builder(WebClientAdapter.forClient(client)).blockTimeout
                        (Duration.of(30, ChronoUnit.SECONDS)).build();
        return factory.createClient(CampaignMgmtService.class);
    }

    @Bean
    public ToolboxExchangeService toolboxExchangeService() {
        WebClient client = WebClient.builder()
                .baseUrl(configurationService.getToolboxUrl())
                .filter(httpExchangeMetricFilter)
                .build();
        HttpServiceProxyFactory factory =
                HttpServiceProxyFactory.builder(WebClientAdapter.forClient(client)).blockTimeout
                        (Duration.of(60, ChronoUnit.SECONDS)).build();
        return factory.createClient(ToolboxExchangeService.class);
    }

    @Bean
    public ContentMgmtService contentMgmtService() {
        WebClient client = WebClient.builder()
                .baseUrl(configurationService.getContentMgmtUrl())
                .filter(httpExchangeMetricFilter)
                .build();
        HttpServiceProxyFactory factory =
                HttpServiceProxyFactory.builder(WebClientAdapter.forClient(client)).blockTimeout
                        (Duration.of(30, ChronoUnit.SECONDS)).build();
        return factory.createClient(ContentMgmtService.class);
    }

    @Bean
    public HttpProfileMgmtService httpProfileMgmtService() {
        WebClient client = WebClient.builder()
                .baseUrl(configurationService.getProfileMgmtUrl())
                .filter(httpExchangeMetricFilter)
                .build();
        HttpServiceProxyFactory factory =
                HttpServiceProxyFactory.builder(WebClientAdapter.forClient(client)).blockTimeout
                        (Duration.of(30, ChronoUnit.SECONDS)).build();
        return factory.createClient(HttpProfileMgmtService.class);
    }

    @Bean
    public FullMapHistoryService fullMapHistoryService() {
        WebClient client = WebClient.builder()
                .baseUrl(configurationService.getFullMapHistoryUrl())
                .filter(httpExchangeMetricFilter)
                .build();
        HttpServiceProxyFactory factory =
                HttpServiceProxyFactory.builder(WebClientAdapter.forClient(client)).blockTimeout
                        (Duration.of(10, ChronoUnit.SECONDS)).build();
        return factory.createClient(FullMapHistoryService.class);
    }

    @Bean
    public ServiceCatalogueService serviceCatalogueService() {
        WebClient client = WebClient.builder()
                .baseUrl(configurationService.getServiceCatalogueUrl())
                .filter(httpExchangeMetricFilter)
                .build();
        HttpServiceProxyFactory factory =
                HttpServiceProxyFactory.builder(WebClientAdapter.forClient(client)).build();
        return factory.createClient(ServiceCatalogueService.class);
    }

    @Bean
    public ServiceMgmtService serviceMgmtService() {
        WebClient client = WebClient.builder()
                .baseUrl(configurationService.getServiceMgmtUrl())
                .filter(httpExchangeMetricFilter)
                .build();
        HttpServiceProxyFactory factory =
                HttpServiceProxyFactory.builder(WebClientAdapter.forClient(client)).build();
        return factory.createClient(ServiceMgmtService.class);
    }

    @Bean
    public VespaAdaptorService vespaAdaptorService() {
        WebClient client = WebClient.builder()
                .baseUrl(configurationService.getVespaAdaptorServerUrl())
                .filter(httpExchangeMetricFilter)
                .build();

        HttpServiceProxyFactory factory = HttpServiceProxyFactory.builder(WebClientAdapter.forClient(client)).build();
        return factory.createClient(VespaAdaptorService.class);
    }

    @Bean
    public RedbendAdaptorClient redbendAdaptorClient() {
        WebClient client = WebClient.builder()
                .baseUrl(configurationService.getRedbendAdaptorServerUrl())
                .filter(httpExchangeMetricFilter)
                .build();

        HttpServiceProxyFactory factory = HttpServiceProxyFactory.builder(WebClientAdapter.forClient(client)).build();
        return factory.createClient(RedbendAdaptorClient.class);
    }

}
