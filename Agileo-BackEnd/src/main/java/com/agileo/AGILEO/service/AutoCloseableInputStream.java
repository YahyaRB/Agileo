package com.agileo.AGILEO.service;

import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;

public class AutoCloseableInputStream extends FilterInputStream {
    private final Runnable onClose;

    public AutoCloseableInputStream(InputStream in, Runnable onClose) {
        super(in);
        this.onClose = onClose;
    }

    @Override
    public void close() throws IOException {
        try { super.close(); } finally { if (onClose != null) onClose.run(); }
    }
}