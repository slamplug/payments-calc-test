import static data.JsonFileLoader.loadPreviousPayments;
import static data.XmlFileLoader.loadInvoiceSummaries;
import static java.util.Collections.emptyList;
import static java.util.Comparator.comparing;
import static java.util.Objects.isNull;
import static java.util.stream.Collectors.toList;
import static util.BillingPeriodCalculator.convertToLocalDateTime;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import api.InvoiceSummary;
import api.Payment;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.firstutility.java.result.Result;
import com.firstutility.reach.customerservice.response.dto.invoice.LognetInvoice;
import com.firstutility.reach.lognet.invoivcessummary.LognetInvoiceSummary;
import data.XmlFileLoader;
import javafx.util.Pair;
import org.springframework.util.StringUtils;
import transformer.LognetInvoiceToInvoiceSummaryTransformerImpl;

public class PaymentCalculator {

    private LognetInvoiceToInvoiceSummaryTransformerImpl transformer = new LognetInvoiceToInvoiceSummaryTransformerImpl();

    private final ObjectMapper mapper = new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false)
            .enable(SerializationFeature.INDENT_OUTPUT);

    private Pair<InvoiceSummary, BigDecimal> buildInvoiceSummary(
            final LognetInvoice lognetInvoice,
            final Result<List<LognetInvoiceSummary>, String> invoicesSummaryResult,
            final LognetInvoice nextInvoice,
            final BigDecimal previousBalance,
            final Result<List<Payment>, String> paymentsResult) {

        //System.out.println(
        //        " InvoiceNo  :" +  lognetInvoice.getInvoiceno() +
        //        " IssueDate  :" +  lognetInvoice.getIssuedate() +
        //        ", PreviousNo : " + (!isNull(previousInvoice) ? previousInvoice.getInvoiceno() : "        ") +
        //        ", NextNo     : " + (!isNull(nextInvoice) ? nextInvoice.getInvoiceno() : "        "));

        final List<Payment> invoicePayments = getInvoicePayments(lognetInvoice.getIssuedate(),
                !isNull(nextInvoice) ? nextInvoice.getIssuedate() : null,
                paymentsResult);

        final BigDecimal paymentsTotal = invoicePayments
                .stream()
                .map(Payment::getTransactionAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        //System.out.println(invoicePayments.stream()
        //        .map(p -> p.getId() + "|" +p.getTransactionDate()+ "|" +p.getTransactionAmount() )
        //        .collect(toList()));

        //System.out.println("payment total:" + paymentsTotal);

        final BigDecimal invoiceBalance = previousBalance
                .add(paymentsTotal)
                .subtract(new BigDecimal(lognetInvoice.getAmount()));

        //System.out.println("balance BF:" + invoiceBalance);

        //@formatter:off
        final LognetInvoiceSummary matchingInvoiceSummary = invoicesSummaryResult
                .toOptional()
                .map(summaries -> summaries
                    .stream()
                    .filter(summary -> summary.getInvoiceno().equals(lognetInvoice.getInvoiceno()))
                    .findFirst().orElse(null))
                .orElse(null);
        //@formatter:off

        return new Pair<>(transformer.transform(lognetInvoice, matchingInvoiceSummary, invoicePayments, invoiceBalance), invoiceBalance);
    }

    private List<Payment> getInvoicePayments(final String invoiceIssueDate, final String nextIssueDate,
            final Result<List<Payment>, String> paymentsResult) {
        //@formatter:off
        return paymentsResult
                .toOptional()
                .map(payments -> payments
                    .stream()
                    .filter(payment -> paymentInIssuePeriod(payment, invoiceIssueDate, nextIssueDate))
                    .collect(toList()))
                .orElse(emptyList());
        //@formatter:off
    }

    private boolean paymentInIssuePeriod(final Payment payment, final String issueDate, final String nextIssueDate) {
        return payment.getTransactionDate().isAfter(convertToLocalDateTime(issueDate))
                && (StringUtils.isEmpty(nextIssueDate) || payment.getTransactionDate().isBefore(convertToLocalDateTime(nextIssueDate)));
    }

    private List<InvoiceSummary> buildInvoiceSummaries(
            final Result<List<LognetInvoice>, String> invoicesResult,
            final Result<List<LognetInvoiceSummary>, String> invoicesSummaryResult,
            final BigDecimal balance,
            final Result<List<Payment>, String> paymentsResult) {

        return invoicesResult.toOptional().map(r -> {

            final List<LognetInvoice> sortedList = r.stream()
                    .sorted(comparing(LognetInvoice::getIssuedate).reversed())
                    .collect(toList());

            // can't use stream as need result of last iteration to feed current
            final List<InvoiceSummary> invoiceSummaries = new ArrayList<>();
            BigDecimal balanceBroughtForward = balance;
            for (final LognetInvoice lognetInvoice : sortedList) {
                final Pair<InvoiceSummary,BigDecimal> response = buildInvoiceSummary(
                        lognetInvoice,
                        invoicesSummaryResult,
                        getNextInvoiceChronologically(convertToLocalDateTime(lognetInvoice.getIssuedate()), sortedList),
                        balanceBroughtForward,
                        paymentsResult);
                invoiceSummaries.add(response.getKey());
                balanceBroughtForward = response.getValue();
            }

            return invoiceSummaries;
        }).orElse(null);
    }

    private LognetInvoice getNextInvoiceChronologically(final LocalDateTime issueDate, final List<LognetInvoice> sortedList) {
        return sortedList.stream()
                .filter(l -> convertToLocalDateTime(l.getIssuedate()).isAfter(issueDate))
                .min(comparing(LognetInvoice::getIssuedate))
                .orElse(null);
    }

    private void calculate() throws Exception {
        final Result<List<LognetInvoice>, String> invoicesResult = Result.success(XmlFileLoader.loadInvoices());
        final Result<List<LognetInvoiceSummary>, String> invoicesSummaryResult = Result.success(loadInvoiceSummaries());

        final Result<List<Payment>, String> paymentsResult = Result.success(loadPreviousPayments());
        final BigDecimal balance = BigDecimal.valueOf(71.77);

        final List<InvoiceSummary> invoiceSummaries = buildInvoiceSummaries(
                invoicesResult, invoicesSummaryResult, balance, paymentsResult);

        final String result = mapper.writeValueAsString(invoiceSummaries);

        System.out.println(result);
    }

    public static void main(String... args) {
        try {
            new PaymentCalculator().calculate();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
