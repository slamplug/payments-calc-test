package transformer;

import static java.time.format.DateTimeFormatter.ofPattern;
import static util.BillingPeriodCalculator.calculateBillingPeriodEndDate;
import static util.BillingPeriodCalculator.calculateBillingPeriodStartDate;
import static util.RoundingHelper.round2dp;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

import api.InvoiceSummary;
import api.Payment;
import com.firstutility.reach.customerservice.response.dto.invoice.LognetInvoice;
import com.firstutility.reach.lognet.invoivcessummary.LognetInvoiceSummary;

public class LognetInvoiceToInvoiceSummaryTransformerImpl {

    private final DateTimeFormatter dtf = ofPattern("yyyy-MM-dd'T'HH:mm:ssZ");

    public InvoiceSummary transform(final LognetInvoice lognetInvoice,
            final LognetInvoiceSummary lognetInvoiceSummary,
            final List<Payment> invoicePayments,
            final BigDecimal previousBalance) {

        final LocalDate billingPeriodEndDate = calculateBillingPeriodEndDate(lognetInvoice, lognetInvoiceSummary);
        final LocalDate billingPeriodStartDate =  calculateBillingPeriodStartDate(lognetInvoice, lognetInvoiceSummary);

        return InvoiceSummary.builder()
                .invoiceNo(lognetInvoice.getInvoiceno())
                .issueDate(LocalDateTime.parse(lognetInvoice.getIssuedate(), dtf).toLocalDate())
                .billingPeriodStartDate(billingPeriodStartDate)
                .billingPeriodEndDate(billingPeriodEndDate)
                .amount(round2dp(lognetInvoice.getAmount()))
                .payments(invoicePayments)
                .previousBalance(previousBalance)
                .build();
    }
}
