/*
 * Copyright (c) 2026, Thiago Sayão. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  The copyright holder designates
 * this particular file as subject to the "Classpath" exception as provided
 * in the LICENSE file that accompanied this code.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 */
package com.sun.glass.ui.gtk4;

import com.sun.glass.ui.Application;
import com.sun.glass.ui.CommonDialogs.ExtensionFilter;
import com.sun.glass.ui.CommonDialogs.FileChooserResult;
import com.sun.glass.ui.CommonDialogs.Type;
import com.sun.glass.ui.EventLoop;
import com.sun.glass.ui.Window;
import com.sun.glass.ui.gtk4.wrappers.GDBusConnection;
import com.sun.glass.ui.gtk4.wrappers.GErrorException;
import com.sun.glass.ui.gtk4.wrappers.GFile;
import com.sun.glass.ui.gtk4.wrappers.GListModel;
import com.sun.glass.ui.gtk4.wrappers.GListStore;
import com.sun.glass.ui.gtk4.wrappers.GObject;
import com.sun.glass.ui.gtk4.wrappers.GObjectFunctions;
import com.sun.glass.ui.gtk4.wrappers.GVariant;
import com.sun.glass.ui.gtk4.wrappers.GVariantType;
import com.sun.glass.ui.gtk4.wrappers.GdkSurface;
import com.sun.glass.ui.gtk4.wrappers.GdkWaylandToplevel;
import com.sun.glass.ui.gtk4.wrappers.Gio;
import com.sun.glass.ui.gtk4.wrappers.GtkFileDialog;
import com.sun.glass.ui.gtk4.wrappers.GtkFileFilter;
import com.sun.glass.ui.gtk4.wrappers.GtkWidget;
import com.sun.glass.ui.gtk4.wrappers.GtkWindow;

import java.io.File;
import java.lang.foreign.Arena;
import java.lang.foreign.FunctionDescriptor;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.ValueLayout;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * File and folder choosers. They use the FileChooser interface of the XDG
 * desktop portal, so that the dialog of the desktop is shown, and fall back to
 * {@code GtkFileDialog} when no portal is running.
 */
final class Gtk4CommonDialogs {

    private static final String PORTAL = "org.freedesktop.portal.Desktop";
    private static final String PORTAL_PATH = "/org/freedesktop/portal/desktop";
    private static final String FILE_CHOOSER = "org.freedesktop.portal.FileChooser";
    private static final String REQUEST = "org.freedesktop.portal.Request";
    private static final int RESPONSE_SUCCESS = 0;
    private static final int RESPONSE_CANCELLED = 1;
    private static final int GLOB = 0;
    private static final boolean USE_PORTAL = !"false".equals(System.getProperty("glass.gtk4.portal"));

    private static final FunctionDescriptor RESPONSE_CALLBACK = Gtk4SignalHelper.buildDescriptor(void.class,
            MemorySegment.class, MemorySegment.class, MemorySegment.class, MemorySegment.class, MemorySegment.class,
            GVariant.class, MemorySegment.class);
    private static final FunctionDescriptor EXPORTED_CALLBACK =
            FunctionDescriptor.ofVoid(ValueLayout.ADDRESS, ValueLayout.ADDRESS, ValueLayout.ADDRESS);

    private static final AtomicInteger tokens = new AtomicInteger();
    private static GDBusConnection bus;
    private static int portalVersion = -1;

    private Gtk4CommonDialogs() {
    }

    static FileChooserResult showFileChooser(Window owner, String folder, String filename, String title,
                                             int type, boolean multipleMode,
                                             ExtensionFilter[] extensionFilters, int defaultFilterIndex) {
        if (portalVersion() > 0) {
            try {
                return portalFileChooser(owner, folder, filename, title, type, multipleMode,
                        extensionFilters, defaultFilterIndex);
            } catch (RuntimeException e) {
                System.err.println("Gtk4CommonDialogs: file chooser portal failed: " + e.getMessage());
            }
        }
        return gtkFileChooser(owner, folder, filename, title, type, multipleMode, extensionFilters,
                defaultFilterIndex);
    }

