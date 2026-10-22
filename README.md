# jfx-gtk4

> [!WARNING]
> Most of this code was developed by an AI model. It is meant for
> demonstration and as a proof of concept only, and has not been reviewed for
> production use. Do not use it in production.

## Requirements

- JDK 25
- A JavaFX SDK of the version the plugin is built against, since it uses
  internal APIs, with no `com.sun.glass.ui.gtk4` or `com.sun.prism.vk`
  classes in `javafx.graphics`
- The GObject introspection files of GTK 4.22 or later (`Gtk-4.0.gir`,
  `Gdk-4.0.gir`, `GdkWayland-4.0.gir`, `Adw-1.gir`, ...), since the wrappers
  are generated from its API. The glass checks the version of GTK at run time,
  so the plugin runs on GTK 4.14 or later. On a system with an older GTK, set
  `girDir` to the `gir-1.0` directory of a newer one.
- The Vulkan registry (`vk.xml`) and a Vulkan 1.3 driver
- `libshaderc` or `glslc`

## Build

```
./gradlew jar -PjfxSdk=/path/to/jfx/build/sdk
```

| Property     | Default                              |
|--------------|--------------------------------------|
| `jfxSdk`     | `../jfx/build/sdk`                   |
| `girDir`     | `/usr/share/gir-1.0`                 |
| `vkRegistry` | `/usr/share/vulkan/registry/vk.xml`  |

The build generates:

- the GTK 4 FFM wrappers from the GIR files (`generateGtk4Wrappers`)
- the Vulkan FFM bindings from `vk.xml` (`generateVulkanBindings`)
- the Vulkan GLSL of the Prism and Decora shaders from JSL, compiled to SPIR-V
  (`generatePrismShaders`, `generateDecoraShaders`, `compile*SPIRVShaders`)

## Run

The plugin runs with an unmodified JavaFX: a released SDK, the JavaFX jars
from Maven Central, or an SDK built from source. It was tested with JavaFX
27.0.1 and with a build of the master branch.

### Runtime requirements

- JDK 25
- JavaFX of the version the plugin was built against (the plugin uses its
  internal APIs), with no `com.sun.glass.ui.gtk4` or `com.sun.prism.vk`
  classes in `javafx.graphics`
- `jfx-gtk4.jar`, from `build/libs` (it contains the generated wrappers and
  the SPIR-V shaders, so the GIR files, `vk.xml` and `libshaderc` are only
  needed to build it)
- GTK 4.14 or later (`libgtk-4.so.1`), which has `GtkGraphicsOffload`: the
  frames are shown in a `GtkGraphicsOffload`, so that GTK can hand the
  dma-bufs to the compositor as subsurfaces. The glass refuses to start with
  an older GTK. libadwaita is used when available.
- For the `vk` pipeline: the Vulkan loader (`libvulkan.so.1`) and a driver
  with Vulkan 1.3 and `VK_KHR_push_descriptor`. Frames are handed to the
  compositor as dma-bufs when the driver also has `VK_KHR_external_memory_fd`
  and `VK_EXT_external_memory_dma_buf`, and copied otherwise. Without Vulkan,
  `-Dprism.order=vk,sw` falls back to the software pipeline.
- A Wayland compositor (GNOME, KDE Plasma, ...). Xorg is not supported: the
  glass does not work in an X11 session, nor with `GDK_BACKEND=x11`.

These requirements are checked when JavaFX starts:

- The glass fails to start, with the reason, when GTK 4 cannot be loaded, is
  older than 4.14, or lacks `GtkGraphicsOffload` or `GdkDmabufTextureBuilder`,
  and when there is no Wayland compositor (no socket at `$WAYLAND_DISPLAY`, or
  `GDK_BACKEND` selects another backend).
- The `vk` pipeline prints why Vulkan cannot be used (no loader, no Vulkan 1.3
  device, no `VK_KHR_push_descriptor`, ...) and JavaFX tries the next pipeline
  of `prism.order`. `-Dprism.verbose=true` shows the devices and the
  extensions in use.

### Command line

```
java --module-path=/path/to/javafx/lib --add-modules=javafx.controls \
     --enable-native-access=javafx.graphics,ALL-UNNAMED \
     --add-exports=javafx.base/com.sun.javafx=ALL-UNNAMED \
     --add-exports=javafx.base/com.sun.javafx.logging=ALL-UNNAMED \
     --add-exports=javafx.graphics/com.sun.glass.events=ALL-UNNAMED \
     --add-exports=javafx.graphics/com.sun.glass.ui=ALL-UNNAMED \
     --add-exports=javafx.graphics/com.sun.glass.ui.delegate=ALL-UNNAMED \
     --add-exports=javafx.graphics/com.sun.javafx.application.preferences=ALL-UNNAMED \
     --add-exports=javafx.graphics/com.sun.javafx.geom=ALL-UNNAMED \
     --add-exports=javafx.graphics/com.sun.javafx.geom.transform=ALL-UNNAMED \
     --add-exports=javafx.graphics/com.sun.javafx.sg.prism=ALL-UNNAMED \
     --add-exports=javafx.graphics/com.sun.javafx.tk=ALL-UNNAMED \
     --add-exports=javafx.graphics/com.sun.prism=ALL-UNNAMED \
     --add-exports=javafx.graphics/com.sun.prism.impl=ALL-UNNAMED \
     --add-exports=javafx.graphics/com.sun.prism.impl.ps=ALL-UNNAMED \
     --add-exports=javafx.graphics/com.sun.prism.paint=ALL-UNNAMED \
     --add-exports=javafx.graphics/com.sun.prism.ps=ALL-UNNAMED \
     --add-exports=javafx.graphics/com.sun.prism.shader=ALL-UNNAMED \
     -Dglass.platform=Gtk4 -Dprism.order=vk,sw \
     -cp jfx-gtk4.jar:app.jar app.Main
```

