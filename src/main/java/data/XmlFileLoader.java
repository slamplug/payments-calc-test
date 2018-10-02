package data;

import java.io.File;
import java.util.Collections;
import java.util.List;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;

import com.firstutility.reach.customerservice.response.dto.invoice.LognetInvoice;
import com.firstutility.reach.customerservice.response.update.dto.Response;
import com.firstutility.reach.lognet.invoivcessummary.LognetInvoiceSummary;
import com.firstutility.reach.lognet.invoivcessummary.LognetInvoicesSummaryResponse;

public class XmlFileLoader {

    public static List<LognetInvoice> loadInvoices() {
        try {
            final ClassLoader classLoader = XmlFileLoader.class.getClassLoader();
            final File file = new File(classLoader.getResource("invoices.xml").getFile());

            final JAXBContext jaxbContext = JAXBContext.newInstance(Response.class);

            final Unmarshaller jaxbUnmarshaller = jaxbContext.createUnmarshaller();

            final Response response = (Response) jaxbUnmarshaller.unmarshal(file);

            return response.getOk().getInvoices();

        } catch (final JAXBException e) {
            e.printStackTrace();
            return Collections.emptyList();
        }
    }

    public static  List<LognetInvoiceSummary> loadInvoiceSummaries() throws Exception {
        try {
            final ClassLoader classLoader = XmlFileLoader.class.getClassLoader();
            final File file = new File(classLoader.getResource("invoice_summary.xml").getFile());

            final JAXBContext jaxbContext = JAXBContext.newInstance(LognetInvoicesSummaryResponse.class);

            final Unmarshaller jaxbUnmarshaller = jaxbContext.createUnmarshaller();

            final LognetInvoicesSummaryResponse response = (LognetInvoicesSummaryResponse) jaxbUnmarshaller.unmarshal(file);

            return response.getOk().getInvoiceSummaries();

        } catch (final JAXBException e) {
            e.printStackTrace();
            return Collections.emptyList();
        }
    }
}
