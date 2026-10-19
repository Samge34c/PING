package com.ping.web;

import com.ping.event.EventEntryLinks;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.ui.ExtendedModelMap;
import static org.assertj.core.api.Assertions.*;

class PagePresentationTests {
    @Test void publicAddressUsesExplicitConfigurationNeverRequestHostOrQuery() {
        var pages=new PagePresentation(new EventEntryLinks("https://ping.example.test/"));
        var request=new MockHttpServletRequest("GET","/join");request.setServerName("untrusted.example");request.addHeader("Forwarded","host=otro.example");request.setQueryString("code=SECRET&alias=PERSONAL");
        var model=new ExtendedModelMap();pages.present(request,model);
        var meta=(PagePresentation.PublicMetadata)model.getAttribute("publicMetadata");
        assertThat(meta.publicPage()).isTrue();assertThat(meta.canonical()).isEqualTo("https://ping.example.test/join");
        assertThat(meta.image()).isEqualTo("https://ping.example.test/img/ping-social.png");
        for(String path:java.util.List.of("/me","/matches/1","/events/1","/events/1/plan","/admin","/join/interests")) {
            request.setRequestURI(path);pages.present(request,model);meta=(PagePresentation.PublicMetadata)model.getAttribute("publicMetadata");
            assertThat(meta.publicPage()).isFalse();assertThat(meta.canonical()).isNull();assertThat(meta.image()).isNull();
        }
    }
    @Test void unconfiguredDevelopmentHasNoInventedAbsoluteLinks() {
        var model=new ExtendedModelMap();new PagePresentation(new EventEntryLinks("")).present(new MockHttpServletRequest("GET","/"),model);
        var meta=(PagePresentation.PublicMetadata)model.getAttribute("publicMetadata");assertThat(meta.publicPage()).isTrue();
        assertThat(meta.canonical()).isNull();assertThat(meta.image()).isNull();
    }
}
