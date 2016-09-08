package learnrxjava.examples;

import rx.Observable;
import rx.Observer;
import rx.functions.Action1;
import rx.functions.Func1;

/**
 *
 * @author dkahlenberg
 */
public class RxMonadic {

    public static void main(String[] args) throws Exception {

        Observable.just(1, 2, 3).doOnNext(new Action1<Integer>() {
            @Override
            public void call(Integer item) {
                if (item > 2) {
                    throw new RuntimeException("Item exceeds maximum value");
                }
            }
        }).flatMap(new Func1() {
            @Override
            public Object call(Object t) {
                return Observable.just(1 + Integer.parseInt(t.toString()));
            }
        }).subscribe(new Observer() {
            @Override
            public void onCompleted() {
                System.out.println("Everything alright");
            }

            @Override
            public void onError(Throwable thrwbl) {
                System.out.println("Exception occurred in assembly: " + thrwbl);
            }

            @Override
            public void onNext(Object t) {
                System.out.println("intermediate addition result: " + t);
            }
        });
    }
}
