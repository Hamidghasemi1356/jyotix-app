# Jyotix Font Files

The Dubai font is a proprietary font owned by the Government of Dubai.
It is available for free from https://dubaifont.com/.

To enable Dubai font in Jyotix:

1. Download Dubai Medium and Dubai Bold TTF files from https://dubaifont.com/.
2. Rename them to `dubai_medium.ttf` and `dubai_bold.ttf`.
3. Copy them to: `app/src/main/res/font/dubai_medium.ttf` and `app/src/main/res/font/dubai_bold.ttf`.

If Dubai font is not present, the application falls back to the system
Arabic-capable font (Noto Naskh Arabic on Android) which renders Persian
correctly.
