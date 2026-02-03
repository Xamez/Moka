# Moka

Moka is a tauri-like framework for building desktop applications using modern web technologies.
It leverages Java for the backend and SWT for rendering the frontend.

> ~60Mb in native build size (for a 1Mb build size web app)

## TODO

- [x] Fix render pages on native builds (``java.lang.NoClassDefFoundError: Could not initialize class io.vertx.core.buffer.impl.VertxByteBufAllocator``)
- [x] Fix Vert.x event loop blocking for a built app using jvm
- [x] Cleanup `jni-config.json` and `reflect-config.json` files
- [x] Add logging
- [x] Add support for native notifications
- [ ] Add support for system tray
- [ ] Test on MacOS and Linux (should not work since jni-config only has windows libs for now)
- [ ] Add documentation