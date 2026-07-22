package com.migracion.marketplace.associate.dto;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

/**
 * Reporte de ventas del asociado. Cifras de órdenes COMPLETED dentro del rango pedido
 * (sin rango = todo el histórico). El stock total y los items sin ventas son en tiempo real.
 */
public record AssociateSalesReportResponse(
        BigDecimal totalRevenue,
        long completedOrdersCount,
        ItemSales bestSeller,
        ItemSales worstSeller,
        long totalStock,
        List<ItemRef> zeroSalesItems) {

    public record ItemSales(UUID itemId, String title, long unitsSold) {
    }

    public record ItemRef(UUID itemId, String title) {
    }
}
