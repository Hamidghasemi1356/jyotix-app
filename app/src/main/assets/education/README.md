# Education Content

This folder holds the seed JSON file (`education.json`) that is loaded into
the Room database on first launch to populate the Astrology Education module.

## Format

```json
[
  {
    "category": "intro_jyotish",
    "title_fa": "مقدمه‌ای بر جیوتیش",
    "title_en": "Introduction to Jyotish",
    "body_fa": "محتوای مقاله...",
    "body_en": "Article body...",
    "order_index": 0
  }
]
```

Categories:
- `intro_jyotish` — Introduction to Jyotish
- `zodiac` — Zodiac Signs
- `planets` — Planets
- `houses` — Houses
- `nakshatras` — Nakshatras
- `vargas` — Vargas
- `dashas` — Dashas
- `panchanga` — Panchanga
- `aspects` — Aspects
- `transit` — Transit Analysis

To add new content later, append entries to `education.json` and reinstall,
or insert rows directly into the `education_articles` Room table.
