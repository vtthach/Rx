package com.morihacky.android.rxjava.fragments;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;

import com.morihacky.android.rxjava.R;
import com.morihacky.android.rxjava.wiring.LogAdapter;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import butterknife.Bind;
import butterknife.ButterKnife;
import butterknife.OnClick;
import io.reactivex.Observable;
import io.reactivex.ObservableEmitter;
import io.reactivex.ObservableOnSubscribe;
import io.reactivex.ObservableSource;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.functions.BooleanSupplier;
import io.reactivex.functions.Function;
import io.reactivex.functions.Predicate;
import io.reactivex.observers.DisposableObserver;
import io.reactivex.schedulers.Schedulers;
import rx.functions.Func0;
import timber.log.Timber;

public class TimeoutDemoFragment
        extends BaseFragment {

    @Bind(R.id.list_threading_log)
    ListView _logsList;

    private LogAdapter _adapter;
    private DisposableObserver<String> _disposable;
    private List<String> _logs;

    @Override
    public void onDestroy() {
        super.onDestroy();

        if (_disposable == null) {
            return;
        }

        _disposable.dispose();
    }

    @Override
    public View onCreateView(LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View layout = inflater.inflate(R.layout.fragment_subject_timeout, container, false);
        ButterKnife.bind(this, layout);
        return layout;
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        _setupLogger();
    }

    @OnClick(R.id.btn_demo_timeout_1_2s)
    public void onStart2sTask() {
        cleanLog();
        _disposable = _getEventCompletionObserver();
        getCustomObserverble()
                .timeout(3, TimeUnit.SECONDS)
                .subscribeOn(Schedulers.computation())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(_disposable);
    }

    @OnClick(R.id.btn_demo_timeout_1_5s)
    public void onStart5sTask() {
        cleanLog();
        _disposable = _getEventCompletionObserver();
        getCustomObserverble()
                .timeout(3, TimeUnit.SECONDS, _onTimeoutObservable())
                .subscribeOn(Schedulers.computation())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(_disposable);
    }

    @OnClick(R.id.btn_demo_timeout_3)
    public void onStartExceptionTask() {
        cleanLog();
        long timeOut = 15 * 1000;
        long start = System.currentTimeMillis();
        int timeDelay = 5000;
        DisposableObserver<Long> _disposable2 = getDisposable(start);
        getCustomExceptionObserverble()
//                .takeUntil(new Predicate<Long>() {
//                    @Override
//                    public boolean test(Long aLong) throws Exception {
//                        _log(String.format("------- takeUntil task - " + aLong));
//                        // Check response
//                        return aLong - start > 15000;
//                    }
//                })
                .timeout(timeOut, TimeUnit.MILLISECONDS)
                .filter(new Predicate<Long>() {
                    @Override
                    public boolean test(Long aLong) throws Exception {
                        _log(String.format("-------filter task value : " + aLong));
                        boolean isAllow = System.currentTimeMillis() - aLong > 13 * 1000;
                        if (isAllow) {
                            return true;
                        } else {
                            throw new FilterException(aLong);
                        }
                    }
                })
                .retryWhen(new RetryWithDelay(3, timeOut, timeDelay))
//                .retryUntil(new MyConditionRetryUntil(maxTimeout, 3))
//                .repeatUntil(new BooleanSupplier() {
//                    @Override
//                    public boolean getAsBoolean() throws Exception {
//                        _log(String.format("-------check repeatUntil task 3s timeout"));
//                        return System.currentTimeMillis() - start > 15000;
//                    }
//                })
                .subscribeOn(Schedulers.computation())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(_disposable2);
    }


    private DisposableObserver<Long> getDisposable(long start) {
        return new DisposableObserver<Long>() {
            @Override
            public void onNext(Long value) {
                _log(String.format("-------onNext task value : " + value));
            }

            @Override
            public void onError(Throwable e) {
                String data = "--";
                if (e instanceof FilterException) {
                    FilterException retryException = (FilterException) e;
                    data = Long.toString(retryException.data);
                }
                _log(String.format("-------onError - " + e.getClass().getSimpleName() + "-" + e.getMessage()) + "-data:" + data);
                _log(String.format("-------Total time on error: " + (System.currentTimeMillis() - start)));
                dispose();
            }

            @Override
            public void onComplete() {
                _log(String.format("-------onComplete"));
                _log(String.format("-------Total time on complete: " + (System.currentTimeMillis() - start)));
                dispose();
            }
        };
    }

    private DisposableObserver<String> _getEventCompletionObserver() {
        return new DisposableObserver<String>() {
            @Override
            public void onNext(String taskType) {
                _log(String.format("onNext task :" + taskType));
            }

            @Override
            public void onError(Throwable e) {
                _log(String.format("onError - " + e.getClass().getSimpleName() + "-" + e.getMessage()));
                Timber.e(e, "Timeout Demo exception");
            }

            @Override
            public void onComplete() {
                _log(String.format("task was completed"));
            }
        };
    }

    private Observable<String> getCustomObserverble() {
        return Observable.defer(new Func0<ObservableSource<? extends Boolean>>() {
            @Override
            public ObservableSource<? extends Boolean> call() {
                _log(String.format("Starting vtt task - boolean"));
                return Observable.just(true);
            }
        }).map(new Function<Boolean, String>() {
            @Override
            public String apply(Boolean aBoolean) {
                _log(String.format("- Start sleep vtt1  task - 5000"));
                try {
                    Thread.sleep(3000);
                } catch (InterruptedException e) {
                    _log(String.format("- InterruptedException"));
                    e.printStackTrace();
                }
                _log(String.format("- End sleep vtt1  task - 5000"));
                return "##Map1##";
            }
        }).map(new Function<String, String>() {
            @Override
            public String apply(String s) {
                _log(String.format("- Start sleep vtt2  task - 5000"));
                try {
                    Thread.sleep(3000);
                } catch (InterruptedException e) {
                    _log(String.format("- InterruptedException"));
                    e.printStackTrace();
                }
                _log(String.format("- End sleep vtt2  task - 5000"));
                return "##Map2##";
            }
        });
    }

    private Observable<Long> getCustomExceptionObserverble() {
        return Observable.defer(new Func0<ObservableSource<? extends Boolean>>() {
            @Override
            public ObservableSource<? extends Boolean> call() {
                _log(String.format("#### Starting vtt task - sleep 5s"));
                return Observable.just(true);
            }
        }).map(new Function<Boolean, String>() {
            @Override
            public String apply(Boolean aBoolean) {
                _log(String.format("- 1 Start sleep 5s..."));
                try {
                    Thread.sleep(3000);
                } catch (InterruptedException e) {
                    _log(String.format("- InterruptedException"));
                    e.printStackTrace();
                }
                _log(String.format("- 1 Start exception after sleep 5s"));
//                throw new RuntimeException();
                return "ok";
            }
        }).map(new Function<String, Long>() {
            @Override
            public Long apply(String s) {
                _log(String.format("- 2 continue sleep 5s..."));
                try {
                    Thread.sleep(3000);
                } catch (InterruptedException e) {
                    _log(String.format("- InterruptedException"));
                    e.printStackTrace();
                }
                _log(String.format("- 2 end sleep 5s///// "));
                return System.currentTimeMillis();
            }
        });
    }

    // -----------------------------------------------------------------------------------
    // Main Rx entities

    private Observable<String> _getObservableTask_5sToComplete() {
        return Observable.create(new ObservableOnSubscribe<String>() {
            @Override
            public void subscribe(ObservableEmitter<String> subscriber) throws Exception {
                _log(String.format("Starting a 5s task"));
                subscriber.onNext("5 s");
                try {
                    Thread.sleep(5_000);
                } catch (InterruptedException e) {
                    _log(String.format("- InterruptedException"));
                    e.printStackTrace();
                }
                subscriber.onComplete();
            }
        });
    }

    private Observable<String> _getObservableTask_2sToComplete() {
        return Observable
                .create(new ObservableOnSubscribe<String>() {
                    @Override
                    public void subscribe(ObservableEmitter<String> subscriber) throws Exception {
                        _log(String.format("Starting a 2s task"));
                        subscriber.onNext("2 s");
                        try {
                            Thread.sleep(2_000);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                        subscriber.onComplete();
                    }
                });
    }

    private Observable<String> _onTimeoutObservable() {
        return Observable.create(new ObservableOnSubscribe<String>() {

            @Override
            public void subscribe(ObservableEmitter<String> subscriber) throws Exception {
                _log("Timing out this task ...");
                subscriber.onError(new Throwable("Timeout Error"));
            }
        });
    }

    // -----------------------------------------------------------------------------------
    // Method that help wiring up the example (irrelevant to RxJava)

    private void _setupLogger() {
        _logs = new ArrayList<>();
        _adapter = new LogAdapter(getActivity(), new ArrayList<>());
        _logsList.setAdapter(_adapter);
    }

    private void cleanLog() {
        _logs.clear();
        _adapter.clear();
        _adapter.notifyDataSetChanged();
    }

    private long timeTrigger;

    private void _log(String logMsg) {
        if (_adapter != null) {
            long timeToLog;
            if (timeTrigger == 0) {
                timeToLog = 0;
                timeTrigger = System.currentTimeMillis();
            } else {
                timeToLog = System.currentTimeMillis() - timeTrigger;
                timeTrigger = System.currentTimeMillis();
            }
            if (_isCurrentlyOnMainThread()) {
                _logs.add(logMsg + Thread.currentThread().getName() + " - " + timeToLog);
                _adapter.clear();
                _adapter.addAll(_logs);
            } else {
                _logs.add(logMsg + Thread.currentThread().getName() + " - " + timeToLog);

                // You can only do below stuff on main thread.
                new Handler(Looper.getMainLooper()).post(() -> {
                    _adapter.clear();
                    _adapter.addAll(_logs);
                });
            }
        }
    }

    private boolean _isCurrentlyOnMainThread() {
        return Looper.myLooper() == Looper.getMainLooper();
    }

    public class MyConditionRetryUntil implements BooleanSupplier {

        private final int maxRetryAttempts;
        private final long startTime;
        private final long maxTimeoutAllow;
        int counter;

        public MyConditionRetryUntil(long maxTimeoutAllow, int maxRetryAttempts) {
            this.maxRetryAttempts = maxRetryAttempts;
            this.maxTimeoutAllow = maxTimeoutAllow;
            startTime = SystemClock.elapsedRealtime();
            counter = 0;
        }

        @Override
        public boolean getAsBoolean() throws Exception {
            // true: will stop retry
            return isMaxRetryAttempt() || isTimeoutException();
        }

        private boolean isMaxRetryAttempt() {
            return ++counter >= maxRetryAttempts;
        }

        private boolean isTimeoutException() {
            boolean isTimeout = SystemClock.elapsedRealtime() - startTime > maxTimeoutAllow;
            if (isTimeout) {
                _log("Time out retry exception!!!");
            }
            return isTimeout;
        }
    }

    private class FilterException extends Exception {
        Long data;

        public FilterException(Long data) {
            this.data = data;
        }


        @Override
        public String getMessage() {
            return "RetryExceptionVTT";
        }
    }

    public class RetryWithDelay implements
            Function<Observable<? extends Throwable>, Observable<?>> {

        private final int maxRetries;
        private final int retryDelayMillis;
        private final long startTime;
        private int attemptRetryCount;
        private long maxTimeout;

        public RetryWithDelay(final int maxRetries, final long maxTimeoutMillis, final int retryDelayMillis) {
            this.maxRetries = maxRetries;
            this.retryDelayMillis = retryDelayMillis;
            this.maxTimeout = maxTimeoutMillis;
            startTime = SystemClock.elapsedRealtime();
            attemptRetryCount = 0;
        }

        private boolean isMaxRetryAttempt() {
            return ++attemptRetryCount >= maxRetries;
        }

        private boolean isTimeoutException() {
            boolean isTimeout = SystemClock.elapsedRealtime() - startTime > maxTimeout;
            if (isTimeout) {
                _log("Time out retry exception!!!");
            }
            return isTimeout;
        }

        @Override
        public Observable<?> apply(Observable<? extends Throwable> observable) throws Exception {
            return observable.flatMap(new Function<Throwable, ObservableSource<?>>() {
                @Override
                public ObservableSource<?> apply(Throwable throwable) throws Exception {
                    _log("RetryWithDelay - apply");
                    if (isMaxRetryAttempt() || isTimeoutException()) {
                        return Observable.error(throwable);
                    }
                    return Observable.timer(retryDelayMillis, TimeUnit.MILLISECONDS);
                }
            });
        }
    }
}