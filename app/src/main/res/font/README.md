# Jyotix Custom Font Files

Place your Persian-capable font TTF files here.

## Recommended: Dubai Font

The Dubai font is a proprietary font owned by the Government of Dubai,
available free from https://dubaifont.com/.

To enable Dubai font in Jyotix:

1. Download Dubai-Medium.ttf and Dubai-Bold.ttf from https://dubaifont.com/.
2. Copy them here (app/src/main/res/font/) as:
   - `dubai_medium.ttf`
   - `dubai_bold.ttf`

## Fallback Behavior

If the TTF files are not present, the application uses Android's
system Persian-capable font (Vazirmatn / Noto Naskh Arabic / Noto Sans Arabic
depending on the device), which renders Persian correctly.

The font loading is handled in `core/ui/theme/Type.kt` with a graceful
fallback, so the project builds successfully even without the TTFs.
