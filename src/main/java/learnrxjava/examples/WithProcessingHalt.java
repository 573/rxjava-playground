package learnrxjava.examples;

import rx.Observable;
import rx.Subscriber;
import rx.functions.Action0;
import rx.functions.Action1;

/**
 *
 * @author http://instil.co/2014/08/05/rxjava-in-different-flavours-of-java/
 */
public class WithProcessingHalt {
    public static void main(String[] args) {
        Observable.OnSubscribe<String> subscribeFunction = new Observable.OnSubscribe<String>() {
            @Override
            public void call(Subscriber<? super String> s) {
                new WithProcessingHalt().produceValuesAndAnError(s);
            }
        };

        Observable createdObservable = Observable.create(subscribeFunction);

        createdObservable.subscribe(new Action1() {
            @Override
            public void call(Object incomingValue) {
                System.out.println("incoming " + incomingValue);
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
    }

    private void produceValuesAndAnError(Subscriber s) {
        Subscriber subscriber = (Subscriber) s;

        try {
            for (int ii = 0; ii < 50; ii++) {
                if (!subscriber.isUnsubscribed()) {
                    subscriber.onNext("Pushed value " + ii);
                }

                if (ii > 45) {
                    throw new Throwable("Something has gone wrong here");
                }
            }

            if (!subscriber.isUnsubscribed()) {
                subscriber.onCompleted();
            }
        } catch (Throwable throwable) {
            subscriber.onError(throwable);
        }
    }

}
