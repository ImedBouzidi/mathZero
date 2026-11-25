package tn.mathzero.resource.service;

public record ResourceProviderDescriptor(
        String provider,
        String status,
        boolean configured,
        String note) {}
