#!/bin/sh
#
# Copyright (c) 2026, Thiago Sayão. All rights reserved.
# DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
#
# This code is free software; you can redistribute it and/or modify it
# under the terms of the GNU General Public License version 2 only, as
# published by the Free Software Foundation.  The copyright holder designates
# this particular file as subject to the "Classpath" exception as provided
# in the LICENSE file that accompanied this code.
#
# This code is distributed in the hope that it will be useful, but WITHOUT
# ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
# FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
# version 2 for more details (a copy is included in the LICENSE file that
# accompanied this code).
#
# You should have received a copy of the GNU General Public License version
# 2 along with this work; if not, write to the Free Software Foundation,
# Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
#

# Runs one or more of the demos in the demos directory with the GTK 4 glass,
# on the current desktop or in a nested compositor that supports translucent
# window backdrops (ext-background-effect-v1). The demos are started together,
# in the same desktop.
#
#   none   the current desktop and session bus, with the backdrop support of
#          the running compositor (the default)
#   gnome  GNOME Shell (51 or later) in a toolbox container, so that a newer
#          GNOME than the one of the host can be used. The desktop shows the
#          wallpaper of the host. When the container does not exist, the
#          launcher asks before creating it.
#   kwin   KWin over the Plasma desktop, or over an animated JavaFX wallpaper
#          when plasmashell is not installed.
#
# A nested compositor runs on a private D-Bus session with its own settings,
# so it leaves the running desktop untouched, and is closed when the last demo
# exits.
#
# Requires a JavaFX SDK and runs inside a Wayland session. The plugin is built
# with the SDK when build/run.args does not exist or names another SDK.
#
# Usage: ./launcher.sh [options] [-- extra JVM options]
#
# Options:
#   --apps=a,b,...          demos to run, by name (see --list), or "all"
#                           (nebula)
#   --sdk=DIR               JavaFX SDK ($JFX_SDK or ../jfx/build/sdk)
#   --compositor=gnome|kwin|none
#                           nested compositor, or none for the current
#                           desktop (none)
#   --pipeline=vk|sw        Prism pipeline (vk)
#   --backdrop=on|off       blur the area behind the windows (on)
#   --toolbox=NAME          toolbox container with GNOME (jfx-gnome51), or
#                           "none" to use the GNOME Shell of the host
#   --toolbox-release=N     Fedora release of a new toolbox container (45)
#   --wallpaper=FILE        gnome: image of the desktop (the host wallpaper)
#                           kwin: "javafx" for the JavaFX wallpaper
#   --size=WxH              kwin: size of the nested output (2400x1440)
#   --scale=N               kwin: output scale (1)
#   --screenshot=FILE       gnome: save a screenshot of the nested desktop
#   --screenshot-delay=S    seconds to wait before the screenshot (12)
#   --list                  list the demos and exit
#   --help, -h              show this help and exit
#
# The environment variables GDK_DISABLE and GSK_DEBUG are passed to the demos.
# The extra JVM options are passed to the demos.
#
# The first run of each demo and pipeline records an AOT cache of the JDK and
# JavaFX classes, which later runs use to start faster. It is rebuilt when the
# SDK or the plugin changes.
#
# Examples:
#   ./launcher.sh --apps=nebula,fractals
#   ./launcher.sh --apps=all --compositor=kwin
#   ./launcher.sh --apps=stage --compositor=gnome --sdk=../jfx/build/sdk

ROOT=$(cd "$(dirname "$0")" && pwd)
DEMOS=$ROOT/demos

APPS=nebula
SDK=${JFX_SDK:-$ROOT/../jfx/build/sdk}
COMPOSITOR=none
PIPELINE=vk
BACKDROP=on
TOOLBOX=jfx-gnome51
TOOLBOX_RELEASE=45
TOOLBOX_PACKAGES="gnome-shell mutter-devkit dbus-daemon nautilus java-25-openjdk-devel gtk4 libadwaita \
vulkan-loader mesa-vulkan-drivers mesa-libgbm dejavu-sans-fonts google-noto-sans-fonts"
WALLPAPER=
SIZE=2400x1440
SCALE=1
SCREENSHOT=
SCREENSHOT_DELAY=12

usage() {
    sed -n '/^# Runs one or more/,/^[^#]/p' "$0" | sed -e '/^[^#]/d' -e 's/^# \{0,1\}//'
    echo "Demos: $(demos | paste -sd' ' -)"
    exit "${1:-0}"
}

