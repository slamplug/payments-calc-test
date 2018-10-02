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
            final LognetInvoice currentInvoice,
            final LognetInvoice prevInvoice,
            final LognetInvoice nextInvoice,
            final BigDecimal balanceBroughtForward,
            final Result<List<LognetInvoiceSummary>, String> invoicesSummaryResult,
            final Result<List<Payment>, String> paymentsResult) {

        /*System.out.println(
                "PreviousNo : " + (!isNull(prevInvoice) ? prevInvoice.getInvoiceno() : "        ") +
                ", PreviousIssueDate : " + (!isNull(prevInvoice) ? prevInvoice.getIssuedate() : "                        ") +
                ", InvoiceNo  :" +  lognetInvoice.getInvoiceno() +
                ", IssueDate  :" +  lognetInvoice.getIssuedate() +
                ", NextNo : " + (!isNull(nextInvoice) ? nextInvoice.getInvoiceno() : "        ") +
                ", NextIssueDate : " + (!isNull(nextInvoice) ? nextInvoice.getIssuedate() : ""));*/

        final List<Payment> currInvoicePayments = getPaymentsBetweenDates(getIssueDateOrNull(prevInvoice),
                getIssueDateOrNull(currentInvoice), paymentsResult);

        final List<Payment> nextInvoicePayments = getPaymentsBetweenDates(getIssueDateOrNull(currentInvoice),
                getIssueDateOrNull(nextInvoice), paymentsResult);

        final BigDecimal paymentsTotal = nextInvoicePayments.stream()
                .map(Payment::getTransactionAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        /*System.out.println(nextInvoicePayments.stream()
                .map(p -> p.getId() + "|" +p.getTransactionDate()+ "|" +p.getTransactionAmount() )
                .collect(toList()));

        System.out.println("payment total:" + paymentsTotal);*/

        final BigDecimal invoiceBalance = balanceBroughtForward.add(paymentsTotal)
                .subtract(new BigDecimal(currentInvoice.getAmount()));

        //System.out.println("balance BF:" + invoiceBalance);

        //@formatter:off
        final LognetInvoiceSummary matchingInvoiceSummary = invoicesSummaryResult
                .toOptional()
                .map(summaries -> summaries
                    .stream()
                    .filter(summary -> summary.getInvoiceno().equals(currentInvoice.getInvoiceno()))
                    .findFirst().orElse(null))
                .orElse(null);
        //@formatter:off

        return new Pair<>(transformer.transform(
                currentInvoice, matchingInvoiceSummary, currInvoicePayments, invoiceBalance), invoiceBalance);
    }

    private String getIssueDateOrNull(final LognetInvoice lognetInvoice) {
        return !isNull(lognetInvoice) ? lognetInvoice.getIssuedate() : null;
    }

    private List<Payment> getPaymentsBetweenDates(final String dateFrom, final String dateTo, final Result<List<Payment>, String> paymentsResult) {
        //@formatter:off
        return paymentsResult
                .toOptional()
                .map(payments -> payments
                    .stream()
                    .filter(payment -> paymentInIssuePeriod(payment, dateFrom, dateTo))
                    .collect(toList()))
                .orElse(emptyList());
        //@formatter:off
    }

    private boolean paymentInIssuePeriod(final Payment payment, final String dateFrom, final String dateTo) {
        if (StringUtils.isEmpty(dateFrom)) {
            return payment.getTransactionDate().isBefore(convertToLocalDateTime(dateTo));
        } else if (StringUtils.isEmpty(dateTo)) {
            return payment.getTransactionDate().isAfter(convertToLocalDateTime(dateFrom));
        } else {
            return payment.getTransactionDate().isAfter(convertToLocalDateTime(dateFrom))
                    && payment.getTransactionDate().isBefore(convertToLocalDateTime(dateTo));
        }
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
                        getPreviousInvoiceChronologically(convertToLocalDateTime(lognetInvoice.getIssuedate()), sortedList),
                        getNextInvoiceChronologically(convertToLocalDateTime(lognetInvoice.getIssuedate()), sortedList),
                        balanceBroughtForward,
                        invoicesSummaryResult,
                        paymentsResult);
                invoiceSummaries.add(response.getKey());
                balanceBroughtForward = response.getValue();
            }

            return invoiceSummaries;
        }).orElse(null);
    }

    private LognetInvoice getPreviousInvoiceChronologically(final LocalDateTime issueDate, final List<LognetInvoice> sortedList) {
        return sortedList.stream()
                .filter(l -> convertToLocalDateTime(l.getIssuedate()).isBefore(issueDate))
                .max(comparing(LognetInvoice::getIssuedate))
                .orElse(null);
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
