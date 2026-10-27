package com.ebv12.backend.service;

public class NoAvailabilityException extends RuntimeException {
    public NoAvailabilityException() {
        super("No hay turnos disponibles para la fecha seleccionada");
    }
}