    static File showFolderChooser(Window owner, String folder, String title) {
        if (portalVersion() >= 3) {
            try {
                return portalFolderChooser(owner, folder, title);
            } catch (RuntimeException e) {
                System.err.println("Gtk4CommonDialogs: file chooser portal failed: " + e.getMessage());
            }
        }
        return gtkFolderChooser(owner, folder, title);
    }

    private static int portalVersion() {
        if (portalVersion >= 0) {
            return portalVersion;
        }
        portalVersion = 0;
        if (!USE_PORTAL) {
            return 0;
        }
        try {
            bus = Gio.busGetSync(Gio.G_BUS_TYPE_SESSION, null);
            GVariant reply = bus.callSync(PORTAL, PORTAL_PATH, "org.freedesktop.DBus.Properties", "Get",
                    GVariant.createTuple(new GVariant[] {
                        GVariant.createString(FILE_CHOOSER), GVariant.createString("version")
                    }), null, Gio.G_DBUS_CALL_FLAGS_NONE, -1, null);
            GVariant boxed = reply.getChildValue(0);
            GVariant version = boxed.getVariant();
            portalVersion = version.getUint32();
            version.unref();
            boxed.unref();
            reply.unref();
        } catch (RuntimeException e) {
            portalVersion = 0;
        }
        return portalVersion;
    }

    private static FileChooserResult portalFileChooser(Window owner, String folder, String filename, String title,
                                                       int type, boolean multipleMode,
                                                       ExtensionFilter[] extensionFilters, int defaultFilterIndex) {
        boolean save = type == Type.SAVE;
        List<GVariant> options = new ArrayList<>();
        options.add(entry("modal", GVariant.createBoolean_(true)));
        if (!save && multipleMode) {
            options.add(entry("multiple", GVariant.createBoolean_(true)));
        }
        if (save && filename != null && !filename.isEmpty()) {
            options.add(entry("current_name", GVariant.createString(filename)));
        }
        if (extensionFilters != null && extensionFilters.length > 0) {
            GVariant[] filters = new GVariant[extensionFilters.length];
            for (int i = 0; i < filters.length; i++) {
                filters[i] = filter(extensionFilters[i]);
            }
            options.add(entry("filters", array("(sa(us))", filters)));
            if (defaultFilterIndex >= 0 && defaultFilterIndex < extensionFilters.length) {
                options.add(entry("current_filter", filter(extensionFilters[defaultFilterIndex])));
            }
        }

        Response response = request(save ? "SaveFile" : "OpenFile", owner, title, folder, options);
        List<File> files = new ArrayList<>();
        ExtensionFilter selected = null;
        if (response.results != null) {
            try {
                addUris(files, response.results);
                String filterName = currentFilterName(response.results);
                if (filterName != null && extensionFilters != null) {
                    for (ExtensionFilter f : extensionFilters) {
                        if (filterName.equals(f.getDescription())) {
                            selected = f;
                            break;
                        }
                    }
                }
            } finally {
                response.results.unref();
            }
        }
        return new FileChooserResult(files, selected);
    }

    private static File portalFolderChooser(Window owner, String folder, String title) {
        List<GVariant> options = new ArrayList<>();
        options.add(entry("modal", GVariant.createBoolean_(true)));
        options.add(entry("directory", GVariant.createBoolean_(true)));
        Response response = request("OpenFile", owner, title, folder, options);
        if (response.results == null) {
            return null;
        }
        try {
            List<File> files = new ArrayList<>();
            addUris(files, response.results);
            return files.isEmpty() ? null : files.getFirst();
        } finally {
            response.results.unref();
        }
    }

    private static GVariant entry(String key, GVariant value) {
        return GVariant.createDictEntry(GVariant.createString(key), GVariant.createVariant(value));
    }

