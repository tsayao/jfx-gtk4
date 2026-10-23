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
- the OpenGL 4.5 core GLSL of the same shaders, from the `-gl` backend of the
  JSL compiler, and of the static Vulkan shaders, whose descriptor sets, push
  constants and specialization constants become bindings, a uniform block
  and macros (`copy*GLShaders`, `generateGLStaticShaders`)

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
- For the `gl` pipeline (`-Dprism.order=gl`): EGL 1.5 with the GBM platform,
  `EGL_KHR_surfaceless_context` and `EGL_EXT_image_dma_buf_import`, and an
  OpenGL 4.5 core driver with `GL_OES_EGL_image`, on a render node
  (`/dev/dri/renderD128` and the next ones, or `-Dprism.gl.device=PATH`). It
  renders with an upper-left origin and a [0, 1] depth range
  (`glClipControl`), as Vulkan does, so both pipelines share their shaders,
  projections and exported frames: linear ARGB8888 buffers of the render
  node, synchronized with a sync file when `EGL_ANDROID_native_fence_sync` is
  available. It does not use the ES2 pipeline of JavaFX.
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
  extensions in use. The `gl` pipeline does the same for EGL and OpenGL.

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

Others test the platform integration, after the manual tests of OpenJFX:

| Demo            | Tests                                                                    |
|-----------------|--------------------------------------------------------------------------|
| `dragdrop`      | Transfer modes and modifiers, data formats, drag images and offsets, drops from and to other applications, full drag |
| `clipboard`     | Copy and paste of text, HTML, RTF, URLs, files, images and custom formats, checked by a second process |
| `touch`         | Touch points, and the scroll, zoom, rotate and swipe gestures of touch screens and touchpads |
| `robot`         | A self test of `javafx.scene.robot.Robot`: pointer, buttons, wheel, keyboard, pixels and screen capture |
| `accessibility` | Accessible roles, names and states, live changes, and an inspector of the accessible attributes |
| `windows`       | Every window style, focus, maximized, full screen and minimized states before and after show, modality, min and max sizes, popups |
| `dialogs`       | File and folder choosers (through the portal), extension filters, alerts, text input and choice dialogs |
| `scene3d`       | 3D rendering checked on the screen and in snapshots, and the frame rate of heavier scenes; skipped on the `sw` pipeline |
| `swing`         | SwingNode painting and input, a JFXPanel in a JFrame, the EDT and FX threads, and the clipboard between Swing (X11, through Xwayland) and JavaFX |
| `tests`         | The tests of all of the above, in a tab per lab, run one lab after the other |

Each of these labs has a table of tests, which show OK, FAIL or SKIP (see
`demos/TestKit.java`), and `tests` shows all the labs in one window, a tab
per lab, with an overview and a button that runs the tests of every lab:

- automatic tests check what the lab can do alone, such as reading the
  clipboard in a second process, exchanging data with `wl-copy` and
  `wl-paste`, or reading the AT-SPI tree of the application with `python3`
  and the Atspi bindings, as a screen reader does;
- robot tests drive the lab with `javafx.scene.robot.Robot`: drags between
  the tiles and to the drop target, copy and paste with the keyboard, focus
  traversal and mnemonics. The GTK 4 glass generates robot input only in the
  GNOME test session, so they run under `--compositor=gnome`, where the
  launcher passes `-Dglass.gtk4.testSession=gnome`, and are skipped elsewhere;
- manual tests wait for a device the robot does not have, such as a touch
  screen: they pass when the lab sees their events, or with their OK and FAIL
  buttons.

```
./launcher.sh --apps=tests --compositor=gnome
./launcher.sh --apps=tests --compositor=gnome -- -Dtests.run=true -Dtests.exit=true
```

`-Dtests.run=true` runs the tests at startup and `-Dtests.exit=true` exits
afterwards, with status 1 when a test failed; each result is also printed, as
`OK`, `FAIL` or `SKIP` lines. A toolbox container created before these tests
needs their tools: `toolbox run -c jfx-gnome51 sudo dnf install wl-clipboard
python3-gobject at-spi2-core`.

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
| `--pipeline=vk\|gl\|sw`        | Prism pipeline: Vulkan, OpenGL 4.5 or software (`vk`)          |
| `--backdrop=on\|off`          | Sets `glass.gtk4.backdrop` (`on`)                              |
| `--toolbox=NAME`             | Toolbox container with GNOME 51 (`jfx-gnome51`), or `none` for the GNOME Shell of the host |
| `--toolbox-release=N`        | Fedora release of a new toolbox container (`45`)               |
| `--wallpaper=FILE`           | gnome: desktop image; kwin: `javafx` for `Wallpaper.java`      |
| `--size=WxH`, `--scale=N`    | gnome, kwin: size of the nested output (2400x1440); kwin: its scale |
| `--screenshot=FILE`          | gnome: screenshot of the nested desktop                        |
| `--list`, `--help`           | Lists the demos, or shows all the options                      |

Arguments after `--` are passed to the JVM of the demos. The logs are written
to `$TMPDIR/jfx-gtk4-launcher-<compositor>`.

With `--compositor=gnome`, GNOME Shell runs in a toolbox container, so that
GNOME 51 can be used on an older host. When the container does not exist, the
launcher asks before creating it from Fedora 45 and installing GNOME Shell,
the JDK, GTK 4 and the Vulkan drivers in it.

