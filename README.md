# CrosshairEditor

## Setup

For setup instructions, please see the [Fabric Documentation page](https://docs.fabricmc.net/develop/getting-started/creating-a-project#setting-up) related to the IDE that you are using.

## Inverted crosshair

The HUD preserves the original 9×9, one-pixel cross and renders it with three disjoint white `GUI_INVERT` fills. With Difference blending, opaque white produces `1 - background` per RGB channel; this is background-color inversion, not a luminance-threshold effect. The shared center is included once, so no pixel is inverted twice. No Gaussian blur or texture is used at runtime. The prior Gaussian generator, tests, and PNG remain in the repository but are inactive.

The focused source-structure regression and Minecraft 26.3 / Java 25 build checks are:

```bash
PYTHONDONTWRITEBYTECODE=1 python3 -m unittest discover -s tools -p 'test_crosshair*.py' -v
bash ./gradlew compileClientJava
bash ./gradlew build
```

These structural tests verify fill geometry and source wiring; they do not test GPU blending or runtime Mixin application. Manual checks on dark, light, and colored backgrounds, GUI scales, visibility behavior, and the attack indicator remain pending; no GUI runtime check has been performed.

## License

This template is available under the CC0 license. Feel free to learn from it and incorporate it in your own projects.