    private static GVariant array(String elementType, GVariant[] children) {
        GVariantType type = GVariantType.create(elementType);
        try {
            return GVariant.createArray(type, children);
        } finally {
            type.free();
        }
    }

    private static GVariant filter(ExtensionFilter extensionFilter) {
        List<String> extensions = extensionFilter.getExtensions();
        GVariant[] patterns = new GVariant[extensions.size()];
        for (int i = 0; i < patterns.length; i++) {
            patterns[i] = GVariant.createTuple(new GVariant[] {
                GVariant.createUint32(GLOB), GVariant.createString(extensions.get(i))
            });
        }
        return GVariant.createTuple(new GVariant[] {
            GVariant.createString(extensionFilter.getDescription()), array("(us)", patterns)
        });
    }

    private static void addUris(List<File> files, GVariant results) {
        GVariantType type = GVariantType.create("as");
        GVariant uris = results.lookupValue("uris", type);
        type.free();
        if (uris == null || uris.isNull()) {
            return;
        }
        try {
            for (String uri : uris.getStrv(null)) {
                try {
                    URI parsed = URI.create(uri);
                    if ("file".equals(parsed.getScheme())) {
                        files.add(new File(parsed));
                    }
                } catch (IllegalArgumentException e) {
                    System.err.println("Gtk4CommonDialogs: ignoring " + uri + ": " + e.getMessage());
                }
            }
        } finally {
            uris.unref();
        }
    }

    private static String currentFilterName(GVariant results) {
        GVariantType type = GVariantType.create("(sa(us))");
        GVariant filter = results.lookupValue("current_filter", type);
        type.free();
        if (filter == null || filter.isNull()) {
            return null;
        }
        try {
            GVariant name = filter.getChildValue(0);
            String value = name.getString(null);
            name.unref();
            return value;
        } finally {
            filter.unref();
        }
    }

    private record Response(int code, GVariant results) {
    }

    private static Response request(String method, Window owner, String title, String folder,
                                    List<GVariant> options) {
        String token = "jfx_" + ProcessHandle.current().pid() + "_" + tokens.incrementAndGet();
        String sender = bus.getUniqueName().substring(1).replace('.', '_');
        String expected = PORTAL_PATH + "/request/" + sender + "/" + token;
        options.add(entry("handle_token", GVariant.createString(token)));

        try (Arena arena = Arena.ofConfined()) {
            if (folder != null && !folder.isEmpty()) {
                options.add(entry("current_folder", GVariant.createBytestring(arena.allocateFrom(folder))));
            }
            Waiter waiter = new Waiter();
            MemorySegment callback = Gtk4SignalHelper.upcall(MethodHandles.lookup(),
                    MethodHandles.lookup().bind(waiter, "response", MethodType.methodType(void.class,
                            MemorySegment.class, MemorySegment.class, MemorySegment.class, MemorySegment.class,
                            MemorySegment.class, GVariant.class, MemorySegment.class)),
                    RESPONSE_CALLBACK, arena);
            int subscription = subscribe(expected, callback);
            Exported parent = exportParent(owner, arena);
            try {
                GVariant reply = bus.callSync(PORTAL, PORTAL_PATH, FILE_CHOOSER, method,
                        GVariant.createTuple(new GVariant[] {
                            GVariant.createString(parent.identifier()),
                            GVariant.createString(title != null ? title : ""),
                            array("{sv}", options.toArray(new GVariant[0]))
                        }), null, Gio.G_DBUS_CALL_FLAGS_NONE, -1, null);
                GVariant handle = reply.getChildValue(0);
                String path = handle.getString(null);
                handle.unref();
                reply.unref();
                if (!expected.equals(path)) {
                    bus.signalUnsubscribe(subscription);
                    subscription = subscribe(path, callback);
                }
                waiter.await();
            } finally {
                bus.signalUnsubscribe(subscription);
                parent.drop();
            }
            if (waiter.code == RESPONSE_SUCCESS) {
                return new Response(waiter.code, waiter.results);
            }
            if (waiter.results != null) {
                waiter.results.unref();
            }
            if (waiter.code != RESPONSE_CANCELLED) {
                throw new IllegalStateException(method + " returned " + waiter.code);
            }
            return new Response(waiter.code, null);
        } catch (NoSuchMethodException | IllegalAccessException e) {
            throw new IllegalStateException(e);
        }
    }

