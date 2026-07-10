package com.milly.table.application.port.outbound;

public interface QrCodeGenerator {

    byte[] generatePngBytes(String content);
}
