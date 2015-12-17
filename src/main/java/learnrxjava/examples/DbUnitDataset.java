package learnrxjava.examples;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
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
import rx.functions.Action0;
import rx.functions.Action1;

/**
 *
 * @author dkahlenberg
 */
public class DbUnitDataset {

    public static void main(String[] args) throws Exception {
        Observable.OnSubscribe<IDataSet> subscribeForDataset = new Observable.OnSubscribe<IDataSet>() {
            @Override
            public void call(Subscriber<? super IDataSet> s) {
                try {
                    Class driverClass = Class.forName("oracle.jdbc.OracleDriver");
                    Connection jdbcConnection = DriverManager.getConnection("jdbc:oracle:thin:@localhost:15219:orcl", "USER", "PASSWD");
                    IDatabaseConnection connection = new DatabaseConnection(jdbcConnection, "SCHEMA");
                    new DbUnitDataset().computeOrderedDataset(s, connection);
                } catch (Throwable t) {
                    throw new RuntimeException(t);
                }
            }
        };

        Observable createdObservable = Observable.create(subscribeForDataset);

        createdObservable.subscribe(new Action1<IDataSet>() {
            @Override
            public void call(IDataSet incomingValue) {
                System.out.println("incoming " + incomingValue);
                try {
                    FlatXmlDataSet.write(incomingValue, new FileWriter(new File("dataset.xml")));
                } catch (IOException | DataSetException ex) {
                    throw new RuntimeException(ex);
                }
            }
        }, new Action1() {
            @Override
            public void call(Object error) {
                System.out.println("Something went wrong " + ((Throwable) error).getMessage());
            }
        }, new Action0() {
            @Override
            public void call() {
                System.out.println("This observable is finished");
            }
        });

        System.out.println("Doin something new");
    }

    private void computeOrderedDataset(Subscriber s, IDatabaseConnection connection) {
        Subscriber subscriber = (Subscriber) s;

        try {
            String[] tableNames = new String[]{
                "dual"};
            ITableFilter filter = new DatabaseSequenceFilter(connection, tableNames);
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
