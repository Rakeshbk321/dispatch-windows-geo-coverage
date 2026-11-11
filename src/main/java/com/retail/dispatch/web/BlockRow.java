package com.retail.dispatch.web;

public record BlockRow(long timeBlockId, String label, String price, boolean soldOut) {
}
