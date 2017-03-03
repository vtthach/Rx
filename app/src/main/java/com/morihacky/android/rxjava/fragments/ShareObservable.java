package com.morihacky.android.rxjava.fragments;


import android.graphics.Color;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import io.reactivex.Observable;
import io.reactivex.ObservableSource;
import io.reactivex.Observer;
import io.reactivex.functions.Function;
import io.reactivex.observables.ConnectableObservable;
import io.reactivex.observers.DisposableObserver;

public class ShareObservable extends Fragment {
    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        TextView view = new TextView(getActivity());
        view.setText("Text share observable");
        view.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        view.setBackgroundColor(Color.BLUE);
        return view;
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        ConnectableObservable<Integer> observableSource = getObservable().share().replay();
        Observer<? super Integer> disposol1 = new DisposableObserver<Integer>() {
            @Override
            public void onNext(Integer value) {
                Log.w("vtt", "onNext  disposol1:" + value + " " + getThreadName());
            }

            @Override
            public void onError(Throwable e) {

            }

            @Override
            public void onComplete() {

            }
        };

        Observer<? super Integer> disposol2 = new DisposableObserver<Integer>() {
            @Override
            public void onNext(Integer value) {
                Log.w("vtt", "onNext  disposol2:" + value + " " + getThreadName());

            }

            @Override
            public void onError(Throwable e) {

            }

            @Override
            public void onComplete() {

            }
        };
        observableSource.subscribe(disposol1);
        observableSource.subscribe(disposol2);
        observableSource.connect();
    }

    private Observable<Integer> getObservable() {
        return Observable.just(new int[]{0, 3, 5}).flatMap(new Function<int[], ObservableSource<Integer>>() {
            @Override
            public ObservableSource<Integer> apply(int[] aBoolean) throws Exception {
                Log.w("vtt", "  flatMap:" + getThreadName());
                int sum = 0;
                for (int i : aBoolean) {
                    sum += i;
                }
                return Observable.just(sum);
            }
        });
    }

    private String getThreadName() {
        return Thread.currentThread().getName();
    }
}
