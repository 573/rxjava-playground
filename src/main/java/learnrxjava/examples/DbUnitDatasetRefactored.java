package learnrxjava.examples;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.Appender;
import ch.qos.logback.core.FileAppender;
import ch.qos.logback.core.encoder.Encoder;
import com.google.common.base.Objects;
import com.google.common.collect.Lists;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.common.util.concurrent.MoreExecutors;
import org.dbunit.database.DatabaseConnection;
import org.dbunit.database.DatabaseSequenceFilter;
import org.dbunit.database.IDatabaseConnection;
import org.dbunit.dataset.FilteredDataSet;
import org.dbunit.dataset.IDataSet;
import org.dbunit.dataset.filter.ITableFilter;
import org.dbunit.dataset.xml.FlatXmlDataSet;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileWriter;
import java.sql.Connection;
import java.sql.DriverManager;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 *
 * @author dkahlenberg
 */
public class DbUnitDatasetRefactored {

    final static String[] set_one = new String[]{
        "TAB1",
        "TAB2"
    };
    final static String[] set_two = new String[]{
        "TAB7", "TAB8"
    };
    final static String[] set_three = new String[]{
        "TAB9",
        "TAB10"
    };

    protected static Logger log;

    public static void main(String[] args) throws Exception {
        Class.forName("oracle.jdbc.OracleDriver");
        final Connection jdbcConnection = DriverManager.getConnection("jdbc:oracle:thin:@localhost:21521:orcl", "SCHE1", "SCHE1");
        final IDatabaseConnection connection = new DatabaseConnection(jdbcConnection, "SCHE1");

        setLogger();

        final ExecutorService pool = Executors.newFixedThreadPool(4);
        final ListeningExecutorService service = MoreExecutors.listeningDecorator(pool);

        FutureCallback<String> futureCallback = new FutureCallback<String>() {
            @Override
            public void onSuccess(String writtenXmlFilename) {
                log.info("dataset xml file: {}", writtenXmlFilename);
            }

            @Override
            public void onFailure(Throwable thrown) {
                if (!Objects.equal(null, thrown)) {
                    log.error("Unintentional error when running dataset extraction: {}", thrown.getMessage());
                    throw new RuntimeException(thrown);
                }
            }
        };

        ListenableFuture<String> providedComponentsExtraction = service.submit(new Callable<String>() {
            @Override
            public String call() throws Exception {
                File providedXml = new File("set_one.xml");
                FlatXmlDataSet.write(DbUnitDatasetRefactored.computeOrderedDataset(set_one, connection), new FileWriter(providedXml));
                return providedXml.getAbsolutePath();
            }
        });
        ListenableFuture<String> basicTablesExtraction = service.submit(new Callable<String>() {
            @Override
            public String call() throws Exception {
                File basicXml = new File("set_two.xml");
                FlatXmlDataSet.write(DbUnitDatasetRefactored.computeOrderedDataset(set_two, connection), new FileWriter(basicXml));
                return basicXml.getAbsolutePath();
            }
        });
        ListenableFuture<String> countryTablesExtraction = service.submit(new Callable<String>() {
            @Override
            public String call() throws Exception {
                File countryXml = new File("set_three.xml");
                FlatXmlDataSet.write(DbUnitDatasetRefactored.computeOrderedDataset(set_three, connection), new FileWriter(countryXml));
                return countryXml.getAbsolutePath();
            }
        });

        Futures.addCallback(providedComponentsExtraction, futureCallback);

        Futures.addCallback(basicTablesExtraction, futureCallback);

        Futures.addCallback(countryTablesExtraction, futureCallback);
        
        service.shutdown();
    }

    private static final IDataSet computeOrderedDataset(String[] datasetTables, IDatabaseConnection connection) throws Exception {
        ITableFilter filter = new DatabaseSequenceFilter(connection, datasetTables);
        return new FilteredDataSet(filter, connection.createDataSet());
    }

    private static void setLogger() {
        final List<FileAppender<ILoggingEvent>> appenders = Lists.newArrayList();
        final List<Logger> loggers = Lists.newArrayList();
        LoggerContext loggerContext = (LoggerContext) LoggerFactory.getILoggerFactory();
        for (final Logger logger : loggerContext.getLoggerList()) {
            for (final Iterator<Appender<ILoggingEvent>> index = logger.iteratorForAppenders();
                    index.hasNext();) {
                final Appender<ILoggingEvent> appender = index.next();
                if (appender instanceof FileAppender) {
                    final FileAppender<ILoggingEvent> fileAppender = (FileAppender<ILoggingEvent>) appender;
                    appenders.add(fileAppender);
                    loggers.add(logger);
                }
            }
        }

        log = loggers.get(0);

        final FileAppender<ILoggingEvent> fileAppender = appenders.get(0);

        if (fileAppender != null) {
            final Encoder<ILoggingEvent> encoder = fileAppender.getEncoder();
            encoder.start();
            fileAppender.start();
            log.error("logfile path: " + fileAppender.getFile());
        }
    }
}