demos() {
    for f in "$DEMOS"/*Demo.java; do
        name=$(basename "$f" Demo.java)
        printf '%s\n' "$name" | tr '[:upper:]' '[:lower:]'
    done
}

while [ $# -gt 0 ]; do
    case "$1" in
        --apps=*) APPS=${1#*=} ;;
        --sdk=*) SDK=${1#*=} ;;
        --compositor=*) COMPOSITOR=${1#*=} ;;
        --pipeline=*) PIPELINE=${1#*=} ;;
        --backdrop=*) BACKDROP=${1#*=} ;;
        --toolbox=*) TOOLBOX=${1#*=} ;;
        --toolbox-release=*) TOOLBOX_RELEASE=${1#*=} ;;
        --wallpaper=*) WALLPAPER=${1#*=} ;;
        --size=*) SIZE=${1#*=} ;;
        --scale=*) SCALE=${1#*=} ;;
        --screenshot=*) SCREENSHOT=${1#*=} ;;
        --screenshot-delay=*) SCREENSHOT_DELAY=${1#*=} ;;
        --list) demos; exit 0 ;;
        --help|-h) usage 0 ;;
        --) shift; break ;;
        *) echo "Unknown option: $1 (see --help)" >&2; exit 2 ;;
    esac
    shift
done

case "$COMPOSITOR" in
    gnome|kwin|none) ;;
    *) echo "Unknown compositor: $COMPOSITOR (gnome, kwin or none)" >&2; exit 2 ;;
esac
case "$PIPELINE" in
    vk|sw) ;;
    *) echo "Unknown pipeline: $PIPELINE (vk or sw)" >&2; exit 2 ;;
esac
case "$BACKDROP" in
    on) BACKDROP_OPTION=-Dglass.gtk4.backdrop=true ;;
    off) BACKDROP_OPTION=-Dglass.gtk4.backdrop=false ;;
    *) echo "Unknown backdrop: $BACKDROP (on or off)" >&2; exit 2 ;;
esac
if [ "$APPS" = all ]; then
    APPS=$(demos | paste -sd, -)
fi
CLASSES=
for app in $(printf '%s' "$APPS" | tr ',' ' '); do
    want=$(printf '%s' "${app%Demo}" | tr '[:upper:]' '[:lower:]')
    class=
    for f in "$DEMOS"/*Demo.java; do
        name=$(basename "$f" .java)
        if [ "$(printf '%s' "${name%Demo}" | tr '[:upper:]' '[:lower:]')" = "$want" ]; then
            class=$name
        fi
    done
    if [ -z "$class" ]; then
        echo "Unknown demo: $app (available: $(demos | paste -sd' ' -))" >&2
        exit 2
    fi
    CLASSES="$CLASSES $class"
done
if [ -z "$CLASSES" ]; then
    echo "No demos given" >&2
    exit 2
fi

if [ ! -d "$SDK/lib" ]; then
    echo "JavaFX SDK not found: $SDK (use --sdk=DIR or JFX_SDK)" >&2
    exit 1
fi
SDK=$(cd "$SDK" && pwd)
if [ -z "$WAYLAND_DISPLAY" ]; then
    echo "This script runs inside a Wayland session" >&2
    exit 1
fi

RUN_ARGS=$ROOT/build/run.args
if [ ! -f /run/.toolboxenv ] && { [ ! -f "$RUN_ARGS" ] || ! grep -qF "\"--module-path=$SDK/lib\"" "$RUN_ARGS"; }; then
    echo "Building the plugin with $SDK"
    (cd "$ROOT" && sh ./gradlew -q jar -PjfxSdk="$SDK") || exit 1
fi

relaunch() {
    "$@" sh "$0" --apps="$APPS" --sdk="$SDK" --compositor="$COMPOSITOR" --pipeline="$PIPELINE" \
        --backdrop="$BACKDROP" --toolbox="$TOOLBOX" --toolbox-release="$TOOLBOX_RELEASE" \
        --wallpaper="$WALLPAPER" --size="$SIZE" --scale="$SCALE" --screenshot="$SCREENSHOT" \
        --screenshot-delay="$SCREENSHOT_DELAY" -- $JVM_OPTIONS
}
JVM_OPTIONS="$*"

ensure_toolbox() {
    if podman container exists "$TOOLBOX" 2>/dev/null; then
        return
    fi
    echo "The toolbox container $TOOLBOX does not exist. It can be created from Fedora $TOOLBOX_RELEASE"
    echo "(GNOME 51 is in Fedora 45), with the packages:"
    echo "  $TOOLBOX_PACKAGES"
    printf 'Create it? [y/N] '
    answer=
    if ! { read -r answer < /dev/tty; } 2>/dev/null; then
        echo
    fi
    case "$answer" in
        y|Y|yes|s|S|sim) ;;
        *)
            echo "Not created; use --toolbox=none to use the GNOME Shell of the host" >&2
            exit 1
            ;;
    esac
    toolbox --assumeyes create --distro fedora --release "$TOOLBOX_RELEASE" "$TOOLBOX" || exit 1
    toolbox run --container "$TOOLBOX" sudo dnf install -y $TOOLBOX_PACKAGES || exit 1
}

if [ "$COMPOSITOR" = gnome ]; then
    if [ -z "$WALLPAPER" ]; then
        WALLPAPER=$(gsettings get org.gnome.desktop.background picture-uri 2>/dev/null \
            | sed -e "s/^'//" -e "s/'$//" -e 's|^file://||')
    fi
    if [ "$TOOLBOX" != none ] && [ ! -f /run/.toolboxenv ]; then
        ensure_toolbox
        relaunch toolbox run --container "$TOOLBOX" env GDK_DISABLE="$GDK_DISABLE" GSK_DEBUG="$GSK_DEBUG"
        exit $?
    fi
fi

if [ "$COMPOSITOR" != none ] && [ -z "$LAUNCHER_SESSION" ]; then
    export LAUNCHER_SESSION=1
    relaunch dbus-run-session --
    exit $?
fi

SOCKET=wayland-launcher-$COMPOSITOR-$$
LOGS=${TMPDIR:-/tmp}/jfx-gtk4-launcher-$COMPOSITOR
[ "$COMPOSITOR" = gnome ] && LOGS=$LOGS-$TOOLBOX
mkdir -p "$LOGS"
HOST_DISPLAY=$WAYLAND_DISPLAY
BUS=$DBUS_SESSION_BUS_ADDRESS
CHILDREN=

cleanup() {
    for pid in $PIDS; do
        pkill -TERM -P "$pid" 2>/dev/null
    done
    kill $CHILDREN $PIDS 2>/dev/null
    if [ "$COMPOSITOR" = none ]; then
        return
    fi
    rm -f "${XDG_RUNTIME_DIR:-/run/user/$(id -u)}/$SOCKET" "${XDG_RUNTIME_DIR:-/run/user/$(id -u)}/$SOCKET.lock"
    for p in /proc/[0-9]*; do
        pid=${p#/proc/}
        [ "$pid" = $$ ] && continue
        if grep -qzx -e "DBUS_SESSION_BUS_ADDRESS=$BUS" -e "DBUS_STARTER_ADDRESS=$BUS" -F "$p/environ" 2>/dev/null; then
            kill "$pid" 2>/dev/null
        fi
    done
}
trap cleanup EXIT
trap 'exit 130' INT TERM

aot_option() {
    aot=$LOGS/$1-$PIPELINE.aot
    if [ -f "$aot" ] && { [ -n "$(find "$SDK/lib" -newer "$aot" -name '*.jar' | head -1)" ] \
            || [ "$ROOT/build/libs/jfx-gtk4.jar" -nt "$aot" ]; }; then
        rm -f "$aot"
    fi
    if [ -f "$aot" ]; then
        echo "-XX:AOTCache=$aot"
    else
        echo "-XX:AOTCacheOutput=$aot"
    fi
}

run() {
    exec java @"$RUN_ARGS" \
        --add-exports javafx.graphics/com.sun.prism=ALL-UNNAMED \
        -Dprism.order=$PIPELINE $BACKDROP_OPTION "$@"
}

wait_for_socket() {
    for i in $(seq 1 100); do
        [ -S "${XDG_RUNTIME_DIR:-/run/user/$(id -u)}/$SOCKET" ] && return
        sleep 0.2
    done
    echo "The nested compositor did not start, see $LOGS" >&2
    exit 1
}

start_gnome() {
    wallpaper=$WALLPAPER
    if [ -n "$wallpaper" ] && [ ! -f "$wallpaper" ] && [ -f "/run/host$wallpaper" ]; then
        wallpaper=/run/host$wallpaper
    fi
    export XDG_CONFIG_HOME="$LOGS/config"
    rm -rf "$XDG_CONFIG_HOME"
    mkdir -p "$XDG_CONFIG_HOME/glib-2.0/settings"
    cat > "$XDG_CONFIG_HOME/glib-2.0/settings/keyfile" <<EOF
[org/gnome/shell]
welcome-dialog-last-shown-version='999'

[org/gnome/desktop/interface]
enable-hot-corners=false

[org/gnome/desktop/session]
idle-delay=uint32 0

[org/gnome/desktop/notifications]
show-banners=false

[org/gnome/desktop/screensaver]
idle-activation-enabled=false
lock-enabled=false

[org/gnome/desktop/background]
picture-uri='file://$wallpaper'
picture-uri-dark='file://$wallpaper'
picture-options='zoom'
EOF
    export GSETTINGS_BACKEND=keyfile

    (unset WAYLAND_DISPLAY DISPLAY; exec gnome-shell --devkit --unsafe-mode --wayland \
        --wayland-display=$SOCKET) > "$LOGS/shell.log" 2>&1 &
    CHILDREN="$CHILDREN $!"
    wait_for_socket
    for i in $(seq 1 150); do
        gdbus call --session --dest org.gnome.Shell --object-path /org/gnome/Shell \
            --method org.gnome.Shell.Eval "Main.layoutManager._startingUp" 2>/dev/null \
            | grep -q "'false'" && break
        sleep 0.2
    done
    gdbus call --session --dest org.gnome.Shell --object-path /org/gnome/Shell \
        --method org.gnome.Shell.Eval "Main.overview.hide()" > /dev/null 2>&1
    sleep 1

    export WAYLAND_DISPLAY=$SOCKET GDK_BACKEND=wayland XDG_CURRENT_DESKTOP=GNOME
    unset DISPLAY
    dbus-update-activation-environment WAYLAND_DISPLAY XDG_CURRENT_DESKTOP > /dev/null 2>&1
    if [ -n "$SCREENSHOT" ]; then
        (sleep "$SCREENSHOT_DELAY"
         gdbus call --session --dest org.gnome.Shell.Screenshot --object-path /org/gnome/Shell/Screenshot \
             --method org.gnome.Shell.Screenshot.Screenshot false false "$SCREENSHOT") > /dev/null 2>&1 &
        CHILDREN="$CHILDREN $!"
    fi
}

kde() {
    exec env XDG_CONFIG_HOME="$LOGS/config" XDG_CURRENT_DESKTOP=KDE KDE_FULL_SESSION=true \
        KDE_SESSION_VERSION=6 QT_QPA_PLATFORM=wayland "$@"
}

start_kwin() {
    mkdir -p "$LOGS/config"
    (kde kwriteconfig6 --file kded6rc --group Module-plasma_welcome --key autoload false)
    (kde kwriteconfig6 --file ksplashrc --group KSplash --key Engine none)
    (kde kwriteconfig6 --file ksplashrc --group KSplash --key Theme None)
    (kde kwriteconfig6 --file kwinrc --group Plugins --key blurEnabled true)
    (kde kwriteconfig6 --file kwinrc --group Plugins --key contrastEnabled true)

    (kde kwin_wayland --wayland-display "$HOST_DISPLAY" --socket $SOCKET \
        --width "${SIZE%x*}" --height "${SIZE#*x}" --scale "$SCALE") > "$LOGS/kwin.log" 2>&1 &
    CHILDREN="$CHILDREN $!"
    wait_for_socket

    export WAYLAND_DISPLAY=$SOCKET
    if [ "$WALLPAPER" != javafx ] && command -v plasmashell > /dev/null; then
        (kde plasmashell --no-respawn) > "$LOGS/plasma.log" 2>&1 &
    else
        (run "$DEMOS/Wallpaper.java") > "$LOGS/wallpaper.log" 2>&1 &
    fi
    CHILDREN="$CHILDREN $!"
    sleep 3
    if [ -n "$SCREENSHOT" ]; then
        echo "--screenshot is only supported with --compositor=gnome" >&2
    fi
}

start_none() {
    if [ -n "$SCREENSHOT" ]; then
        echo "--screenshot is only supported with --compositor=gnome" >&2
    fi
}

PIDS=
start_$COMPOSITOR

for class in $CLASSES; do
    name=$(printf '%s' "${class%Demo}" | tr '[:upper:]' '[:lower:]')
    (run "$(aot_option "$class")" -Dprism.verbose=true -Djavafx.enablePreview=true \
        -Djavafx.suppressPreviewWarning=true $JVM_OPTIONS "$DEMOS/$class.java" 2>&1 \
        | tee "$LOGS/$name.log" | sed -u "s/^/[$name] /") &
    PIDS="$PIDS $!"
done
if [ "$COMPOSITOR" = none ]; then
    echo "Running$CLASSES on the current desktop ($PIPELINE), logs in $LOGS"
else
    echo "Running$CLASSES on $COMPOSITOR ($PIPELINE), logs in $LOGS"
fi
wait $PIDS