    private static int subscribe(String path, MemorySegment callback) {
        return bus.signalSubscribe(PORTAL, REQUEST, "Response", path, null,
                Gio.G_DBUS_SIGNAL_FLAGS_NO_MATCH_RULE, callback, null, null);
    }

    private static final class Waiter {
        private int code = -1;
        private GVariant results;
        private boolean done;
        private EventLoop loop;

        void await() {
            if (!done) {
                loop = Application.GetApplication().createEventLoop();
                loop.enter();
            }
        }

        void finish() {
            done = true;
            if (loop != null) {
                loop.leave(null);
            }
        }

        @SuppressWarnings("unused")
        void response(MemorySegment connection, MemorySegment sender, MemorySegment path,
                      MemorySegment iface, MemorySegment signal, GVariant parameters, MemorySegment data) {
            if (done) {
                return;
            }
            GVariant value = parameters.getChildValue(0);
            code = value.getUint32();
            value.unref();
            results = parameters.getChildValue(1);
            finish();
        }

        @SuppressWarnings("unused")
        void exported(MemorySegment toplevel, MemorySegment handle, MemorySegment data) {
            if (handle.address() != 0) {
                exportedHandle = handle.reinterpret(Long.MAX_VALUE).getString(0);
            }
            finish();
        }

        private String exportedHandle;
    }

    private record Exported(GdkWaylandToplevel toplevel, String handle) {
        String identifier() {
            return handle != null ? "wayland:" + handle : "";
        }

        void drop() {
            if (handle != null) {
                toplevel.dropExportedHandle(handle);
            }
        }
    }

    private static Exported exportParent(Window owner, Arena arena)
            throws NoSuchMethodException, IllegalAccessException {
        if (!(owner instanceof Gtk4Window window) || !GdkWaylandToplevel.isAvailable()) {
            return new Exported(null, null);
        }
        GdkSurface surface = window.getSurface();
        if (surface == null || surface.isNull() || !GObjectFunctions.typeIsA(typeOf(surface.getPointer()),
                GdkWaylandToplevel.getGType())) {
            return new Exported(null, null);
        }
        GdkWaylandToplevel toplevel = surface.cast(GdkWaylandToplevel::new);
        Waiter waiter = new Waiter();
        MemorySegment callback = Gtk4SignalHelper.upcall(MethodHandles.lookup(),
                MethodHandles.lookup().bind(waiter, "exported", MethodType.methodType(void.class,
                        MemorySegment.class, MemorySegment.class, MemorySegment.class)),
                EXPORTED_CALLBACK, arena);
        if (!toplevel.exportHandle(callback, null, null)) {
            return new Exported(null, null);
        }
        waiter.await();
        return new Exported(toplevel, waiter.exportedHandle);
    }

    private static long typeOf(MemorySegment instance) {
        MemorySegment klass = instance.reinterpret(ValueLayout.ADDRESS.byteSize()).get(ValueLayout.ADDRESS, 0);
        return klass.reinterpret(Long.BYTES).get(ValueLayout.JAVA_LONG, 0);
    }

