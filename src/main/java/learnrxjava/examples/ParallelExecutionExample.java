package learnrxjava.examples;

import java.util.List;

import rx.Observable;
import rx.Subscriber;
import rx.functions.Action0;
import rx.functions.Func1;
import rx.functions.Func2;
import rx.schedulers.Schedulers;

public class ParallelExecutionExample {

    public static void run() {
        Observable<Tile> searchTile = getSearchResults("search term");

        Func1<Tile, Observable<TileResponse>> mapFunc = new Func1<Tile, Observable<TileResponse>>() {
            @Override
            public Observable<TileResponse> call(final Tile t) {
                Observable<Reviews> reviews = getSellerReviews(t.getSellerId());
                Observable<String> imageUrl = getProductImage(t.getProductId());

                Func2<Reviews, String, TileResponse> zipFunction = new Func2<Reviews, String, TileResponse>() {
                    @Override
                    public TileResponse call(Reviews r, String u) {
                        return new TileResponse(t, r, u);
                    }
                };

                return Observable.zip(reviews, imageUrl, zipFunction);
            }
        };

        Observable<TileResponse> populatedTiles = searchTile.flatMap(mapFunc);

        List<TileResponse> allTiles = populatedTiles.toList()
                .toBlocking().single();
    }

    public static void main(String[] args) {
        final long startTime = System.currentTimeMillis();

        Observable<Tile> searchTile = getSearchResults("search term")
                .doOnSubscribe(new Action0() {
                    @Override
                    public void call() {
                        logTime("Search started ", startTime);
                    }
                })
                .doOnCompleted(new Action0() {
                    @Override
                    public void call() {
                        logTime("Search completed ", startTime);
                    }
                });

        Func1<Tile, Observable<TileResponse>> mapFunc = new Func1<Tile, Observable<TileResponse>>() {
            @Override
            public Observable<TileResponse> call(final Tile t) {
                Observable<Reviews> reviews = getSellerReviews(t.getSellerId())
                        .doOnCompleted(
                                new Action0() {
                            @Override
                            public void call() {
                                logTime("getSellerReviews[" + t.id + "] completed ", startTime);
                            }
                        });
                Observable<String> imageUrl = getProductImage(t.getProductId())
                        .doOnCompleted(
                                new Action0() {
                            @Override
                            public void call() {
                                logTime("getProductImage[" + t.id + "] completed ", startTime);
                            }
                        });

                Func2<Reviews, String, TileResponse> zipFunction = new Func2<Reviews, String, TileResponse>() {
                    @Override
                    public TileResponse call(Reviews r, String u) {
                        return new TileResponse(t, r, u);
                    }
                };

                return Observable.zip(reviews, imageUrl, zipFunction).doOnCompleted(new Action0() {
                    @Override
                    public void call() {
                        logTime("zip[" + t.id + "] completed ", startTime);
                    }
                });
            }

        };

        Observable<TileResponse> populatedTiles = searchTile.flatMap(mapFunc);

        List<TileResponse> allTiles = populatedTiles.toList()
                .doOnCompleted(new Action0() {
                    @Override
                    public void call() {
                        logTime("All Tiles Completed ", startTime);
                    }
                }).toBlocking().single();
    }

    private static Observable<Tile> getSearchResults(String string) {
        return mockClient(new Tile(1), new Tile(2), new Tile(3));
    }

    private static Observable<Reviews> getSellerReviews(int id) {
        return mockClient(new Reviews());
    }

    private static Observable<String> getProductImage(int id) {
        return mockClient("image_" + id);
    }

    private static void logTime(String message, long startTime) {
        System.out.println(message + " => " + (System.currentTimeMillis() - startTime) + "ms");
    }

    private static <T> Observable<T> mockClient(final T... ts) {

        Observable.OnSubscribe<T> onSubscribe = new Observable.OnSubscribe<T>() {
            @Override
            public void call(Subscriber<? super T> s) {
                // simulate latency
                try {
                    Thread.sleep(1000);
                } catch (Exception e) {
                }
                for (T t : ts) {
                    s.onNext(t);
                }
                s.onCompleted();
            }
        };

        return Observable.create(onSubscribe).subscribeOn(Schedulers.io());
        // note the use of subscribeOn to make an otherwise synchronous Observable async
    }

    public static class TileResponse {

        public TileResponse(Tile t, Reviews r, String u) {
            // store the values
        }

    }

    public static class Tile {

        private final int id;

        public Tile(int i) {
            this.id = i;
        }

        public int getSellerId() {
            return id;
        }

        public int getProductId() {
            return id;
        }

    }

    public static class Reviews {

    }
}