The plugin is on the class path, so the exports are to `ALL-UNNAMED`. With
JavaFX on the class path too (no module path), the `--add-exports` options are
not needed, but JavaFX reports it as an unsupported configuration and the main
class must not extend `Application`.

`./gradlew -q runArgs -PjfxSdk=DIR` prints these arguments for an SDK, and the
build writes them to `build/run.args`, for `java @build/run.args` (paths under the
home directory are relative to the project directory, from where it must run).

JavaFX loads `com.sun.glass.ui.gtk4.Gtk4PlatformFactory` for
`-Dglass.platform=Gtk4` and `com.sun.prism.vk.VKPipeline` for
`-Dprism.order=vk`. With a JavaFX that supports the `ext:` prefix, the classes
can also be given by name: `-Dglass.platform=ext:com.sun.glass.ui.gtk4.Gtk4PlatformFactory`.

| System property              | Description                                             |
|------------------------------|---------------------------------------------------------|
| `glass.gtk4.backdrop=true`   | Blurs the area behind toplevel windows, see [Backdrop](#backdrop) |
| `glass.gtk4.backdrop.active` | Set by the glass: whether the backdrop is in use            |
| `glass.gtk4.adwaita=false`   | Uses `GtkWindow` instead of `AdwWindow`                  |
| `glass.gtk4.portal=false`    | Uses GTK file dialogs instead of the portal             |
| `glass.gtk4.verbose=<cats>`  | Logs the given categories, or `all`                     |

## Backdrop

With `-Dglass.gtk4.backdrop=true`, the compositor blurs the area behind every
toplevel window that is not transparent, when it supports the
`ext-background-effect-v1` protocol (GNOME 51). The compositors that support
it are listed at <https://wayland.app/protocols/ext-background-effect-v1>.
The blur shows through where the scene fill and the content are transparent or
translucent, so the application sets a transparent `Scene` fill.

When the compositor does not support the protocol, the backdrop is disabled
and a warning is printed. The glass sets `glass.gtk4.backdrop.active` to `true`
or `false` before the application starts, so that the application can use an
opaque fill when there is no blur behind the window, as the demos do.

This property stands in for the `StageBackdrop` API of
[openjdk/jfx#2048](https://github.com/openjdk/jfx/pull/2048) (8375578: Enable
translucent window backdrops), which selects the backdrop per stage. When that
API is available in JavaFX, the plugin will implement it and the property will
be removed.

## Demos

The `*Demo.java` files in `demos/` are single-file applications that exercise
the platform: `aurora`, `disco`, `fireworks`, `fractals`, `nebula`,
`render` and `stage`. `Wallpaper.java` is the animated desktop used by
`launcher.sh` under KWin.

## Launcher

`launcher.sh` runs one or more demos together, on the current desktop or in a
nested compositor, with the plugin and the JavaFX SDK of `build/run.args`, so
the plugin is built first with `./gradlew jar`.

```
./launcher.sh --apps=nebula,fractals
./launcher.sh --apps=all --compositor=kwin
./launcher.sh --apps=stage --compositor=gnome
./launcher.sh --apps=disco --pipeline=sw --backdrop=off -- -Dglass.gtk4.verbose=all
```

| Option                       | Description                                                     |
|------------------------------|-----------------------------------------------------------------|
| `--apps=a,b,...`             | Demos to run, by name, or `all` (`nebula`)                     |
| `--compositor=none\|gnome\|kwin` | Current desktop, or a nested compositor (`none`)        |
| `--pipeline=vk\|sw`           | Prism pipeline (`vk`)                                          |
| `--backdrop=on\|off`          | Sets `glass.gtk4.backdrop` (`on`)                              |
| `--toolbox=NAME`             | Toolbox container with GNOME 51 (`jfx-gnome51`), or `none` for the GNOME Shell of the host |
| `--toolbox-release=N`        | Fedora release of a new toolbox container (`45`)               |
| `--wallpaper=FILE`           | gnome: desktop image; kwin: `javafx` for `Wallpaper.java`      |
| `--size=WxH`, `--scale=N`    | kwin: size and scale of the nested output                      |
| `--screenshot=FILE`          | gnome: screenshot of the nested desktop                        |
| `--list`, `--help`           | Lists the demos, or shows all the options                      |

Arguments after `--` are passed to the JVM of the demos. The logs are written
to `$TMPDIR/jfx-gtk4-launcher-<compositor>`.

With `--compositor=gnome`, GNOME Shell runs in a toolbox container, so that
GNOME 51 can be used on an older host. When the container does not exist, the
launcher asks before creating it from Fedora 45 and installing GNOME Shell,
the JDK, GTK 4 and the Vulkan drivers in it.