    private static FileChooserResult gtkFileChooser(Window owner, String folder, String filename, String title,
                                                    int type, boolean multipleMode,
                                                    ExtensionFilter[] extensionFilters, int defaultFilterIndex) {
        GtkFileDialog dialog = GtkFileDialog.create();
        try {
            configure(dialog, title, folder);
            if (type == Type.SAVE && filename != null && !filename.isEmpty()) {
                dialog.setInitialName(filename);
            }

            List<GtkFileFilter> filters = new ArrayList<>();
            if (extensionFilters != null && extensionFilters.length > 0) {
                GListStore store = GListStore.create(GtkFileFilter.getGType());
                for (ExtensionFilter extensionFilter : extensionFilters) {
                    GtkFileFilter filter = GtkFileFilter.create();
                    filter.setName(extensionFilter.getDescription());
                    for (String extension : extensionFilter.getExtensions()) {
                        filter.addPattern(extension);
                    }
                    store.append(filter);
                    filters.add(filter);
                }
                dialog.setFilters(store);
                if (defaultFilterIndex >= 0 && defaultFilterIndex < filters.size()) {
                    dialog.setDefaultFilter(filters.get(defaultFilterIndex));
                }
                store.unref();
            }

            GtkWindow parent = parentOf(owner);
            List<File> files = new ArrayList<>();
            try {
                if (type == Type.SAVE) {
                    GFile file = Gtk4Async.await(cb -> dialog.save(parent, null, cb, null), dialog::saveFinish);
                    addFile(files, file);
                } else if (multipleMode) {
                    GListModel list = Gtk4Async.await(cb -> dialog.openMultiple(parent, null, cb, null),
                            dialog::openMultipleFinish);
                    addFiles(files, list);
                } else {
                    GFile file = Gtk4Async.await(cb -> dialog.open(parent, null, cb, null), dialog::openFinish);
                    addFile(files, file);
                }
            } catch (GErrorException e) {
                if (!e.isCancelled()) {
                    System.err.println("Gtk4CommonDialogs: " + e.getMessage());
                }
            }

            ExtensionFilter selected = null;
            GtkFileFilter selectedFilter = dialog.getDefaultFilter();
            for (int i = 0; i < filters.size(); i++) {
                if (filters.get(i).equals(selectedFilter)) {
                    selected = extensionFilters[i];
                }
            }
            for (GtkFileFilter filter : filters) {
                filter.unref();
            }
            return new FileChooserResult(files, selected);
        } finally {
            dialog.unref();
        }
    }

    private static File gtkFolderChooser(Window owner, String folder, String title) {
        GtkFileDialog dialog = GtkFileDialog.create();
        try {
            configure(dialog, title, folder);
            GtkWindow parent = parentOf(owner);
            GFile file = Gtk4Async.await(cb -> dialog.selectFolder(parent, null, cb, null),
                    dialog::selectFolderFinish);
            List<File> files = new ArrayList<>();
            addFile(files, file);
            return files.isEmpty() ? null : files.getFirst();
        } catch (GErrorException e) {
            if (!e.isCancelled()) {
                System.err.println("Gtk4CommonDialogs: " + e.getMessage());
            }
            return null;
        } finally {
            dialog.unref();
        }
    }

    private static void configure(GtkFileDialog dialog, String title, String folder) {
        dialog.setModal(true);
        if (title != null) {
            dialog.setTitle(title);
        }
        if (folder != null && !folder.isEmpty()) {
            GFile initialFolder = GFile.newForPath(folder);
            dialog.setInitialFolder(initialFolder);
            initialFolder.cast(GObject::new).unref();
        }
    }

    private static GtkWindow parentOf(Window owner) {
        if (owner instanceof Gtk4Window window) {
            GtkWidget keyWidget = window.getKeyWidget();
            return keyWidget != null ? keyWidget.cast(GtkWindow::new) : null;
        }
        return null;
    }

    private static void addFile(List<File> files, GFile file) {
        if (file == null || file.isNull()) {
            return;
        }
        String path = file.getPath();
        if (path != null && !path.isEmpty()) {
            files.add(new File(path));
        }
        file.cast(GObject::new).unref();
    }

    private static void addFiles(List<File> files, GListModel list) {
        if (list == null || list.isNull()) {
            return;
        }
        int count = list.getNItems();
        for (int i = 0; i < count; i++) {
            addFile(files, list.getObject(i).cast(GFile::wrap));
        }
        list.cast(GObject::new).unref();
    }
}
