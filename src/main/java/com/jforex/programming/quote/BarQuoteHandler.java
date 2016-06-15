package com.jforex.programming.quote;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.aeonbits.owner.ConfigFactory;
import org.apache.commons.collections4.keyvalue.MultiKey;

import com.jforex.programming.builder.BarQuoteSubscription;
import com.jforex.programming.misc.HistoryUtil;
import com.jforex.programming.misc.JFHotObservable;
import com.jforex.programming.misc.JForexUtil;
import com.jforex.programming.settings.UserSettings;

import com.dukascopy.api.IBar;
import com.dukascopy.api.IContext;
import com.dukascopy.api.Instrument;
import com.dukascopy.api.OfferSide;
import com.dukascopy.api.Period;

import rx.Observable;

public class BarQuoteHandler implements BarQuoteProvider {

    private final JForexUtil jforexUtil;
    private final IContext context;
    private final HistoryUtil historyUtil;
    private final JFHotObservable<BarQuote> barQuotePublisher = new JFHotObservable<>();
    private final Observable<BarQuote> barQuoteObservable = barQuotePublisher.observable();
    private final Map<MultiKey<Object>, BarQuote> latestBarQuote = new ConcurrentHashMap<>();

    private final static UserSettings userSettings = ConfigFactory.create(UserSettings.class);

    public BarQuoteHandler(final JForexUtil jforexUtil) {
        this.jforexUtil = jforexUtil;
        context = jforexUtil.context();
        historyUtil = jforexUtil.historyUtil();
    }

    @Override
    public IBar askBar(final Instrument instrument,
                       final Period period) {
        return bar(instrument, period, OfferSide.ASK);
    }

    @Override
    public IBar bidBar(final Instrument instrument,
                       final Period period) {
        return bar(instrument, period, OfferSide.BID);
    }

    @Override
    public IBar forOfferSide(final Instrument instrument,
                             final Period period,
                             final OfferSide offerSide) {
        return bar(instrument, period, offerSide);
    }

    private IBar bar(final Instrument instrument,
                     final Period period,
                     final OfferSide offerSide) {
        return latestBarQuote.containsKey(barQuoteKey(instrument, period, offerSide))
                ? barQuoteByOfferSide(instrument, period, offerSide)
                : historyUtil.latestBar(instrument, period, offerSide);
    }

    private IBar barQuoteByOfferSide(final Instrument instrument,
                                     final Period period,
                                     final OfferSide offerSide) {
        final BarQuote barQuote = latestBarQuote.get(barQuoteKey(instrument, period, offerSide));
        return barQuote.bar();
    }

    @Override
    public Observable<BarQuote> quoteObservable(final BarQuoteSubscription subscription) {
        if (subscription.period().name() == null)
            subscribeToContext(subscription);
        return quoteFilterObservable(subscription);
    }

    private void subscribeToContext(final BarQuoteSubscription subscription) {
        subscription
                .instruments()
                .forEach(instrument -> context.subscribeToBarsFeed(instrument,
                                                                   subscription.period(),
                                                                   subscription.offerSide(),
                                                                   this::onBar));
    }

    private Observable<BarQuote> quoteFilterObservable(final BarQuoteSubscription subscription) {
        return barQuoteObservable
                .filter(barQuote -> subscription.instruments().contains(barQuote.instrument()))
                .filter(barQuote -> subscription.period().compareTo(barQuote.period()) == 0)
                .filter(barQuote -> barQuote.offerSide() == subscription.offerSide());
    }

    @Override
    public Observable<BarQuote> observable() {
        return barQuoteObservable;
    }

    private MultiKey<Object> barQuoteKey(final Instrument instrument,
                                         final Period period,
                                         final OfferSide offerSide) {
        return new MultiKey<Object>(instrument, period, offerSide);
    }

    public void onBar(final Instrument instrument,
                      final Period period,
                      final OfferSide offerSide,
                      final IBar bar) {
        if (userSettings.enableWeekendQuoteFilter() && !jforexUtil.isMarketClosed(bar.getTime()))
            onBarQuote(new BarQuote(instrument, period, offerSide, bar));
    }

    private void onBarQuote(final BarQuote barQuote) {
        final MultiKey<Object> multiKey = barQuoteKey(barQuote.instrument(),
                                                      barQuote.period(),
                                                      barQuote.offerSide());
        latestBarQuote.put(multiKey, barQuote);
        barQuotePublisher.onNext(barQuote);
    }
}