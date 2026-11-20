package com.silva.edu.finances.domain.enums;

/**
 * Tipo de movimento do lançamento: decide se ele entra no saldo de caixa e/ou nos relatórios.
 * NORMAL e CARD entram nos relatórios; NORMAL, TRANSFER e INVOICE_PAYMENT movem o caixa.
 */
public enum TransactionEntryType {
    NORMAL,
    CARD,
    TRANSFER,
    INVOICE_PAYMENT
}
