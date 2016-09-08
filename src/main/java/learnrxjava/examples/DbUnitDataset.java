package learnrxjava.examples;

import com.google.common.base.Objects;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.common.util.concurrent.MoreExecutors;
import org.dbunit.database.DatabaseConnection;
import org.dbunit.database.DatabaseSequenceFilter;
import org.dbunit.database.IDatabaseConnection;
import org.dbunit.dataset.DataSetException;
import org.dbunit.dataset.FilteredDataSet;
import org.dbunit.dataset.IDataSet;
import org.dbunit.dataset.filter.ITableFilter;
import org.dbunit.dataset.xml.FlatXmlDataSet;
import rx.Observable;
import rx.Subscriber;
import rx.Subscription;
import rx.functions.Action0;
import rx.functions.Action1;

import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 *
 * @author dkahlenberg
 */
public class DbUnitDataset {

    public static void main(String[] args) throws Exception {
        final Class driverClass = Class.forName("oracle.jdbc.OracleDriver");
        final Connection jdbcConnection = DriverManager.getConnection("jdbc:oracle:thin:@localhost:21521:orcl", "SCHE1", "SCHE1");
        final IDatabaseConnection connection = new DatabaseConnection(jdbcConnection, "SCHE1");
        final String[] providedComponents = new String[]{
            "TAB1"
        };
        final String[] basicGenerated = new String[]{
            "TAB2"
        };
        final String[] countryGenerated = new String[]{
            "TAB3"
        };

        final Observable.OnSubscribe<IDataSet> subscribeForProvidedComponentsDataset = new Observable.OnSubscribe<IDataSet>() {
            @Override
            public void call(Subscriber<? super IDataSet> s) {
                try {
                    new DbUnitDataset().computeOrderedDataset(s, providedComponents, connection);
                } catch (Throwable t) {
                    throw new RuntimeException(t);
                }
            }
        };

        final Observable.OnSubscribe<IDataSet> subscribeForBasicDataset = new Observable.OnSubscribe<IDataSet>() {
            @Override
            public void call(Subscriber<? super IDataSet> s) {
                try {
                    new DbUnitDataset().computeOrderedDataset(s, basicGenerated, connection);
                } catch (Throwable t) {
                    throw new RuntimeException(t);
                }
            }
        };

        final Observable.OnSubscribe<IDataSet> subscribeForCountryDataset = new Observable.OnSubscribe<IDataSet>() {
            @Override
            public void call(Subscriber<? super IDataSet> s) {
                try {
                    new DbUnitDataset().computeOrderedDataset(s, countryGenerated, connection);
                } catch (Throwable t) {
                    throw new RuntimeException(t);
                }
            }
        };

        final Action1<IDataSet> onNext = new Action1<IDataSet>() {
            @Override
            public void call(IDataSet incomingValue) {
                System.out.println("incoming dataset " + incomingValue);
                try {
                    final Path path = Files.createTempFile("dataset-", ".xml");
                    System.out.println("Temp file : " + path);
                    FlatXmlDataSet.write(incomingValue, new FileWriter(path.toFile()));
                } catch (IOException | DataSetException ex) {
                    throw new RuntimeException(ex);
                }
            }
        };
        final Action1<Throwable> onError = new Action1<Throwable>() {
            @Override
            public void call(Throwable error) {
                System.out.println("Something went wrong " + error.getMessage());
            }
        };
        final Action0 onComplete = new Action0() {
            @Override
            public void call() {
                System.out.println("This dataset generation is finished");
            }
        };

        Observable.create(subscribeForProvidedComponentsDataset).subscribe(onNext, onError, onComplete);
        Observable.create(subscribeForBasicDataset).subscribe(onNext, onError, onComplete);
        Observable.create(subscribeForCountryDataset).subscribe(onNext, onError, onComplete);

        final ExecutorService pool = Executors.newFixedThreadPool(1);
        FutureCallback<Integer> futureCallback = new FutureCallback<Integer>() {
            @Override
            public void onSuccess(Integer exitCode) {
                pool.shutdown();
            }

            @Override
            public void onFailure(Throwable thrown) {
                if (!Objects.equal(null, thrown)) {
                    System.err.println("Unintentional error when running dataset extraction: " + thrown.getMessage());
                }
                pool.shutdown();
            }
        };

        final ListeningExecutorService service = MoreExecutors.listeningDecorator(pool);
        ListenableFuture<Integer> providedComponentsExtraction = service.submit(new Callable<Integer>() {
            @Override
            public Integer call() throws IOException, InterruptedException {
                Subscription ob1 = Observable.create(subscribeForProvidedComponentsDataset).subscribe(onNext, onError, onComplete);
                return 1;
            }
        });
        final ListenableFuture<Integer> basicTablesExtraction = service.submit(new Callable<Integer>() {
            @Override
            public Integer call() throws IOException, InterruptedException {
                Subscription ob1 = Observable.create(subscribeForBasicDataset).subscribe(onNext, onError, onComplete);
                return 1;
            }
        });
        final ListenableFuture<Integer> countryTablesExtraction = service.submit(new Callable<Integer>() {
            @Override
            public Integer call() throws IOException, InterruptedException {
                Subscription ob1 = Observable.create(subscribeForCountryDataset).subscribe(onNext, onError, onComplete);
                return 1;
            }
        });
        Futures.addCallback(providedComponentsExtraction, futureCallback);
        Futures.addCallback(basicTablesExtraction, futureCallback);
        Futures.addCallback(countryTablesExtraction, futureCallback);
    }

    private void computeOrderedDataset(Subscriber s, String[] datasetTables, IDatabaseConnection connection) {

        Subscriber subscriber = (Subscriber) s;

        try {
            ITableFilter filter = new DatabaseSequenceFilter(connection, datasetTables);
            IDataSet dataset = new FilteredDataSet(filter, connection.createDataSet());

            if (!subscriber.isUnsubscribed()) {
                subscriber.onNext(dataset);
            }

            if (!subscriber.isUnsubscribed()) {
                subscriber.onCompleted();
            }
        } catch (Throwable throwable) {
            subscriber.onError(throwable);
        }
    }
}
