package org.cef.browser;

import java.awt.Component;
import java.awt.event.InputEvent;
import java.awt.event.MouseEvent;

/** JCEF host input for the standard website fullscreen button; never resizes the Swing app. */
public final class AppFullscreenInput {
    private AppFullscreenInput() {}

    public static void press(CefBrowser browser, int x, int y) {
        Component view = browser.getUIComponent();
        if (x <= 0 || y <= 0 || x >= view.getWidth() || y >= view.getHeight()) return;
        if (browser instanceof CefBrowser_N host) {
            long now = System.currentTimeMillis();
            host.sendMouseEvent(
                    new MouseEvent(view, MouseEvent.MOUSE_MOVED, now, 0, x, y, 0, false, 0));
            host.sendMouseEvent(
                    new MouseEvent(
                            view,
                            MouseEvent.MOUSE_PRESSED,
                            now,
                            InputEvent.BUTTON1_DOWN_MASK,
                            x,
                            y,
                            1,
                            false,
                            MouseEvent.BUTTON1));
            host.sendMouseEvent(
                    new MouseEvent(
                            view,
                            MouseEvent.MOUSE_RELEASED,
                            now,
                            0,
                            x,
                            y,
                            1,
                            false,
                            MouseEvent.BUTTON1));
        }
    }
}
