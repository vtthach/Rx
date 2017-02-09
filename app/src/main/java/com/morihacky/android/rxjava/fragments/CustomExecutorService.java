package com.morihacky.android.rxjava.fragments;


import android.graphics.Color;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import io.reactivex.Observable;
import io.reactivex.ObservableSource;
import io.reactivex.Scheduler;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.functions.Function;
import io.reactivex.observers.DisposableObserver;
import io.reactivex.schedulers.Schedulers;


public class CustomExecutorService extends BaseFragment implements View.OnClickListener {
    ExecutorService executor;
    private CompositeDisposable compositeDisposal;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        TextView view = new TextView(getActivity());
        view.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        view.setBackgroundColor(Color.BLUE);
        return view;
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        int threadCt = Runtime.getRuntime().availableProcessors() + 1;
        Log.i("vtt", threadCt + " threadCt");

        Map<Thread, StackTraceElement[]> threads = Thread.getAllStackTraces();
        Log.i("vtt", "Thread count: " + threads.size());

        executor = Executors.newFixedThreadPool(threadCt);

        Scheduler schedulers = Schedulers.from(executor);

        Observable.range(1, 100)
                .flatMap(new Function<Integer, ObservableSource<Integer>>() {
                    @Override
                    public ObservableSource<Integer> apply(Integer integer) {
                        Log.w("vtt", integer + " apply:" + getThreadName());
                        try {
                            Thread.sleep(200);
                        } catch (InterruptedException e) {
                            Log.w("vtt", integer + " InterruptedException flatMap:" + getThreadName());
                            e.printStackTrace();
                        }
                        return Observable.just(integer);
                    }
                })
                .map(integer -> {
                    try {
                        Thread.sleep(200);
                    } catch (InterruptedException e) {
                        Log.w("vtt", integer + " InterruptedException map:" + getThreadName());
                        e.printStackTrace();
                    }
                    return integer + " map";
                })
                .subscribeOn(schedulers)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(add(getDisposal()));

        Observable.range(1, 100)
                .flatMap(new Function<Integer, ObservableSource<Integer>>() {
                    @Override
                    public ObservableSource<Integer> apply(Integer integer) {
                        Log.w("vtt", integer + " apply2:" + getThreadName());
                        try {
                            Thread.sleep(200);
                        } catch (InterruptedException e) {
                            Log.w("vtt", integer + " InterruptedException flatMap2:" + getThreadName());
                            e.printStackTrace();
                        }
                        return Observable.just(integer);
                    }
                })
                .map(integer -> {
                    try {
                        Thread.sleep(200);
                    } catch (InterruptedException e) {
                        Log.w("vtt", integer + " InterruptedException map2:" + getThreadName());
                        e.printStackTrace();
                    }
                    return integer + " map2";
                })
                .subscribeOn(schedulers)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(add(getDisposal()));

        Map<Thread, StackTraceElement[]> threadsAfter = Thread.getAllStackTraces();

        Log.i("vtt", "Thread count after: " + threadsAfter.size());
    }

    private DisposableObserver<String> add(DisposableObserver<String> disposal) {
        if(compositeDisposal == null){
            compositeDisposal = new CompositeDisposable();
        }
        compositeDisposal.add(disposal);
        return disposal;
    }

    private DisposableObserver<String> getDisposal() {
        return new DisposableObserver<String>() {
            @Override
            public void onNext(String value) {
                Log.i("vtt", "onNext:" + value + getThreadName());
            }

            @Override
            public void onError(Throwable e) {
                Log.i("vtt", "onError:" + e + getThreadName());
            }

            @Override
            public void onComplete() {
                Log.i("vtt", "onComplete" + getThreadName());
            }
        };
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        Log.i("vtt", "onDestroy" + getThreadName());
        compositeDisposal.dispose();
        if (!executor.isShutdown()) {
            executor.shutdown();
        }
    }

    private String getThreadName() {
        return " " + Thread.currentThread().getName();
    }

    @Override
    public void onClick(View v) {

    }
}
