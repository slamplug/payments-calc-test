package util;

import static java.time.format.DateTimeFormatter.ofPattern;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Date;

import com.firstutility.reach.customerservice.response.dto.invoice.LognetInvoice;
import com.firstutility.reach.lognet.invoivcessummary.LognetInvoiceSummary;

public class BillingPeriodCalculator {

    private static final DateTimeFormatter dtf = ofPattern("yyyy-MM-dd'T'HH:mm:ssZ");

    public static LocalDate calculateBillingPeriodEndDate(final LognetInvoice lognetInvoice, final LognetInvoiceSummary lognetInvoiceSummary) {
        return lognetInvoiceSummary != null ?
            convertToLocalDate(lognetInvoiceSummary.getBillperiodenddate()) :
            calculateBillingPeriodEndDate(lognetInvoice);
    }

    private static LocalDate calculateBillingPeriodEndDate(final LognetInvoice lognetInvoice) {
        return convertToLocalDateTime(lognetInvoice.getStartdate()).toLocalDate();
    }

    public static LocalDate calculateBillingPeriodStartDate(final LognetInvoice lognetInvoice, final LognetInvoiceSummary lognetInvoiceSummary) {
        return lognetInvoiceSummary != null ?
            convertToLocalDate(lognetInvoiceSummary.getBillperiodstartdate()) :
            calculateBillingPeriodStartDate(calculateBillingPeriodEndDate(lognetInvoice));
    }

    private static LocalDate calculateBillingPeriodStartDate(final LocalDate billingPeriodEndDate) {
        return isLastDayOfMonth(billingPeriodEndDate) ?
                billingPeriodEndDate.withDayOfMonth(1) :
                billingPeriodEndDate.minusMonths(1).plusDays(1);
    }

    private static boolean isLastDayOfMonth(final LocalDate localDate) {
        return localDate.getDayOfMonth() == localDate.withDayOfMonth(localDate.lengthOfMonth()).getDayOfMonth();
    }

    private static LocalDate convertToLocalDate(final Date input) {
        return input.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
    }

    public static LocalDateTime convertToLocalDateTime(final String date) {
        return LocalDateTime.parse(date, dtf);
    }
}
