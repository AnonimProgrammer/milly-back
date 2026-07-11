package com.milly.billing.infrastructure.adapter.outbound.receipt;

import com.milly.billing.application.port.outbound.PaymentReceiptGenerator;
import com.milly.billing.domain.entity.PaymentEntity;
import com.milly.billing.domain.model.GeneratedReceipt;
import com.milly.billing.domain.valueobject.PaymentProvider;
import org.openpdf.text.Document;
import org.openpdf.text.DocumentException;
import org.openpdf.text.Element;
import org.openpdf.text.Font;
import org.openpdf.text.FontFactory;
import org.openpdf.text.Paragraph;
import org.openpdf.text.Phrase;
import org.openpdf.text.Rectangle;
import org.openpdf.text.pdf.PdfPCell;
import org.openpdf.text.pdf.PdfPTable;
import org.openpdf.text.pdf.PdfWriter;
import org.springframework.stereotype.Component;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

@Component
public class OpenPdfPaymentReceiptGenerator implements PaymentReceiptGenerator {

    private static final String MIME_TYPE = "application/pdf";
    private static final String FILE_EXTENSION = "pdf";

    private static final DateTimeFormatter DATE_TIME_FORMATTER =
            DateTimeFormatter.ofPattern("dd MMM yyyy, HH:mm");

    @Override
    public GeneratedReceipt generate(PaymentEntity payment) {
        try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            Document document = new Document();
            PdfWriter.getInstance(document, outputStream);
            document.open();

            Font headerFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 20);
            Font bodyFont = FontFactory.getFont(FontFactory.HELVETICA, 12);

            document.add(buildHeader(headerFont));
            addBlankLines(document, 2);

            document.add(buildDetailTable(bodyFont,
                    row("Payment", lastEight(payment.getId())),
                    row("Order", lastEight(payment.getOrderId())),
                    row("Provider", formatProvider(payment.getProvider())),
                    row("Date & Time", payment.getCreatedAt().format(DATE_TIME_FORMATTER)),
                    row("Reference", payment.getProviderReference())));

            addBlankLines(document, 1);

            document.add(buildDetailTable(bodyFont,
                    row("Paid Amount", formatMoney(payment.getAmount().amount())),
                    row("Tip Amount", formatMoney(payment.getTipAmount().amount()))));

            document.close();
            return new GeneratedReceipt(outputStream.toByteArray(), MIME_TYPE, FILE_EXTENSION);
        } catch (DocumentException | IOException exception) {
            throw new IllegalStateException("Failed to generate payment receipt.", exception);
        }
    }

    private PdfPTable buildHeader(Font headerFont) {
        PdfPTable header = new PdfPTable(2);
        header.setWidthPercentage(100);
        header.setWidths(new float[]{1f, 1f});
        header.addCell(headerCell("Receipt", headerFont, Element.ALIGN_LEFT));
        header.addCell(headerCell("milly.", headerFont, Element.ALIGN_RIGHT));
        return header;
    }

    private PdfPTable buildDetailTable(Font bodyFont, String[]... rows) {
        PdfPTable table = new PdfPTable(2);
        table.setWidthPercentage(100);
        table.setWidths(new float[]{1.2f, 1f});

        for (String[] row : rows) {
            table.addCell(detailCell(row[0], bodyFont, Element.ALIGN_LEFT));
            table.addCell(detailCell(row[1], bodyFont, Element.ALIGN_RIGHT));
        }

        return table;
    }

    private PdfPCell headerCell(String text, Font font, int alignment) {
        PdfPCell cell = new PdfPCell(new Phrase(text, font));
        cell.setBorder(Rectangle.NO_BORDER);
        cell.setHorizontalAlignment(alignment);
        cell.setPadding(0f);
        cell.setPaddingBottom(2f);
        return cell;
    }

    private PdfPCell detailCell(String text, Font font, int alignment) {
        PdfPCell cell = new PdfPCell(new Phrase(text, font));
        cell.setBorder(Rectangle.NO_BORDER);
        cell.setHorizontalAlignment(alignment);
        cell.setPadding(0f);
        cell.setPaddingBottom(6f);
        return cell;
    }

    private void addBlankLines(Document document, int count) throws DocumentException {
        for (int index = 0; index < count; index++) {
            Paragraph blankLine = new Paragraph(" ");
            blankLine.setSpacingAfter(10f);
            document.add(blankLine);
        }
    }

    private String[] row(String label, String value) {
        return new String[]{label, value};
    }

    private String lastEight(UUID uuid) {
        String compact = uuid.toString().replace("-", "");
        return compact.substring(compact.length() - 8).toLowerCase();
    }

    private String formatProvider(PaymentProvider provider) {
        return switch (provider) {
            case CARD -> "card";
            case APPLE -> "apple pay";
            case GOOGLE -> "google pay";
        };
    }

    private String formatMoney(BigDecimal amount) {
        return amount.setScale(2).toPlainString() + " ₼";
    }
}
