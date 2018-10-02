package api;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.firstutility.telco.selfserv.customer.api.CallChargeSummary;
import com.firstutility.telco.selfserv.customer.api.InvoiceItem;
import lombok.Builder;
import lombok.Data;

@Builder
@Data
@JsonPropertyOrder({ "invoiceNo", "issueDate", "billingPeriodStartDate", "billingPeriodEndDate", "amount",
        "invoiceItems", "callChargeSummary" })
public class InvoiceSummary {

    private final String invoiceNo;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
    private final LocalDate issueDate;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
    private final LocalDate billingPeriodStartDate;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
    private final  LocalDate billingPeriodEndDate;

    private final BigDecimal amount;

    private final List<InvoiceItem> invoiceItems;

    private final List<CallChargeSummary> callChargeSummary;

    private final List<Payment> payments;

    private final BigDecimal previousBalance;

    public static class InvoiceSummaryBuilder {

        InvoiceSummaryBuilder withInvoiceItems(final InvoiceItem.InvoiceItemBuilder... builders) {
            List<InvoiceItem> summaryList = new ArrayList<InvoiceItem>();

            for (final InvoiceItem.InvoiceItemBuilder builder: builders) {
                summaryList.add(builder.build());
            }

            this.invoiceItems = summaryList;
            return this;
        }

        InvoiceSummaryBuilder withCallChargeSummary(final CallChargeSummary.CallChargeSummaryBuilder... builders) {
            List<CallChargeSummary> summaryList = new ArrayList<CallChargeSummary>();

            for (final CallChargeSummary.CallChargeSummaryBuilder builder: builders) {
                summaryList.add(builder.build());
            }

            this.callChargeSummary = summaryList;
            return this;
        }

        public InvoiceSummaryBuilder withDefaultValues() {
            // @formatter:off
            return invoiceNo("12345678")
                    .issueDate(LocalDate.parse("2018-02-20"))
                    .billingPeriodStartDate(LocalDate.parse("2018-01-01"))
                    .billingPeriodEndDate(LocalDate.parse("2018-01-31"))
                    .amount(new BigDecimal(10.31).setScale(2, BigDecimal.ROUND_HALF_UP))
                    .withInvoiceItems(
                            InvoiceItem.builder().withDefaultValues(),
                            InvoiceItem.builder().withDefaultValues()
                    )
                    .withCallChargeSummary(
                            CallChargeSummary.builder().withDefaultValues(),
                            CallChargeSummary.builder().withDefaultValues()
                    );
            // @formatter:on
        }
    }
}
