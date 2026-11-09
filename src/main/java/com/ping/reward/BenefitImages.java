package com.ping.reward;
import org.springframework.http.*;
/** Shared response for reward images: versioned URL, private cache, no sniffing. */
final class BenefitImages {
    private BenefitImages() { }
    static ResponseEntity<byte[]> response(BenefitImageService.Stored stored) {
        return ResponseEntity.ok().contentType(MediaType.parseMediaType(stored.contentType()))
            .header("Cache-Control", "private, max-age=604800").header("X-Content-Type-Options", "nosniff")
            .header("Content-Disposition", "inline").eTag("\"" + stored.revision() + "\"").body(stored.bytes());
    }
}
