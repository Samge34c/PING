# Hub SOFA — Festival Social

Implemented scope: only `/events/{id}/hub`, approved second composition (Samuel, DEC-031). Operate surface with an expressive event cover. Other routes keep their shared layout.

- Existing local Manrope and `--ping-*` theme tokens; Hub-only plum #251333, yellow #f8e956, purple #9b79d2. Fixed plum cover with light lettering works in both themes; operational areas follow the selected theme.
- Context and real next action before the cover. Split message/social illustration on desktop; compact sequence on mobile. Source and optimization: `docs/HUB_ART_PROVENANCE.json`. Image reserves 1536×1024 dimensions and a fixed crop; no lazy loading of visible art.
- Gamer yellow and larger; Cosplay purple poster, Communities compact, technology/music/cinema with specific original mini SVG art. On mobile the Gamer name comes first; the duplicate section map link is omitted because the contextual map action stays above the cover. Active meeting marker compares zone IDs derived from `experience.atPoint`, never pavilion numbers.
- Header/mobile navigation are exclusive fragments. Mobile scrollport reserves 68px plus safe area for its own bottom navigation. The Hub omits the shared main-entry animation. Controls keep at least 44px height and width; keyboard focus and reduced motion are explicit.
- Real alias, balance, event and match states; no fabricated activity. Academic/unofficial notices stay visible. No private recognition hints on Hub.

Editable sources: `templates/experience/hub.html`, `experience/hub-fragments.html`, scoped Festival Social section at end of `static/css/brand.css`. No new framework or dependency.
