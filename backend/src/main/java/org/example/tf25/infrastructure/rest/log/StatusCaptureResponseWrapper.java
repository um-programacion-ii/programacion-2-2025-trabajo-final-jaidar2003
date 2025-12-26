package org.example.tf25.infrastructure.rest.log;

import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpServletResponseWrapper;

/**
 * Wrapper simple para capturar el status final de la respuesta HTTP.
 */
public class StatusCaptureResponseWrapper extends HttpServletResponseWrapper {
    private int httpStatus = SC_OK;

    public StatusCaptureResponseWrapper(HttpServletResponse response) {
        super(response);
    }

    @Override
    public void setStatus(int sc) {
        super.setStatus(sc);
        this.httpStatus = sc;
    }

    @Override
    public void sendError(int sc) {
        try {
            super.sendError(sc);
        } catch (Exception ignored) {}
        this.httpStatus = sc;
    }

    @Override
    public void sendError(int sc, String msg) {
        try {
            super.sendError(sc, msg);
        } catch (Exception ignored) {}
        this.httpStatus = sc;
    }

    @Override
    public void sendRedirect(String location) {
        try {
            super.sendRedirect(location);
        } catch (Exception ignored) {}
        this.httpStatus = SC_FOUND;
    }

    @Override
    public int getStatus() {
        return httpStatus;
    }
}
