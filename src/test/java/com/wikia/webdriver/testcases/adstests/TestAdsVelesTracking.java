package com.wikia.webdriver.testcases.adstests;

import com.wikia.webdriver.common.core.Assertion;
import com.wikia.webdriver.common.core.annotations.NetworkTrafficDump;
import com.wikia.webdriver.common.core.url.Page;
import com.wikia.webdriver.common.dataprovider.ads.AdsDataProvider;
import com.wikia.webdriver.common.templates.NewTestTemplate;
import com.wikia.webdriver.pageobjectsfactory.pageobject.adsbase.AdsBaseObject;

import io.netty.handler.codec.http.DefaultFullHttpResponse;
import io.netty.handler.codec.http.HttpRequest;
import io.netty.handler.codec.http.HttpResponse;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.HttpVersion;
import net.lightbody.bmp.core.har.HarEntry;
import net.lightbody.bmp.filters.RequestFilter;
import net.lightbody.bmp.util.HttpMessageContents;
import net.lightbody.bmp.util.HttpMessageInfo;
import org.apache.http.NameValuePair;
import org.apache.http.client.utils.URLEncodedUtils;
import org.testng.annotations.Test;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static java.util.stream.Collectors.toSet;

public class TestAdsVelesTracking extends NewTestTemplate {

  public static final String VELES_TRACKING_REQUEST_PART = "adengadinfo";

  public static final String VELES_TRACKING_POSITION_PARAMETER = "kv_pos";

  public static final String VELES_TRACKING_PRICE_PARAMETER = "bidder_8";

  @NetworkTrafficDump
  @Test(
      groups = "AdsTrackingVeles",
      dataProviderClass = AdsDataProvider.class,
      dataProvider = "adsVelesTracking")
  public void adsTrackingVelesTracked(final Page page, final Map<String, String> positionsAndPrices) {
    networkTrafficInterceptor.addRequestFilter(new RequestFilter() {
      @Override
      public HttpResponse filterRequest(HttpRequest request, HttpMessageContents contents,
                                        HttpMessageInfo messageInfo) {

        if(request.getUri().contains("ads")){
          DefaultFullHttpResponse
              response = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.INTERNAL_SERVER_ERROR);
          return response;
        }
        return null;
      }
    });
    networkTrafficInterceptor.startIntercepting();
    AdsBaseObject pageObject = new AdsBaseObject(driver, urlBuilder.getUrlForPage(page));

    Set<HarEntry> entries = findEntriesByUrlPart(pageObject, VELES_TRACKING_REQUEST_PART, positionsAndPrices);

    Assertion.assertTrue(wasVelesTrackedIn(entries, positionsAndPrices), "Veles should be tracked only in " + positionsAndPrices);
  }

  private Set<HarEntry> findEntriesByUrlPart(AdsBaseObject pageObject, final String s, final Map<String, String> positionsAndPrices) {
    for(String slotName: positionsAndPrices.keySet()) {
      waitForSlotTrackingRequest(pageObject, slotName);
    }

    return networkTrafficInterceptor.getHar().getLog().getEntries().stream()
        .filter(entry -> entry.getRequest().getUrl().contains(s))
        .collect(toSet());
  }

  private void waitForSlotTrackingRequest(AdsBaseObject pageObject, String slotName) {
    final String pattern = String.format(".*%s.*%s=%s.*", VELES_TRACKING_REQUEST_PART, VELES_TRACKING_POSITION_PARAMETER, slotName);
    pageObject.wait.forSuccessfulResponseByUrlPattern(networkTrafficInterceptor, pattern);
  }

  private boolean wasVelesTrackedIn(final Set<HarEntry> entries, final Map<String, String> positionsAndPrices) {
    boolean result = false;

    if (!entries.isEmpty()) {

      result = entries.stream()
          .map(entry -> URLEncodedUtils.parse(entry.getRequest().getUrl(), StandardCharsets.UTF_8))
          .map(VelesTrackingInfo::fromParameters)
          .allMatch(trackingInfo -> validatePricesIn(trackingInfo, positionsAndPrices));
    }

    return result;
  }

  private boolean validatePricesIn(final VelesTrackingInfo trackingInfo, final Map<String, String> positionsAndPrices) {
    return positionsAndPrices.containsKey(trackingInfo.getPosition())
        ? positionsAndPrices.get(trackingInfo.getPosition()).equals(trackingInfo.getPrice())
        : "".equals(trackingInfo.getPrice());
  }

  private static class VelesTrackingInfo {
    private final String position;

    private final String price;

    private VelesTrackingInfo(String position, String price) {
      this.position = position;
      this.price = price;
    }

    String getPosition() {
      return position;
    }

    String getPrice() {
      return price;
    }

    @Override
    public String toString() {
      return "VelesTrackingInfo{" +
          "position='" + position + '\'' +
          ", price='" + price + '\'' +
          '}';
    }

    private static VelesTrackingInfo fromParameters(final List<NameValuePair> parameters) {
      return new VelesTrackingInfo(find(parameters, VELES_TRACKING_POSITION_PARAMETER), find(parameters, VELES_TRACKING_PRICE_PARAMETER));
    }

    private static String find(final List<NameValuePair> parameters, final String key) {
      return parameters.stream()
          .filter(parameter -> key.equals(parameter.getName()))
          .findFirst()
          .map(NameValuePair::getValue)
          .orElse("");
    }
  }
}
