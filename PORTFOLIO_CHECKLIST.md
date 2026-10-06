# Portfolio Finishing Checklist

## Before sharing this repository

- [ ] Play through several waves and confirm title → gameplay → perk/upgrade → game-over flows work reliably.
- [ ] Test every currently implemented gesture on a real device/emulator.
- [ ] Run `./gradlew connectedAndroidTest` with an emulator/device connected.
- [ ] Decide whether to implement the intended fifth/high-tier gesture before college applications; if not, keep the README wording as-is.
- [ ] If you change gesture behavior, update both tests and the README gesture table together.

## Media to add

- [ ] Record a 15–25 second gameplay clip showing at least two different gesture kills.
- [ ] Include one movement-pattern moment and one upgrade/perk interaction.
- [ ] Convert the short clip to `docs/demo/karate-slicer-demo.gif` and uncomment the GIF line in the main README.
- [ ] Add `title.png`, `gameplay.png`, `upgrades.png`, `wave-perk.png`, and `game-over.png` under `docs/screenshots/`.
- [ ] Uncomment the screenshot table in the main README.

## Highest-value technical improvement

- [ ] Implement the fifth/high-tier gesture.
- [ ] Add noisy/synthetic strokes that test borderline cases and false positives.
- [ ] Document the recognition thresholds and why you chose them.

## GitHub presentation

- [ ] Add repository description: `Gesture-controlled Android arcade game with a custom SurfaceView loop and rule-based stroke recognition.`
- [ ] Add topics: `android`, `java`, `game-development`, `gesture-recognition`, `surfaceview`, `algorithms`.
- [ ] Make the repository public when ready for applications.
- [ ] Pin this repository on your GitHub profile.
- [ ] Consider linking a 30–60 second narrated YouTube demo from the README after the GIF.
