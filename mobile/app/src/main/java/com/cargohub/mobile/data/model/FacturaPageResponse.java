package com.cargohub.mobile.data.model;

import java.util.List;

public class FacturaPageResponse {
    private List<Factura> content;
    private int totalPages;
    private long totalElements;
    private int number;
    private int size;

    public List<Factura> getContent() { return content; }
    public int getTotalPages() { return totalPages; }
    public long getTotalElements() { return totalElements; }
    public int getNumber() { return number; }
    public int getSize() { return size; }
}
