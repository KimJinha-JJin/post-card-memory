# Post Card Memory

Post Card Memory is an Android postcard-stamp gallery prototype. It lets people capture or import photos, crops the central area into a fixed stamp-shaped postcard frame, and saves the rendered card into `Pictures/PostCardMemory`.

## Product direction

- **Neo-brutalist UI**: thick black outlines, chunky rounded cards, bright pastel pink/yellow/mint surfaces, and elevated tactile buttons.
- **Cute collection loop**: memories are treated like collectible stamps in a small visual dex, inspired by the feeling of collecting creatures or cards.
- **Camera and gallery flow**: users can take a new photo with the camera or pick one from the file/gallery picker.
- **Postcard stamp output**: every selected image is center-cropped into the same postcard stamp frame before being saved.

## Build

This repository is a Gradle Android project:

```bash
gradle :app:assembleDebug
```

An Android SDK and access to the Android Gradle Plugin/Kotlin dependencies are required.
