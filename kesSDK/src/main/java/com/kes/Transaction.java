package com.kes;

/**
 * Created by gadza on 2015.03.02..
 */
public abstract class Transaction {

    private KES mSession;

    protected Transaction(KES session)
    {
        mSession = session;
    }

    public abstract void execute();

    public void cancel()
    {

    }

}
